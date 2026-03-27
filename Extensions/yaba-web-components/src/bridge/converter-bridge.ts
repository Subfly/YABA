import DOMPurify from "dompurify"
import { Readability } from "@mozilla/readability"
import { Editor } from "@tiptap/core"
import ePub from "epubjs"
import { GlobalWorkerOptions, getDocument } from "pdfjs-dist"
import pdfWorkerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url"
import { createEditorExtensions } from "../tiptap/editor-extensions"
import { extractLinkMetadata, type LinkMetadata } from "./link-metadata"

GlobalWorkerOptions.workerSrc = pdfWorkerUrl

const ASSET_PLACEHOLDER_PREFIX = "yaba-asset://"
const CODE_CLASS_HINT = /(language-|lang-|highlight|hljs|codehilite|prism|rouge|prettyprint|sourcecode|syntax)/i
const INLINE_TEXT_CONTAINERS = new Set(["P", "LI", "TD", "TH", "A", "SPAN", "EM", "STRONG", "B", "I", "U", "S", "DEL"])
const READABILITY_OPTIONS = {
  // Keep CSS classes so code-related classnames survive extraction.
  keepClasses: true,
  // Allow shorter technical posts/snippets to still be considered as article content.
  charThreshold: 20,
  // Consider more candidates before settling on the main article body.
  nbTopCandidates: 10,
}

export interface ConverterInput {
  html: string
  baseUrl?: string
}

export interface ConverterAsset {
  placeholder: string
  url: string
  alt?: string
}

export interface ConverterOutput {
  /** TipTap/ProseMirror JSON string (canonical reader document). */
  documentJson: string
  assets: ConverterAsset[]
  /** web-meta-scraper + tidy-url on Core-fetched HTML (no URL fetch in the bridge; HTML-only scrape). */
  linkMetadata: LinkMetadata
}

export interface YabaConverterBridge {
  sanitizeAndConvertHtmlToReaderHtml: (input: ConverterInput) => Promise<ConverterOutput>
  startHtmlConversion: (input: ConverterInput) => string
  getHtmlConversionJob: (jobId: string) => HtmlConversionJobState | null
  deleteHtmlConversionJob: (jobId: string) => void
  startPdfExtraction: (input: PdfExtractionInput) => string
  getPdfExtractionJob: (jobId: string) => PdfExtractionJobState | null
  deletePdfExtractionJob: (jobId: string) => void
  startEpubExtraction: (input: EpubExtractionInput) => string
  getEpubExtractionJob: (jobId: string) => EpubExtractionJobState | null
  deleteEpubExtractionJob: (jobId: string) => void
}

export interface PdfExtractionInput {
  pdfUrl: string
  renderScale?: number
}

export interface PdfTextSection {
  sectionKey: string
  text: string
}

export interface PdfExtractionOutput {
  title: StringOrNull
  author: StringOrNull
  subject: StringOrNull
  creationDate: StringOrNull
  pageCount: number
  firstPagePngDataUrl: StringOrNull
  sections: PdfTextSection[]
}

export type StringOrNull = string | null

export interface PdfExtractionJobState {
  status: "pending" | "done" | "error"
  output?: PdfExtractionOutput
  error?: string
}

export interface HtmlConversionJobState {
  status: "pending" | "done" | "error"
  output?: ConverterOutput
  error?: string
}

const pdfExtractionJobs = new Map<string, PdfExtractionJobState>()
const htmlConversionJobs = new Map<string, HtmlConversionJobState>()

export interface EpubExtractionInput {
  epubUrl: string
}

export interface EpubExtractionOutput {
  coverPngDataUrl: StringOrNull
  title: StringOrNull
  author: StringOrNull
  description: StringOrNull
  pubdate: StringOrNull
}

export interface EpubExtractionJobState {
  status: "pending" | "done" | "error"
  output?: EpubExtractionOutput
  error?: string
}

const epubExtractionJobs = new Map<string, EpubExtractionJobState>()

function resolveUrl(baseUrl: string | undefined, src: string): string {
  if (!baseUrl || src.startsWith("data:") || src.startsWith("http://") || src.startsWith("https://")) {
    return src
  }
  try {
    return new URL(src, baseUrl).href
  } catch {
    return src
  }
}

