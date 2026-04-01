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
import html2pdf from "html2pdf.js"
import showdown from "showdown"
import { postToYabaNativeHost } from "./yaba-native-host"

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
 * Injected only into html2canvas' cloned document — does not affect the live page.
 * Forces readable black body copy; theme grays (--yaba-reader-on-bg, etc.) stay on screen only.
 */
const PDF_EXPORT_CLONE_STYLE = `
.yaba-editor-container,
.yaba-editor-container .ProseMirror,
[data-yaba-editor-root],
[data-yaba-editor-root] .ProseMirror,
.ProseMirror {
  color: #000000 !important;
}
.yaba-editor-container p,
.yaba-editor-container li,
.yaba-editor-container td,
.yaba-editor-container th,
.yaba-editor-container blockquote,
.yaba-editor-container figcaption,
.yaba-editor-container a,
[data-yaba-editor-root] p,
[data-yaba-editor-root] li,
[data-yaba-editor-root] td,
[data-yaba-editor-root] th,
[data-yaba-editor-root] blockquote,
[data-yaba-editor-root] a {
  color: #000000 !important;
}
`

/**
 * Renders the editor DOM with [html2pdf.js] and posts base64 to the native host.
 * Uses `.yaba-editor-container` when present so theme CSS (`--yaba-font-family`, reader vars) applies to capture.
 */
export function startEditorPdfExportJob(editor: Editor, jobId: string): void {
  void runEditorPdfExportJob(editor, jobId)
}

async function runEditorPdfExportJob(editor: Editor, jobId: string): Promise<void> {
  const finish = (status: "done" | "error", pdfBase64?: string, error?: string) => {
    postToYabaNativeHost({
      type: "editorPdfExport",
      jobId,
      status,
      pdfBase64,
      error,
    })
  }

  try {
    const root = editor.view.dom as HTMLElement
    const element =
      (root.closest(".yaba-editor-container") as HTMLElement | null) ??
      (root.closest("[data-yaba-editor-root]") as HTMLElement | null) ??
      root

    if (!(element.textContent ?? "").trim()) {
      finish("error", undefined, "empty")
      return
    }

    const opt = {
      margin: [12, 16, 12, 16] as [number, number, number, number],
      jsPDF: { unit: "mm" as const, format: "a4" as const, orientation: "portrait" as const },
      html2canvas: {
        onclone: (clonedDoc: Document) => {
          const style = clonedDoc.createElement("style")
          style.setAttribute("data-yaba-pdf-export-only", "")
          style.textContent = PDF_EXPORT_CLONE_STYLE
          clonedDoc.head.appendChild(style)
        },
      },
    }

    const dataUri = (await html2pdf().set(opt).from(element).outputPdf("datauristring")) as string
    const comma = dataUri.indexOf(",")
    const b64 = comma >= 0 ? dataUri.slice(comma + 1) : dataUri
    if (!b64?.trim()) {
      finish("error", undefined, "empty_pdf")
      return
    }
    finish("done", b64)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    finish("error", undefined, msg.slice(0, 500))
  }
}
