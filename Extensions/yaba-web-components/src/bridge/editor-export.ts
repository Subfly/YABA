/**
 * Export note editor content to Markdown (with assets) or PDF.
 *
 * Primary path: Showdown [makeMarkdown] on preprocessed HTML (same family as paste HTML→MD).
 * Fallback: TipTap [@tiptap/markdown] full-document serialize — Showdown can yield empty/undefined on some TipTap HTML.
 */
import type { Editor, JSONContent } from "@tiptap/core"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import DOMPurify from "dompurify"
import { jsPDF } from "jspdf"
import showdown from "showdown"

export interface MarkdownExportAsset {
  relativePath: string
  dataBase64: string
}

export interface MarkdownExportBundle {
  markdown: string
  assets: MarkdownExportAsset[]
}

/** Matches [editor-bridge] paste HTML converter options so export behaves like the rest of the note pipeline. */
const exportMarkdownConverter = new showdown.Converter({
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
})

function guessImageExtension(url: string): string {
  const lower = url.split("?")[0]?.toLowerCase() ?? ""
  if (lower.endsWith(".png")) return "png"
  if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "jpg"
  if (lower.endsWith(".gif")) return "gif"
  if (lower.endsWith(".webp")) return "webp"
  if (lower.endsWith(".svg")) return "svg"
  return "png"
}

/**
 * Synchronous fetch for WebView `evaluateJavascript` — async Promises are not returned to Kotlin.
 * Falls back to null on failure (caller keeps remote URL in markdown).
 */
function fetchUrlAsBase64Sync(url: string): string | null {
  try {
    const xhr = new XMLHttpRequest()
    xhr.open("GET", url, false)
    xhr.responseType = "arraybuffer"
    xhr.send(null)
    if (xhr.status !== 200 && xhr.status !== 0) return null
    const buf = xhr.response as ArrayBuffer | null
    if (!buf || buf.byteLength === 0) return null
    const bytes = new Uint8Array(buf)
    let binary = ""
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]!)
    }
    return btoa(binary)
  } catch {
    return null
  }
}

/**
 * Showdown only knows standard HTML — turn YABA inline nodes and KaTeX into shapes `makeMarkdown` can walk.
 */
function preprocessHtmlForShowdownExport(html: string): string {
  const wrap = document.createElement("div")
  wrap.innerHTML = html

  wrap.querySelectorAll("[data-yaba-inline-link]").forEach((el) => {
    const text = el.getAttribute("data-text") || el.textContent || ""
    const url = el.getAttribute("data-url") || ""
    const a = document.createElement("a")
    a.href = url
    a.textContent = text
    el.replaceWith(a)
  })

  wrap.querySelectorAll("[data-yaba-inline-mention]").forEach((el) => {
    const label =
      el.getAttribute("data-bookmark-label") || el.getAttribute("data-text") || el.textContent || ""
    const id = el.getAttribute("data-bookmark-id") || ""
    const a = document.createElement("a")
    a.href = `bookmark:${id}`
    a.textContent = label
    el.replaceWith(a)
  })

  wrap.querySelectorAll("span.katex:not(.katex-display)").forEach((el) => {
    const ann = el.querySelector("annotation")
    const latex = ann?.textContent?.trim() || ""
    if (latex) {
      el.replaceWith(document.createTextNode(`$${latex}$`))
    }
  })

  wrap.querySelectorAll("span.katex-display").forEach((el) => {
    const ann = el.querySelector("annotation")
    const latex = ann?.textContent?.trim() || ""
    if (latex) {
      const p = document.createElement("p")
      p.textContent = `$$${latex}$$`
      el.replaceWith(p)
    }
  })

  return wrap.innerHTML
}

/**
 * Same strategy as [getSelectionMarkdownFromEditor] in editor-bridge, but for the whole document.
 */
function getPlainTextFallbackFromEditor(ed: Editor): string {
  try {
    const t = ed.getText({ blockSeparator: "\n\n" })
    if (t.trim().length > 0) return t
  } catch {
    /* fall through */
  }
  try {
    const dom = ed.view.dom as HTMLElement
    const inner = dom.innerText?.trim()
    if (inner) return inner
  } catch {
    /* ignore */
  }
  return ""
}