/**
 * Resolves relative `href` / `src` / `srcset` / `poster` against `baseUrl` in-place.
 * We intentionally do **not** inject `<base href>`: Android WebView CSP uses `base-uri 'self'`, and
 * setting the document base to an article origin is blocked. Readability still needs absolute URLs.
 */
function resolveRelativeUrlsInDocument(doc: Document, baseUrl: string): void {
  const base = baseUrl.trim()
  if (!base) return

  const toAbs = (raw: string): string => {
    const u = raw.trim()
    if (
      !u ||
      u.startsWith("#") ||
      u.startsWith("data:") ||
      u.startsWith("mailto:") ||
      u.startsWith("javascript:") ||
      u.startsWith("http://") ||
      u.startsWith("https://")
    ) {
      return raw
    }
    try {
      return new URL(u, base).href
    } catch {
      return raw
    }
  }

  const resolveSrcset = (v: string): string =>
    v
      .split(",")
      .map((part) => {
        const p = part.trim()
        if (!p) return part
        const sp = p.indexOf(" ")
        if (sp === -1) return toAbs(p)
        return `${toAbs(p.slice(0, sp))}${p.slice(sp)}`
      })
      .join(", ")

  doc.querySelectorAll("base").forEach((el) => el.remove())
  doc.querySelectorAll("[href]").forEach((el) => {
    const h = el.getAttribute("href")
    if (h) el.setAttribute("href", toAbs(h))
  })
  doc.querySelectorAll("[src]").forEach((el) => {
    const s = el.getAttribute("src")
    if (s && !s.startsWith("data:")) el.setAttribute("src", toAbs(s))
  })
  doc.querySelectorAll("[srcset]").forEach((el) => {
    const s = el.getAttribute("srcset")
    if (s) el.setAttribute("srcset", resolveSrcset(s))
  })
  doc.querySelectorAll("[poster]").forEach((el) => {
    const p = el.getAttribute("poster")
    if (p) el.setAttribute("poster", toAbs(p))
  })
}

interface CodeNodeStats {
  preCount: number
  codeCount: number
  blockCodeCount: number
}

function isLikelyBlockCodeNode(codeNode: Element): boolean {
  const classText = `${codeNode.className || ""} ${codeNode.parentElement?.className || ""}`.trim()
  if (CODE_CLASS_HINT.test(classText)) return true

  if (codeNode.closest("pre")) return true

  const parentTag = codeNode.parentElement?.tagName ?? ""
  if (INLINE_TEXT_CONTAINERS.has(parentTag)) return false

  const codeText = (codeNode.textContent || "").trim()
  if (codeText.length <= 24) return false

  return codeText.includes("\n")
}

function normalizeCodeWrappers(root: ParentNode): void {
  const codeNodes = Array.from(root.querySelectorAll("code"))
  codeNodes.forEach((codeNode) => {
    if (codeNode.closest("pre")) return
    if (!isLikelyBlockCodeNode(codeNode)) return

    const pre = root.ownerDocument?.createElement("pre") ?? document.createElement("pre")
    codeNode.parentNode?.insertBefore(pre, codeNode)
    pre.appendChild(codeNode)
  })
}

function getCodeNodeStats(html: string): CodeNodeStats {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  normalizeCodeWrappers(doc.body)
  const codeNodes = Array.from(doc.querySelectorAll("code"))
  const blockCodeCount = codeNodes.filter((codeNode) => isLikelyBlockCodeNode(codeNode)).length
  return {
    preCount: doc.querySelectorAll("pre").length,
    codeCount: codeNodes.length,
    blockCodeCount,
  }
}

function readabilityDroppedTooMuchCode(originalHtml: string, readerHtml: string): boolean {
  const original = getCodeNodeStats(originalHtml)
  if (original.preCount <= 0 && original.codeCount <= 0 && original.blockCodeCount <= 0) return false

  const extracted = getCodeNodeStats(readerHtml)
  if (original.blockCodeCount > 0 && extracted.blockCodeCount === 0) return true
  if (original.preCount > 0 && extracted.preCount === 0) return true
  if (
    original.blockCodeCount >= 2 &&
    extracted.blockCodeCount < Math.max(1, Math.floor(original.blockCodeCount * 0.5))
  ) {
    return true
  }
  if (original.codeCount >= 3 && extracted.codeCount === 0) return true
  if (original.codeCount >= 6 && extracted.codeCount < Math.max(2, Math.floor(original.codeCount * 0.35)))
    return true
  return false
}

