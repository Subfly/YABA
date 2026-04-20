/**
 * Table of contents extraction and navigation for the Milkdown/ProseMirror editor.
 */
import type { EditorView } from "@milkdown/prose/view"
import { TextSelection } from "@milkdown/prose/state"
import { publishToc, type TocJson } from "./toc-host-events"
import { getBridgeView } from "./editor-bridge-shared"

const TOC_PUBLISH_DEBOUNCE_MS = 1000
let tocPublishTimer: ReturnType<typeof setTimeout> | null = null

function clearTocPublishTimer(): void {
  if (tocPublishTimer !== null) {
    clearTimeout(tocPublishTimer)
    tocPublishTimer = null
  }
}

export type HeadingEntry = { pos: number; level: number; text: string }

/** Single document walk: non-empty headings in document order. */
export function collectHeadingEntries(doc: EditorView["state"]["doc"]): HeadingEntry[] {
  const headings: HeadingEntry[] = []
  doc.descendants((node, pos) => {
    if (node.type.name === "heading") {
      const level = Math.min(6, Math.max(1, Number(node.attrs.level) || 1))
      const text = node.textContent.trim()
      if (text.length > 0) {
        headings.push({ pos, level, text })
      }
    }
    return true
  })
  return headings
}

function buildNestedTocFromHeadings(headings: HeadingEntry[]): TocJson | null {
  if (headings.length === 0) return null

  type Item = {
    id: string
    title: string
    level: number
    children: Item[]
    extrasJson?: string | null
  }

  const root: Item[] = []
  const stack: { level: number; children: Item[] }[] = [{ level: 0, children: root }]
  let idCounter = 0

  for (const h of headings) {
    const id = `toc-h-${idCounter++}`
    const extrasJson = JSON.stringify({ pos: h.pos })
    const item: Item = {
      id,
      title: h.text,
      level: h.level,
      children: [],
      extrasJson,
    }
    while (stack.length > 1 && stack[stack.length - 1].level >= h.level) {
      stack.pop()
    }
    stack[stack.length - 1].children.push(item)
    stack.push({ level: h.level, children: item.children })
  }

  return { items: root }
}

export function buildHeadingTocJson(view: EditorView): TocJson | null {
  return buildNestedTocFromHeadings(collectHeadingEntries(view.state.doc))
}

function parseHeadingPosFromExtras(extrasJson: string | null | undefined): number | null {
  if (!extrasJson?.trim()) return null
  try {
    const extra = JSON.parse(extrasJson) as { pos?: unknown }
    if (typeof extra.pos === "number" && Number.isFinite(extra.pos) && extra.pos >= 0) {
      return extra.pos
    }
  } catch {
    /* ignore */
  }
  return null
}

function findHeadingDocPosForTocItemId(view: EditorView, tocItemId: string): number | null {
  const m = /^toc-h-(\d+)$/.exec(tocItemId)
  if (!m) return null
  const targetIndex = parseInt(m[1], 10)
  if (targetIndex < 0 || !Number.isFinite(targetIndex)) return null
  const headings = collectHeadingEntries(view.state.doc)
  const h = headings[targetIndex]
  return h?.pos ?? null
}

function getHeadingIndexFromTocItemId(tocItemId: string): number | null {
  const m = /^toc-h-(\d+)$/.exec(tocItemId)
  if (!m) return null
  const index = parseInt(m[1], 10)
  if (!Number.isFinite(index) || index < 0) return null
  return index
}

function findHeadingElementForTocItemId(view: EditorView, tocItemId: string): HTMLElement | null {
  const headingIndex = getHeadingIndexFromTocItemId(tocItemId)
  if (headingIndex == null) return null
  const headingElements = Array.from(
    view.dom.querySelectorAll<HTMLElement>("h1, h2, h3, h4, h5, h6"),
  ).filter((el) => el.textContent?.trim().length)
  return headingElements[headingIndex] ?? null
}

function scrollHeadingIntoEditorView(view: EditorView, headingEl: HTMLElement): void {
  const container =
    (view.dom.closest("[data-yaba-editor-root]") as HTMLElement | null) ??
    (document.querySelector(".yaba-editor-container") as HTMLElement | null)

  if (!container) {
    headingEl.scrollIntoView({ behavior: "smooth", block: "center" })
    return
  }

  const containerRect = container.getBoundingClientRect()
  const headingRect = headingEl.getBoundingClientRect()
  const desiredTop =
    container.scrollTop +
    (headingRect.top - containerRect.top) -
    (container.clientHeight / 2 - headingRect.height / 2)
  const maxTop = Math.max(0, container.scrollHeight - container.clientHeight)
  const top = Math.max(0, Math.min(desiredTop, maxTop))
  container.scrollTo({ top, behavior: "smooth" })
}

export function publishHeadingTocFromEditor(): void {
  const view = getBridgeView()
  if (!view) {
    publishToc(null)
    return
  }
  publishToc(buildHeadingTocJson(view))
}

export function scheduleHeadingTocPublish(): void {
  const page = typeof document !== "undefined" ? document.body?.dataset.yabaPage : undefined
  if (page !== "editor" && page !== "viewer") return
  clearTocPublishTimer()
  tocPublishTimer = setTimeout(() => {
    tocPublishTimer = null
    publishHeadingTocFromEditor()
  }, TOC_PUBLISH_DEBOUNCE_MS)
}

export function resetTocPublishScheduling(): void {
  clearTocPublishTimer()
}

export function navigateToTocItemInView(
  view: EditorView,
  id: string,
  extrasJson?: string | null,
): void {
  const fromExtras = parseHeadingPosFromExtras(extrasJson)
  const nodePos = fromExtras ?? findHeadingDocPosForTocItemId(view, id)
  if (nodePos == null) return
  try {
    const docSize = view.state.doc.content.size
    const headingEl = findHeadingElementForTocItemId(view, id)
    queueMicrotask(() => {
      if (headingEl) {
        scrollHeadingIntoEditorView(view, headingEl)
      }
    })
    const inside = Math.min(nodePos + 1, docSize)
    const sel = TextSelection.create(view.state.doc, inside)
    view.dispatch(view.state.tr.setSelection(sel))
  } catch {
    /* ignore */
  }
}
