/**
 * Text-layer helpers for YABA PDF anchors (section keys `page-{0-based index}` + offsets).
 */

export interface PdfSelectionSnapshot {
  startSectionKey: string
  startOffsetInSection: number
  endSectionKey: string
  endOffsetInSection: number
  selectedText: string
  prefixText?: string
  suffixText?: string
}

export interface PdfHighlightForRendering {
  id: string
  startSectionKey: string
  startOffsetInSection: number
  endSectionKey: string
  endOffsetInSection: number
  colorRole: string
}

export interface PdfTextSpanRange {
  element: HTMLSpanElement
  start: number
  end: number
}

export interface PdfPageState {
  pageIndex: number
  textLayerElement: HTMLElement
  textContent: string
  textSpanRanges: PdfTextSpanRange[]
}

export const CONTEXT_WINDOW = 30

export function parsePageIndex(sectionKey: string): number | null {
  if (!sectionKey.startsWith("page-")) return null
  const parsed = Number.parseInt(sectionKey.replace("page-", ""), 10)
  return Number.isFinite(parsed) ? parsed : null
}

export function comparePosition(
  leftSectionKey: string,
  leftOffset: number,
  rightSectionKey: string,
  rightOffset: number,
): number {
  const leftPage = parsePageIndex(leftSectionKey)
  const rightPage = parsePageIndex(rightSectionKey)
  if (leftPage === null || rightPage === null) return 0
  if (leftPage < rightPage) return -1
  if (leftPage > rightPage) return 1
  if (leftOffset < rightOffset) return -1
  if (leftOffset > rightOffset) return 1
  return 0
}

export function rangesOverlap(
  a: PdfSelectionSnapshot,
  b: PdfHighlightForRendering,
): boolean {
  const aStartBeforeBEnd =
    comparePosition(
      a.startSectionKey,
      a.startOffsetInSection,
      b.endSectionKey,
      b.endOffsetInSection,
    ) < 0
  const bStartBeforeAEnd =
    comparePosition(
      b.startSectionKey,
      b.startOffsetInSection,
      a.endSectionKey,
      a.endOffsetInSection,
    ) < 0
  return aStartBeforeBEnd && bStartBeforeAEnd
}

export function computeOffsetInLayer(
  textLayer: HTMLElement,
  node: Node,
  offset: number,
): number {
  const range = document.createRange()
  range.selectNodeContents(textLayer)
  range.setEnd(node, offset)
  return range.toString().length
}

export function buildSpanRanges(textLayerElement: HTMLElement): {
  textContent: string
  textSpanRanges: PdfTextSpanRange[]
} {
  const spans = Array.from(textLayerElement.querySelectorAll("span"))
  let cursor = 0
  const textSpanRanges: PdfTextSpanRange[] = []
  spans.forEach((span) => {
    const text = span.textContent ?? ""
    const start = cursor
    const end = start + text.length
    cursor = end
    textSpanRanges.push({
      element: span,
      start,
      end,
    })
  })

  return {
    textContent: textLayerElement.textContent ?? "",
    textSpanRanges,
  }
}

export function createRangeByOffsets(
  textLayer: HTMLElement,
  startOffset: number,
  endOffset: number,
): Range | null {
  const range = document.createRange()
  const walker = document.createTreeWalker(textLayer, NodeFilter.SHOW_TEXT)
  let current = 0
  let startNode: Node | null = null
  let startOffsetInNode = 0
  let endNode: Node | null = null
  let endOffsetInNode = 0
  let node: Node | null
  while ((node = walker.nextNode())) {
    const len = node.textContent?.length ?? 0
    if (startNode === null && current + len > startOffset) {
      startNode = node
      startOffsetInNode = Math.max(0, startOffset - current)
    }
    if (endNode === null && current + len >= endOffset) {
      endNode = node
      endOffsetInNode = Math.min(len, endOffset - current)
    }
    if (startNode && endNode) break
    current += len
  }
  if (!startNode || !endNode) return null
  try {
    range.setStart(startNode, startOffsetInNode)
    range.setEnd(endNode, endOffsetInNode)
    return range
  } catch {
    return null
  }
}

function getTextLayerElements(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll(".textLayer")) as HTMLElement[]
}

export function rebuildPageStatesFromDom(root: HTMLElement): PdfPageState[] {
  return getTextLayerElements(root).map((textLayerElement) => {
    const pageElement = textLayerElement.closest(".page") as HTMLElement | null
    const pageNumberRaw = pageElement?.dataset.pageNumber ?? "1"
    const pageIndex = Math.max(0, Number.parseInt(pageNumberRaw, 10) - 1)
    const { textContent, textSpanRanges } = buildSpanRanges(textLayerElement)
    return {
      pageIndex,
      textLayerElement,
      textContent,
      textSpanRanges,
    }
  })
}

function findPageStateByLayerNode(
  pageStates: PdfPageState[],
  node: Node | null,
): PdfPageState | null {
  if (!node) return null
  const element =
    node instanceof Element ? node : node.parentElement
  const textLayer = element?.closest(".textLayer")
  if (!textLayer) return null
  return pageStates.find((state) => state.textLayerElement === textLayer) ?? null
}

export function getSelectionSnapshotFromRoot(
  root: HTMLElement,
): PdfSelectionSnapshot | null {
  const pageStates = rebuildPageStatesFromDom(root)
  const selection = window.getSelection()
  if (!selection || selection.rangeCount <= 0 || selection.isCollapsed) return null
  const range = selection.getRangeAt(0)
  const startPageState = findPageStateByLayerNode(pageStates, range.startContainer)
  const endPageState = findPageStateByLayerNode(pageStates, range.endContainer)
  if (!startPageState || !endPageState) return null

  const selectedText = selection.toString().trim()
  if (!selectedText) return null

  const startOffsetInSection = computeOffsetInLayer(
    startPageState.textLayerElement,
    range.startContainer,
    range.startOffset,
  )
  const endOffsetInSection = computeOffsetInLayer(
    endPageState.textLayerElement,
    range.endContainer,
    range.endOffset,
  )

  const prefixText = startPageState.textContent
    .slice(Math.max(0, startOffsetInSection - CONTEXT_WINDOW), startOffsetInSection)
    .trim()
  const suffixText = endPageState.textContent
    .slice(
      endOffsetInSection,
      Math.min(endPageState.textContent.length, endOffsetInSection + CONTEXT_WINDOW),
    )
    .trim()

  return {
    startSectionKey: `page-${startPageState.pageIndex}`,
    startOffsetInSection,
    endSectionKey: `page-${endPageState.pageIndex}`,
    endOffsetInSection,
    selectedText,
    prefixText: prefixText || undefined,
    suffixText: suffixText || undefined,
  }
}

export function getCanCreateAnnotationFromRoot(
  root: HTMLElement,
  currentHighlights: PdfHighlightForRendering[],
): boolean {
  const snapshot = getSelectionSnapshotFromRoot(root)
  if (!snapshot) return false
  return !currentHighlights.some((highlight) => rangesOverlap(snapshot, highlight))
}