function extractSemanticMainHtml(html: string): string | null {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  const candidates = Array.from(doc.querySelectorAll("article, main, [role='main']"))
  if (candidates.length <= 0) return null

  const bestCandidate = candidates
    .map((candidate) => {
      const candidateHtml = candidate.innerHTML
      const stats = getCodeNodeStats(candidateHtml)
      const textLength = (candidate.textContent || "").trim().length
      const score = textLength + stats.blockCodeCount * 250 + stats.preCount * 200 + stats.codeCount * 40
      return { candidateHtml, score, textLength, stats }
    })
    .sort((a, b) => b.score - a.score)
    .find(({ textLength, stats }) => textLength >= 280 || stats.blockCodeCount > 0 || stats.preCount > 0)

  return bestCandidate?.candidateHtml ?? null
}

const SANITIZE_OPTIONS = {
  ALLOWED_TAGS: [
    "h1", "h2", "h3", "h4", "h5", "h6",
    "p", "br", "hr",
    "strong", "b", "em", "i", "u", "s", "strike", "del",
    "code", "pre",
    "ul", "ol", "li",
    "blockquote",
    "a", "img", "iframe",
    "table", "thead", "tbody", "tr", "th", "td",
    "div", "span", "section", "article", "main",
    "figure", "figcaption", "time",
  ],
  ALLOWED_ATTR: [
    "href", "src", "alt", "title", "class", "datetime",
    "allow", "allowfullscreen", "frameborder",
  ],
}

function toReaderModeHtml(html: string, baseUrl?: string): string {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  if (baseUrl?.trim()) {
    resolveRelativeUrlsInDocument(doc, baseUrl.trim())
  }
  normalizeCodeWrappers(doc.body)
  const normalizedOriginalHtml = doc.body.innerHTML

  const docClone = doc.cloneNode(true) as Document
  const reader = new Readability(docClone, READABILITY_OPTIONS)
  const article = reader.parse()
  if (article?.content && article.content.trim().length > 0) {
    const readerDoc = new DOMParser().parseFromString(
      `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${article.content}</body></html>`,
      "text/html"
    )
    normalizeCodeWrappers(readerDoc.body)
    const normalizedReaderHtml = readerDoc.body.innerHTML

    if (readabilityDroppedTooMuchCode(normalizedOriginalHtml, normalizedReaderHtml)) {
      const semanticFallbackHtml = extractSemanticMainHtml(normalizedOriginalHtml)
      if (semanticFallbackHtml) return semanticFallbackHtml
      return normalizedOriginalHtml
    }
    return normalizedReaderHtml
  }
  const semanticFallbackHtml = extractSemanticMainHtml(normalizedOriginalHtml)
  if (semanticFallbackHtml) return semanticFallbackHtml
  return normalizedOriginalHtml
}

function isImagePlaceholder(url: string): boolean {
  const u = url.toLowerCase()
  return (
    u.includes("1x1") ||
    u.includes("pixel") ||
    u.includes("spacer") ||
    u.includes("blank") ||
    u.includes("data:image") ||
    u.includes("transparent.gif") ||
    u.includes("tracking")
  )
}

/**
 * 1) Readability + DOMPurify sanitize
 * 2) Rewrite img src to yaba-asset:// placeholders + collect assets
 * 3) TipTap parse → document JSON
 */
function sanitizeReaderHtmlWithPlaceholders(html: string, baseUrl?: string): { htmlWithPlaceholders: string; assets: ConverterAsset[] } {
  const readerHtml = toReaderModeHtml(html, baseUrl)
  const clean = DOMPurify.sanitize(readerHtml, SANITIZE_OPTIONS)

  const wrapper = document.createElement("div")
  wrapper.innerHTML = clean
  normalizeCodeWrappers(wrapper)

  if (baseUrl) {
    wrapper.querySelectorAll("a[href]").forEach((a) => {
      const href = a.getAttribute("href")?.trim()
      if (href && !href.startsWith("#") && !href.startsWith("mailto:") && !href.startsWith("http")) {
        a.setAttribute("href", resolveUrl(baseUrl, href))
      }
    })
  }

  const assets: ConverterAsset[] = []
  const seenUrls = new Map<string, string>()

  const imgs = wrapper.querySelectorAll("img")
  imgs.forEach((img) => {
    const src = img.getAttribute("src")?.trim()
    if (!src || src.startsWith("data:") || isImagePlaceholder(src)) return

    const resolvedUrl = resolveUrl(baseUrl, src)
    const alt = img.getAttribute("alt")?.trim() || undefined

    let placeholder: string
    const existing = seenUrls.get(resolvedUrl)
    if (existing) {
      placeholder = existing
    } else {
      placeholder = `${ASSET_PLACEHOLDER_PREFIX}${assets.length}`
      seenUrls.set(resolvedUrl, placeholder)
      assets.push({ placeholder, url: resolvedUrl, alt })
    }
    img.setAttribute("src", placeholder)
  })

  return { htmlWithPlaceholders: wrapper.innerHTML, assets }
}

