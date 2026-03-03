import DOMPurify from "dompurify"
import { Readability } from "@mozilla/readability"
import TurndownService from "turndown"
import { gfm } from "turndown-plugin-gfm"

const YOUTUBE_URL_RE = /(?:youtube\.com\/(?:watch\?v=|embed\/|shorts\/)|youtu\.be\/)([\w-]+)/i

const turndown = new TurndownService({
  headingStyle: "atx",
  codeBlockStyle: "fenced",
  bulletListMarker: "-",
})

turndown.use(gfm)

turndown.addRule("youtubeIframe", {
  filter: (node: HTMLElement) => {
    if (node.tagName !== "IFRAME") return false
    const src = node.getAttribute("src") ?? ""
    return YOUTUBE_URL_RE.test(src)
  },
  replacement: (_content: string, node: HTMLElement) => {
    const src = node.getAttribute("src") ?? ""
    return `\n\n\`\`\`yaba-youtube\n${src}\n\`\`\`\n\n`
  },
})

turndown.addRule("youtubeWrapper", {
  filter: (node: HTMLElement) => {
    if (node.tagName !== "DIV") return false
    return node.hasAttribute("data-youtube-video") || node.querySelector("iframe") !== null && YOUTUBE_URL_RE.test(node.querySelector("iframe")?.getAttribute("src") ?? "")
  },
  replacement: (_content: string, node: HTMLElement) => {
    const iframe = node.querySelector("iframe")
    if (!iframe) return _content
    const src = iframe.getAttribute("src") ?? ""
    if (!YOUTUBE_URL_RE.test(src)) return _content
    return `\n\n\`\`\`yaba-youtube\n${src}\n\`\`\`\n\n`
  },
})

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
  markdown: string
  assets: ConverterAsset[]
}

export interface YabaConverterBridge {
  sanitizeAndConvertHtmlToMarkdown: (input: ConverterInput) => ConverterOutput
}

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
    "allow", "allowfullscreen", "frameborder", "data-youtube-video",
  ],
}

function toReaderModeHtml(html: string, baseUrl?: string): string {
  const doc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  if (baseUrl) {
    const base = doc.createElement("base")
    base.setAttribute("href", baseUrl)
    doc.head.appendChild(base)
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

function extractYouTubeIframes(root: ParentNode): string[] {
  const iframes = Array.from(root.querySelectorAll("iframe"))
  const youtubeHtml: string[] = []
  iframes.forEach((iframe) => {
    const src = iframe.getAttribute("src") ?? ""
    if (YOUTUBE_URL_RE.test(src)) {
      youtubeHtml.push(`<div data-youtube-video><iframe src="${src}"></iframe></div>`)
    }
  })
  return youtubeHtml
}

function sanitizeAndConvertWithAssets(html: string, baseUrl?: string): ConverterOutput {
  const preDoc = new DOMParser().parseFromString(
    `<!DOCTYPE html><html><head><meta charset="UTF-8"></head><body>${html}</body></html>`,
    "text/html"
  )
  const savedYouTubeHtml = extractYouTubeIframes(preDoc.body)

  const readerHtml = toReaderModeHtml(html, baseUrl)
  const clean = DOMPurify.sanitize(readerHtml, SANITIZE_OPTIONS)

  const wrapper = document.createElement("div")
  wrapper.innerHTML = clean
  normalizeCodeWrappers(wrapper)

  const existingYouTube = extractYouTubeIframes(wrapper)
  if (existingYouTube.length === 0 && savedYouTubeHtml.length > 0) {
    savedYouTubeHtml.forEach((yt) => {
      const div = document.createElement("div")
      div.innerHTML = yt
      wrapper.appendChild(div.firstElementChild!)
    })
  }

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

  const markdown = turndown.turndown(wrapper)
  return { markdown, assets }
}

export function initConverterBridge(): void {
  const win = window as Window & { YabaConverterBridge?: YabaConverterBridge }
  win.YabaConverterBridge = {
    sanitizeAndConvertHtmlToMarkdown(input: ConverterInput): ConverterOutput {
      return sanitizeAndConvertWithAssets(input.html, input.baseUrl)
    },
  }
}
