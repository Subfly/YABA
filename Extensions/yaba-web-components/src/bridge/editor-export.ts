/**
 * Export note editor content to Markdown or PDF.
 *
 * Primary path: Showdown [makeMarkdown] on preprocessed HTML (same family as paste HTML→MD).
 * Fallback: TipTap [@tiptap/markdown] full-document serialize — Showdown can yield empty/undefined on some TipTap HTML.
 *
 * After export, image links are rewritten to `./assets/<filename>` so the host can copy files from app storage.
 */
import type { Editor, JSONContent } from "@tiptap/core"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import DOMPurify from "dompurify"
import { jsPDF } from "jspdf"
import showdown from "showdown"

/** Matches [editor-bridge] paste HTML converter options so export behaves like the rest of the note pipeline. */
const exportMarkdownConverter = new showdown.Converter({
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
})

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
 * Rewrites markdown image targets to `./assets/<filename>` for any URL that points at the note's `assets/` file.
 */
function rewriteMarkdownImageUrlsToRelativeAssets(markdown: string): string {
  const imageRe = /!\[([^\]]*)\]\(([^)]+)\)/g
  return markdown.replace(imageRe, (full, alt, urlRaw) => {
    const url = urlRaw.trim().replace(/^<|>$/g, "").trim()
    const m = url.match(/assets\/([^/?#]+)/)
    if (!m?.[1]) return full
    const fileName = m[1].replace(/>$/, "").trim()
    if (!fileName) return full
    return `![${alt}](./assets/${fileName})`
  })
}

/**
 * Full-document HTML → Markdown via Showdown [makeMarkdown].
 * Synchronous — [WebView.evaluateJavascript] does not deliver Promise results to Kotlin.
 */
export function exportMarkdownFromEditor(editor: Editor): string {
  const html = preprocessHtmlForShowdownExport(editor.getHTML())
  const safeHtml = DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
  let markdown = ""
  try {
    const showdownOut = exportMarkdownConverter.makeMarkdown(safeHtml)
    markdown = typeof showdownOut === "string" ? showdownOut.trim() : String(showdownOut ?? "").trim()
  } catch {
    markdown = ""
  }
  if (!markdown) {
    markdown = getFullDocumentMarkdownFromEditor(editor).trim()
  }
  if (!markdown) {
    markdown = getPlainTextFallbackFromEditor(editor).trim()
  }

  markdown = rewriteMarkdownImageUrlsToRelativeAssets((markdown || "").trim())

  return markdown.trim() + "\n"
}

/**
 * Plain-text PDF via jsPDF (synchronous). Images are replaced with placeholders; no base64 in WebView.
 */
export function exportPdfBase64FromEditor(editor: Editor): string {
  const wrap = document.createElement("div")
  wrap.innerHTML = preprocessHtmlForShowdownExport(editor.getHTML())
  wrap.querySelectorAll("img").forEach((img) => {
    const alt = img.getAttribute("alt")?.trim()
    const marker = document.createTextNode(alt && alt.length > 0 ? `[Image: ${alt}]` : "[Image]")
    img.replaceWith(marker)
  })
  const text = (wrap.innerText || getPlainTextFallbackFromEditor(editor)).trim()
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
