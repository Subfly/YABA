/**
 * DOM wiring: clipboard, content taps, chrome insets, focus/cursor, selection publishing.
 */
import type { Crepe } from "@milkdown/crepe"
import { editorViewCtx } from "@milkdown/core"
import type { EditorView } from "@milkdown/prose/view"
import { Selection, TextSelection } from "@milkdown/prose/state"
import DOMPurify from "dompurify"
import { replaceRange } from "@milkdown/utils"
import { getSelectionSnapshot } from "./selection-extractor"
import { selectionOverlapsAnnotationLink } from "@/milkdown/annotation-links"
import { postToYabaNativeHost } from "./yaba-native-host"
import { ANNOTATION_HREF_PREFIX } from "@/milkdown/yaba-href"
import { publishEditorHostState } from "./editor-host-events"
import { getBridgeView, getLastStoredCursor, setLastStoredCursor } from "./editor-bridge-shared"
import type { YabaEditorBridge } from "./editor-bridge-types"
import { readerHtmlToMarkdown } from "./converter-bridge"

export function decodeHtmlEntities(value: string): string {
  const textarea = document.createElement("textarea")
  textarea.innerHTML = value
  return textarea.value
}

export function applySupSubMarkdownSyntax(value: string): string {
  const withSup = value.replace(/(^|[^\^])\^([^^\n]+)\^/g, "$1<sup>$2</sup>")
  return withSup.replace(/(^|[^~])~([^~\n]+)~(?!~)/g, "$1<sub>$2</sub>")
}

/** Inserts plain (or lightly HTML-tagged) clipboard text as Markdown via Crepe. */
export function insertPastedPlainAsMarkdown(crepe: Crepe, clipboardText: string): void {
  const trimmed = clipboardText.trim()
  if (trimmed.length === 0) return

  const sanitizedText = DOMPurify.sanitize(trimmed, { USE_PROFILES: { html: true } })
  const decodedText = decodeHtmlEntities(sanitizedText)
  const markdownReadyText = applySupSubMarkdownSyntax(decodedText)
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    const { from, to } = view.state.selection
    replaceRange(markdownReadyText || decodedText, { from, to })(ctx)
  })
}

export function getEditorDom(crepe: Crepe): HTMLElement | null {
  try {
    return crepe.editor.action((ctx) => ctx.get(editorViewCtx).dom as HTMLElement)
  } catch {
    return null
  }
}

export function wireDefaultMarkdownClipboard(crepe: Crepe): void {
  const dom = getEditorDom(crepe)
  if (!dom) return

  const onCopy = (event: Event) => {
    const ev = event as ClipboardEvent
    const view = getBridgeView()
    if (!view || !view.editable) return
    const { from, to } = view.state.selection
    if (from === to) return
    const md = view.state.doc.textBetween(from, to, "\n").trim()
    if (md.length === 0) return
    ev.preventDefault()
    ev.clipboardData?.setData("text/plain", md)
    ev.clipboardData?.setData("text/markdown", md)
  }

  const handlePasteWithMarkdownPipeline = (ev: ClipboardEvent): boolean => {
    const view = getBridgeView()
    if (!view?.editable) return false
    const html = ev.clipboardData?.getData("text/html")
    const plain = ev.clipboardData?.getData("text/plain") ?? ""
    if (html && html.trim() !== "") {
      ev.preventDefault()
      const safe = DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
      const md = readerHtmlToMarkdown(safe).trim()
      crepe.editor.action((ctx) => {
        const v = ctx.get(editorViewCtx)
        const { from, to } = v.state.selection
        replaceRange(md || plain.trim(), { from, to })(ctx)
      })
      return true
    }
    if (plain.trim() === "") return false
    ev.preventDefault()
    insertPastedPlainAsMarkdown(crepe, plain)
    return true
  }

  dom.addEventListener("copy", onCopy)
  dom.addEventListener(
    "paste",
    (ev: ClipboardEvent) => {
      if (handlePasteWithMarkdownPipeline(ev)) {
        ev.stopPropagation()
      }
    },
    true,
  )
}