const EMPTY_DOC_JSON = '{"type":"doc","content":[]}'

let converterTipTapEditor: Editor | null = null

function getConverterTipTapEditor(): Editor {
  if (!converterTipTapEditor) {
    converterTipTapEditor = new Editor({
      extensions: createEditorExtensions(),
      editable: false,
    })
  }
  return converterTipTapEditor
}

function htmlToDocumentJson(html: string): string {
  const ed = getConverterTipTapEditor()
  const payload = html?.trim() ? html : "<p></p>"
  try {
    ed.commands.setContent(payload, { emitUpdate: false })
    return JSON.stringify(ed.getJSON())
  } catch {
    return EMPTY_DOC_JSON
  }
}

async function sanitizeAndConvertWithAssets(html: string, baseUrl?: string): Promise<ConverterOutput> {
  const pageUrl = (baseUrl && baseUrl.trim().length > 0 ? baseUrl.trim() : "https://invalid.invalid") as string
  const { htmlWithPlaceholders, assets } = sanitizeReaderHtmlWithPlaceholders(html, baseUrl)
  const documentJson = htmlToDocumentJson(htmlWithPlaceholders)
  const linkMetadata = await extractLinkMetadata(html, pageUrl)
  return { documentJson, assets, linkMetadata }
}

function createHtmlConversionJobId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID()
  }
  return `html-job-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function createPdfExtractionJobId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID()
  }
  return `pdf-job-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

function createEpubExtractionJobId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID()
  }
  return `epub-job-${Date.now()}-${Math.random().toString(36).slice(2)}`
}

/**
 * Inline EPUB (zip) data URL for bookmark preview — `data:application/epub+zip;base64,...`.
 */
function epubDataUrlToArrayBuffer(dataUrl: string): ArrayBuffer {
  if (!dataUrl.startsWith("data:")) {
    throw new Error("Expected a data: URL for inline EPUB")
  }
  const comma = dataUrl.indexOf(",")
  if (comma < 0) {
    throw new Error("Invalid data URL")
  }
  const meta = dataUrl.slice("data:".length, comma).toLowerCase()
  const segments = meta.split(";").map((s) => s.trim())
  const mime = segments[0]
  if (mime !== "application/epub+zip") {
    throw new Error("Inline EPUB data URL must use application/epub+zip")
  }
  if (!segments.includes("base64")) {
    throw new Error("Inline EPUB data URL must be base64-encoded")
  }
  const b64 = dataUrl.slice(comma + 1).replace(/\s/g, "")
  let binary: string
  try {
    binary = atob(b64)
  } catch {
    throw new Error("Invalid base64 in EPUB data URL")
  }
  const len = binary.length
  const bytes = new Uint8Array(len)
  for (let i = 0; i < len; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  if (len < 4 || bytes[0] !== 0x50 || bytes[1] !== 0x4b) {
    throw new Error("Decoded data URL is not a ZIP/EPUB file")
  }
  return bytes.buffer
}

async function loadEpubBytes(epubUrl: string): Promise<ArrayBuffer> {
  if (epubUrl.startsWith("data:")) {
    return epubDataUrlToArrayBuffer(epubUrl)
  }
  const response = await fetch(epubUrl)
  if (!response.ok) throw new Error(`Failed to fetch EPUB: ${response.status}`)
  return response.arrayBuffer()
}

async function blobToDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error("read failed"))
    reader.readAsDataURL(blob)
  })
}

