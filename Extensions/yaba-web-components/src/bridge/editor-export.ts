/**
 * Export note editor content to Markdown or PDF (Milkdown / Crepe).
 */
import type { Crepe } from "@milkdown/crepe"
import { editorViewCtx } from "@milkdown/core"
import html2pdf from "html2pdf.js"
import { normalizeMarkdownAssetPathsForPersistence } from "@/milkdown/markdown-assets"
import { postToYabaNativeHost } from "./yaba-native-host"

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
 * Markdown for Save Copy; optional base URL context for canonical asset paths.
 */
export function exportMarkdownFromCrepe(crepe: Crepe, lastAssetsBaseUrl: string | undefined): string {
  let markdown = ""
  try {
    markdown = (crepe.getMarkdown() ?? "").trim()
  } catch {
    markdown = ""
  }
  markdown = normalizeMarkdownAssetPathsForPersistence(markdown, lastAssetsBaseUrl)
  markdown = rewriteMarkdownImageUrlsToRelativeAssets(markdown)
  return markdown.trim() + "\n"
}

/**
 * Injected only into html2canvas' cloned document — does not affect the live page.
 */
const PDF_EXPORT_CLONE_STYLE = `
.yaba-editor-container,
.yaba-editor-container .milkdown,
.yaba-editor-container .editor,
[data-yaba-editor-root],
[data-yaba-editor-root] .milkdown,
[data-yaba-editor-root] .editor,
.milkdown .editor {
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

export function startEditorPdfExportJob(crepe: Crepe, jobId: string): void {
  void runEditorPdfExportJob(crepe, jobId)
}

async function runEditorPdfExportJob(crepe: Crepe, jobId: string): Promise<void> {
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
    const viewDom = crepe.editor.action((ctx) => ctx.get(editorViewCtx).dom as HTMLElement)
    if (!viewDom) {
      finish("error", undefined, "no_editor")
      return
    }

    const element = (viewDom.closest(".yaba-editor-container") ??
      viewDom.closest("[data-yaba-editor-root]") ??
      viewDom) as HTMLElement

    if (!element || !(element.textContent ?? "").trim()) {
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