export function wireNativeContentTaps(crepe: Crepe): void {
  const root = getEditorDom(crepe)
  if (!root) return

  const onClick = (ev: MouseEvent) => {
    const target = ev.target as HTMLElement | null
    if (!target) return

    const ann = target.closest<HTMLAnchorElement>('a[href^="yaba-annotation:"]')
    if (ann) {
      const href = ann.getAttribute("href") ?? ""
      const id = href
        .replace(
          new RegExp(`^${ANNOTATION_HREF_PREFIX.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}`),
          "",
        )
        .split(/[?#]/)[0]
      postToYabaNativeHost({ type: "annotationTap", id })
      const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
      win.YabaEditorBridge?.onAnnotationTap?.(id)
      ev.preventDefault()
      return
    }

    const bm = target.closest<HTMLAnchorElement>('a[href^="bookmark:"]')
    if (bm) {
      const href = bm.getAttribute("href") ?? ""
      const bookmarkId = href.replace(/^bookmark:/, "")
      const text = bm.textContent ?? ""
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(bm, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "inlineMentionTap",
        pos,
        text,
        bookmarkId,
        bookmarkKindCode: 0,
        bookmarkLabel: text,
      })
      ev.preventDefault()
      return
    }

    const httpLink = target.closest<HTMLAnchorElement>("a[href]")
    if (httpLink) {
      const href = httpLink.getAttribute("href") ?? ""
      if (href.startsWith("yaba-annotation:") || href.startsWith("bookmark:")) return
      if (!/^https?:\/\//i.test(href) && !href.startsWith("mailto:")) return
      const text = httpLink.textContent ?? ""
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(httpLink, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "inlineLinkTap",
        pos,
        text,
        url: href,
      })
      ev.preventDefault()
      return
    }

    const katexEl = target.closest(".katex") as HTMLElement | null
    if (katexEl) {
      const annEl = katexEl.querySelector("annotation")
      const latex = annEl?.textContent?.trim() ?? ""
      const block = katexEl.closest(".katex-display")
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(katexEl, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "mathTap",
        kind: block ? "block" : "inline",
        pos,
        latex,
      })
      ev.preventDefault()
    }
  }

  root.addEventListener("click", onClick, true)
}

export function applyWebChromeInsetsToDocument(topChromeInsetPx: number): void {
  const root = document.documentElement
  const total = Math.max(0, Math.round(topChromeInsetPx))
  root.style.setProperty("--yaba-web-chrome-status-bar", `${total}px`)
  root.style.setProperty("--yaba-web-chrome-top-bar", "0px")
  root.style.setProperty("--yaba-web-chrome-safe-area-top-additional", "0px")
}

export function captureStoredCursorFromEditor(): void {
  const view = getBridgeView()
  if (!view) return
  const { anchor, head } = view.state.selection
  setLastStoredCursor({ anchor, head })
}

export function clearSelectionToCaretAtStart(view: EditorView): void {
  const pos = Selection.atStart(view.state.doc).from
  const selection = TextSelection.create(view.state.doc, pos)
  const tr = view.state.tr.setSelection(selection)
  view.dispatch(tr)
}

export function applyInitialFocusStateAfterContent(crepe: Crepe): void {
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    clearSelectionToCaretAtStart(view)
    const { anchor, head } = view.state.selection
    setLastStoredCursor({ anchor, head })
    view.dom.blur()
  })
  const container =
    (document.querySelector("[data-yaba-editor-root]") as HTMLElement | null) ??
    (document.querySelector(".yaba-editor-container") as HTMLElement | null)
  container?.scrollTo({ top: 0, behavior: "auto" })
}

export function getCanCreateAnnotationForCurrentSelection(): boolean {
  const view = getBridgeView()
  const snap = getSelectionSnapshot(view)
  if (!snap || !view) return false
  const { from, to } = view.state.selection
  if (from === to) return false
  const page = document.body?.dataset.yabaPage
  if (page === "editor") return true
  return !selectionOverlapsAnnotationLink(view.state.doc, from, to)
}

export function publishCurrentEditorState(): void {
  publishEditorHostState(getBridgeView(), getCanCreateAnnotationForCurrentSelection)
}

export function focusEditorRestoringCursor(crepe: Crepe | null): void {
  if (!crepe) return
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    try {
      const stored = getLastStoredCursor()
      if (stored) {
        const size = view.state.doc.content.size
        const from = Math.max(0, Math.min(stored.anchor, size))
        const to = Math.max(0, Math.min(stored.head, size))
        const tr = view.state.tr.setSelection(TextSelection.create(view.state.doc, from, to))
        view.dispatch(tr)
      }
    } catch {
      /* ignore */
    }
    view.focus()
  })
}

export function withPreparedEditorSelection(crepe: Crepe | null, run: (editor: Crepe) => void): void {
  if (!crepe) return
  focusEditorRestoringCursor(crepe)
  run(crepe)
  captureStoredCursorFromEditor()
  publishCurrentEditorState()
}