function epubMetaText(v: unknown): string | null {
  if (v == null) return null
  if (typeof v === "string") {
    const t = v.trim()
    return t.length > 0 ? t : null
  }
  if (typeof v === "object" && v !== null) {
    const o = v as Record<string, unknown>
    if (typeof o["#text"] === "string") {
      const t = o["#text"].trim()
      return t.length > 0 ? t : null
    }
  }
  const s = String(v).trim()
  return s.length > 0 ? s : null
}

async function extractEpubCover(input: EpubExtractionInput): Promise<EpubExtractionOutput> {
  const data = await loadEpubBytes(input.epubUrl)
  const book = ePub(data)
  await book.ready
  let coverPngDataUrl: StringOrNull = null
  try {
    const coverUrl = await book.coverUrl()
    if (coverUrl) {
      const r = await fetch(coverUrl)
      const blob = await r.blob()
      coverPngDataUrl = await blobToDataUrl(blob)
    }
  } catch {
    coverPngDataUrl = null
  }

  const md = (book.packaging?.metadata || {}) as unknown as Record<string, unknown>
  const title = epubMetaText(md.title)
  let author = epubMetaText(md.creator)
  if (!author) author = epubMetaText(md.publisher)
  const description = epubMetaText(md.description)
  const pubdate = epubMetaText(md.pubdate) ?? epubMetaText(md["dc:date"])

  try {
    await book.destroy()
  } catch {
    /* ignore */
  }
  return {
    coverPngDataUrl,
    title,
    author,
    description,
    pubdate,
  }
}

/**
 * Decode an inline PDF data URL without fetch() so strict CSP (connect-src without data:) still allows
 * bookmark preview extraction. Only `data:application/pdf;base64,...` is accepted — not arbitrary data: URLs.
 */