function getFullDocumentMarkdownFromEditor(ed: Editor): string {
  const doc = ed.state.doc
  try {
    const slice = doc.slice(0, doc.content.size)
    const json = slice.content.toJSON() as JSONContent[] | JSONContent | null | undefined
    const content: JSONContent[] = Array.isArray(json) ? json : json != null ? [json] : []
    const docJson: JSONContent = { type: "doc", content }
    const pmDoc = ed.state.schema.nodeFromJSON(docJson)

    const storage = (
      ed as unknown as {
        storage?: {
          markdown?: {
            serializer?: { serialize?: (doc: ProseMirrorNode) => string }
          }
        }
      }
    ).storage?.markdown
    const serializedByStorage = storage?.serializer?.serialize?.(pmDoc)
    if (serializedByStorage != null && serializedByStorage.trim().length > 0) {
      return serializedByStorage
    }

    const mgr = ed.markdown as { serialize?: (doc: JSONContent | ProseMirrorNode) => string } | undefined
    if (mgr?.serialize) {
      const serializedByManager = mgr.serialize(pmDoc)
      if (serializedByManager.trim().length > 0) return serializedByManager
      const serializedJson = mgr.serialize(docJson)
      if (serializedJson.trim().length > 0) return serializedJson
    }
  } catch {
    /* fall through */
  }
  try {
    const t = doc.textBetween(0, doc.content.size, "\n")
    if (t.trim().length > 0) return t
  } catch {
    /* fall through */
  }
  return getPlainTextFallbackFromEditor(ed)
}

/**
 * Full-document HTML → Markdown via Showdown [makeMarkdown], then inline images as relative files under `assets/`.
 * Synchronous — [WebView.evaluateJavascript] does not deliver Promise results to Kotlin.
 */
export function exportMarkdownBundleFromEditor(editor: Editor): MarkdownExportBundle {
  const rawHtml = editor.getHTML()
  // Preprocess before DOMPurify so YABA inline nodes (links, mentions, KaTeX) are not stripped first.
  const preprocessed = preprocessHtmlForShowdownExport(rawHtml)
  const safeHtml = DOMPurify.sanitize(preprocessed, { USE_PROFILES: { html: true } })
  const showdownOut = exportMarkdownConverter.makeMarkdown(safeHtml)
  let markdown = typeof showdownOut === "string" ? showdownOut.trim() : String(showdownOut ?? "").trim()
  if (!markdown) {
    markdown = getFullDocumentMarkdownFromEditor(editor).trim()
  }
  if (!markdown) {
    markdown = getPlainTextFallbackFromEditor(editor).trim()
  }

  const assets: MarkdownExportAsset[] = []
  const urlToRel = new Map<string, string>()
  let nextIdx = 0

  const imgPattern = /!\[([^\]]*)\]\(([^)\s]+)\)/g
  let match: RegExpExecArray | null
  const uniqueUrls: string[] = []
  while ((match = imgPattern.exec(markdown)) !== null) {
    const url = match[2]
    if (!urlToRel.has(url)) {
      urlToRel.set(url, `assets/note-img-${nextIdx++}.${guessImageExtension(url)}`)
      uniqueUrls.push(url)
    }
  }

  for (const url of uniqueUrls) {
    const rel = urlToRel.get(url)
    if (!rel) continue
    const dataBase64 = fetchUrlAsBase64Sync(url)
    if (dataBase64 != null) {
      assets.push({ relativePath: rel, dataBase64 })
    } else {
      urlToRel.delete(url)
    }
  }

  for (const [url, rel] of urlToRel) {
    const escaped = url.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
    markdown = markdown.replace(new RegExp(`!\\[([^\\]]*)\\]\\(${escaped}\\)`, "g"), (_m, alt) => {
      return `![${alt}](./${rel})`
    })
  }

  const body = (markdown || "").trim() + "\n"
  return { markdown: body, assets }
}

/**
 * Plain-text PDF via jsPDF (synchronous). Pixel-accurate HTML→canvas would require async html2canvas,
 * which cannot return through [WebView.evaluateJavascript] on Android.
 */
export function exportPdfBase64FromEditor(editor: Editor): string {
  const text = getPlainTextFallbackFromEditor(editor).trim()
  if (!text) return ""

  const pdf = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" })
  const pageWidth = pdf.internal.pageSize.getWidth()
  const pageHeight = pdf.internal.pageSize.getHeight()
  const margin = 40
  const maxWidth = pageWidth - margin * 2
  const lineHeight = 14
  const lines = pdf.splitTextToSize(text, maxWidth)
  let y = margin
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]!
    if (y + lineHeight > pageHeight - margin) {
      pdf.addPage()
      y = margin
    }
    pdf.text(line, margin, y)
    y += lineHeight
  }

  const out = pdf.output("datauristring") as string
  const comma = out.indexOf(",")
  return comma >= 0 ? out.slice(comma + 1) : out
}
