/**
 * Export note editor content to Markdown (with assets) or PDF.
 *
 * HTML→Markdown uses Showdown's [Converter.makeMarkdown] (same stack as Markdown→HTML for paste).
 * YABA-only spans are normalized to plain `<a>` / text before conversion.
 */
import type { Editor } from "@tiptap/core"
import DOMPurify from "dompurify"
import html2canvas from "html2canvas"
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

async function fetchUrlAsBase64(url: string): Promise<string> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`fetch failed ${res.status}`)
  const blob = await res.blob()
  return await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      const dataUrl = reader.result as string
      const idx = dataUrl.indexOf(",")
      resolve(idx >= 0 ? dataUrl.slice(idx + 1) : dataUrl)
    }
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(blob)
  })
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
 * Full-document HTML → Markdown via Showdown [makeMarkdown], then inline images as relative files under `assets/`.
 */
export async function exportMarkdownBundleFromEditor(editor: Editor): Promise<MarkdownExportBundle> {
  const rawHtml = editor.getHTML()
  const safeHtml = DOMPurify.sanitize(rawHtml, { USE_PROFILES: { html: true } })
  const preprocessed = preprocessHtmlForShowdownExport(safeHtml)
  let markdown = exportMarkdownConverter.makeMarkdown(preprocessed)

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
    try {
      const dataBase64 = await fetchUrlAsBase64(url)
      assets.push({ relativePath: rel, dataBase64 })
    } catch {
      /* keep remote URL in markdown if fetch fails */
      urlToRel.delete(url)
    }
  }

  for (const [url, rel] of urlToRel) {
    const escaped = url.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
    markdown = markdown.replace(new RegExp(`!\\[([^\\]]*)\\]\\(${escaped}\\)`, "g"), (_m, alt) => {
      return `![${alt}](./${rel})`
    })
  }

  return { markdown: markdown.trim() + "\n", assets }
}

/**
 * Renders the editor DOM to PDF: DOMPurify → html2canvas → jsPDF (multi-page when needed).
 */
export async function exportPdfBase64FromEditor(editor: Editor): Promise<string> {
  const root = editor.view.dom as HTMLElement
  const host = document.createElement("div")
  host.style.boxSizing = "border-box"
  host.style.width = `${Math.max(root.scrollWidth, root.offsetWidth)}px`
  host.style.padding = "16px"
  host.style.background = "#ffffff"
  host.style.color = "#111111"
  host.innerHTML = DOMPurify.sanitize(root.innerHTML, { USE_PROFILES: { html: true } })
  host.style.position = "fixed"
  host.style.left = "-12000px"
  host.style.top = "0"
  host.style.zIndex = "-1"
  document.body.appendChild(host)

  try {
    const canvas = await html2canvas(host, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      logging: false,
      backgroundColor: "#ffffff",
    })

    const pdf = new jsPDF({ orientation: "portrait", unit: "pt", format: "a4" })
    const pageWidth = pdf.internal.pageSize.getWidth()
    const pageHeight = pdf.internal.pageSize.getHeight()
    const margin = 24
    const imgWidth = pageWidth - margin * 2
    const imgHeight = (canvas.height * imgWidth) / canvas.width

    const imgData = canvas.toDataURL("image/png", 1.0)
    let heightLeft = imgHeight
    let position = margin

    pdf.addImage(imgData, "PNG", margin, position, imgWidth, imgHeight)
    heightLeft -= pageHeight - margin * 2

    while (heightLeft > 0) {
      position = heightLeft - imgHeight
      pdf.addPage()
      pdf.addImage(imgData, "PNG", margin, position, imgWidth, imgHeight)
      heightLeft -= pageHeight - margin * 2
    }

    const out = pdf.output("datauristring") as string
    const comma = out.indexOf(",")
    return comma >= 0 ? out.slice(comma + 1) : out
  } finally {
    document.body.removeChild(host)
  }
}