function pdfDataUrlToArrayBuffer(dataUrl: string): ArrayBuffer {
  if (!dataUrl.startsWith("data:")) {
    throw new Error("Expected a data: URL for inline PDF")
  }
  const comma = dataUrl.indexOf(",")
  if (comma < 0) {
    throw new Error("Invalid data URL")
  }
  const meta = dataUrl.slice("data:".length, comma).toLowerCase()
  const segments = meta.split(";").map((s) => s.trim())
  const mime = segments[0]
  if (mime !== "application/pdf") {
    throw new Error("Inline PDF data URL must use application/pdf")
  }
  if (!segments.includes("base64")) {
    throw new Error("Inline PDF data URL must be base64-encoded")
  }
  const b64 = dataUrl.slice(comma + 1).replace(/\s/g, "")
  let binary: string
  try {
    binary = atob(b64)
  } catch {
    throw new Error("Invalid base64 in PDF data URL")
  }
  const len = binary.length
  const bytes = new Uint8Array(len)
  for (let i = 0; i < len; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  // Reject non-PDF payloads even if MIME was forged
  if (len < 5 || String.fromCharCode(bytes[0], bytes[1], bytes[2], bytes[3]) !== "%PDF") {
    throw new Error("Decoded data URL is not a PDF file")
  }
  return bytes.buffer
}

async function loadPdfBytes(pdfUrl: string): Promise<ArrayBuffer> {
  if (pdfUrl.startsWith("data:")) {
    return pdfDataUrlToArrayBuffer(pdfUrl)
  }
  const response = await fetch(pdfUrl)
  if (!response.ok) throw new Error(`Failed to fetch PDF: ${response.status}`)
  return response.arrayBuffer()
}

async function extractPdfPreviewAndText(input: PdfExtractionInput): Promise<PdfExtractionOutput> {
  const data = await loadPdfBytes(input.pdfUrl)
  const loadingTask = getDocument({ data })
  const pdfDocument = await loadingTask.promise

  try {
    const metadata = await pdfDocument.getMetadata().catch(() => null)
    const info = metadata?.info as {
      Title?: string
      Author?: string
      Subject?: string
      CreationDate?: string
    } | undefined
    const titleCandidate = info?.Title ?? null
    const title = titleCandidate && titleCandidate.trim().length > 0 ? titleCandidate.trim() : null
    const authorRaw = info?.Author
    const author =
      authorRaw && String(authorRaw).trim().length > 0 ? String(authorRaw).trim() : null
    const subjectRaw = info?.Subject
    const subject =
      subjectRaw && String(subjectRaw).trim().length > 0 ? String(subjectRaw).trim() : null
    const creationRaw = info?.CreationDate
    const creationDate =
      creationRaw && String(creationRaw).trim().length > 0 ? String(creationRaw).trim() : null
    const sections: PdfTextSection[] = []
    const pageCount = pdfDocument.numPages
    const renderScale = input.renderScale && input.renderScale > 0 ? input.renderScale : 1.2
    let firstPagePngDataUrl: StringOrNull = null

    for (let pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
      const page = await pdfDocument.getPage(pageNumber)
      const textContent = await page.getTextContent()
      const text = textContent.items
        .map((item) => ("str" in item ? item.str : ""))
        .join(" ")
        .replace(/\s+/g, " ")
        .trim()
      sections.push({
        sectionKey: `page-${pageNumber - 1}`,
        text,
      })

      if (pageNumber === 1) {
        const viewport = page.getViewport({ scale: renderScale })
        const canvas = document.createElement("canvas")
        const context = canvas.getContext("2d")
        if (!context) throw new Error("Failed to create canvas context for PDF preview")

        canvas.width = Math.max(1, Math.floor(viewport.width))
        canvas.height = Math.max(1, Math.floor(viewport.height))

        await page.render({
          canvas,
          canvasContext: context,
          viewport,
        }).promise
        firstPagePngDataUrl = canvas.toDataURL("image/png")
      }
    }

    return {
      title,
      author,
      subject,
      creationDate,
      pageCount,
      firstPagePngDataUrl,
      sections,
    }
  } finally {
    await pdfDocument.destroy().catch(() => undefined)
  }
}

export function initConverterBridge(): void {
  const win = window as Window & { YabaConverterBridge?: YabaConverterBridge }
  win.YabaConverterBridge = {
    sanitizeAndConvertHtmlToReaderHtml(input: ConverterInput): Promise<ConverterOutput> {
      return sanitizeAndConvertWithAssets(input.html, input.baseUrl)
    },
    startHtmlConversion(input: ConverterInput): string {
      const jobId = createHtmlConversionJobId()
      htmlConversionJobs.set(jobId, { status: "pending" })
      void sanitizeAndConvertWithAssets(input.html, input.baseUrl)
        .then((output) => {
          htmlConversionJobs.set(jobId, {
            status: "done",
            output,
          })
        })
        .catch((error) => {
          htmlConversionJobs.set(jobId, {
            status: "error",
            error: error instanceof Error ? error.message : "Unknown HTML conversion error",
          })
        })
      return jobId
    },
    getHtmlConversionJob(jobId: string): HtmlConversionJobState | null {
      return htmlConversionJobs.get(jobId) ?? null
    },
    deleteHtmlConversionJob(jobId: string): void {
      htmlConversionJobs.delete(jobId)
    },
    startPdfExtraction(input: PdfExtractionInput): string {
      const jobId = createPdfExtractionJobId()
      pdfExtractionJobs.set(jobId, { status: "pending" })

      void extractPdfPreviewAndText(input)
        .then((output) => {
          pdfExtractionJobs.set(jobId, {
            status: "done",
            output,
          })
        })
        .catch((error) => {
          pdfExtractionJobs.set(jobId, {
            status: "error",
            error: error instanceof Error ? error.message : "Unknown PDF extraction error",
          })
        })

      return jobId
    },
    getPdfExtractionJob(jobId: string): PdfExtractionJobState | null {
      return pdfExtractionJobs.get(jobId) ?? null
    },
    deletePdfExtractionJob(jobId: string): void {
      pdfExtractionJobs.delete(jobId)
    },
    startEpubExtraction(input: EpubExtractionInput): string {
      const jobId = createEpubExtractionJobId()
      epubExtractionJobs.set(jobId, { status: "pending" })

      void extractEpubCover(input)
        .then((output) => {
          epubExtractionJobs.set(jobId, {
            status: "done",
            output,
          })
        })
        .catch((error) => {
          epubExtractionJobs.set(jobId, {
            status: "error",
            error: error instanceof Error ? error.message : "Unknown EPUB extraction error",
          })
        })

      return jobId
    },
    getEpubExtractionJob(jobId: string): EpubExtractionJobState | null {
      return epubExtractionJobs.get(jobId) ?? null
    },
    deleteEpubExtractionJob(jobId: string): void {
      epubExtractionJobs.delete(jobId)
    },
  }
}
