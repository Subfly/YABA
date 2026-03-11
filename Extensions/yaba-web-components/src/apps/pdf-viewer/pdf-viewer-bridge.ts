import {
  type AppearanceMode,
  applyTheme,
  type Platform,
} from "@/theme"
import {
  GlobalWorkerOptions,
  getDocument,
  type PDFDocumentProxy,
} from "pdfjs-dist"
import pdfWorkerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url"
import {
  EventBus,
  PDFLinkService,
  PDFSinglePageViewer,
} from "pdfjs-dist/web/pdf_viewer.mjs"

GlobalWorkerOptions.workerSrc = pdfWorkerUrl

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

interface PdfTextSpanRange {
  element: HTMLSpanElement
  start: number
  end: number
}

interface PdfPageState {
  pageIndex: number
  textLayerElement: HTMLElement
  textContent: string
  textSpanRanges: PdfTextSpanRange[]
}

interface YabaPdfBridge {
  isReady: () => boolean
  setPdfUrl: (pdfUrl: string) => boolean
  getSelectionSnapshot: () => PdfSelectionSnapshot | null
  getCanCreateHighlight: () => boolean
  setHighlights: (highlightsJson: string) => void
  scrollToHighlight: (highlightId: string) => void
  getCurrentPageNumber: () => number
  getPageCount: () => number
  nextPage: () => boolean
  prevPage: () => boolean
  setPlatform: (platform: Platform) => void
  setAppearance: (appearance: AppearanceMode) => void
  onHighlightTap?: (id: string) => void
}

const colorRoleToClass: Record<string, string> = {
  NONE: "yaba-highlight-yellow",
  BLUE: "yaba-highlight-blue",
  BROWN: "yaba-highlight-brown",
  CYAN: "yaba-highlight-cyan",
  GRAY: "yaba-highlight-gray",
  GREEN: "yaba-highlight-green",
  INDIGO: "yaba-highlight-indigo",
  MINT: "yaba-highlight-mint",
  ORANGE: "yaba-highlight-orange",
  PINK: "yaba-highlight-pink",
  PURPLE: "yaba-highlight-purple",
  RED: "yaba-highlight-red",
  TEAL: "yaba-highlight-teal",
  YELLOW: "yaba-highlight-yellow",
}

const SCALE_FIT = "page-width"
const CONTEXT_WINDOW = 30

let isReady = false
let currentPlatform: Platform = "compose"
let currentAppearance: AppearanceMode = "auto"
let currentPdfUrl: string | null = null
let currentHighlights: PdfHighlightForRendering[] = []
let currentPdfDocument: PDFDocumentProxy | null = null
let pdfViewer: PDFSinglePageViewer | null = null
let pdfEventBus: EventBus | null = null
let pageStates: PdfPageState[] = []

function getRootElement(): HTMLElement {
  const root = document.getElementById("pdf-root")
  if (!root) throw new Error("Missing #pdf-root")
  root.classList.add("yaba-pdf-root")
  return root
}

function applyFitWidth(): void {
  const viewer = pdfViewer
  if (!viewer) return
  viewer.currentScaleValue = SCALE_FIT
}

function buildViewerShell(): void {
  const root = getRootElement()
  root.innerHTML = ""

  const container = document.createElement("div")
  container.className = "yaba-pdf-container"

  const viewerElement = document.createElement("div")
  viewerElement.className = "pdfViewer"
  container.appendChild(viewerElement)
  root.appendChild(container)

  pdfEventBus = new EventBus()
  const linkService = new PDFLinkService({
    eventBus: pdfEventBus,
  })
  pdfViewer = new PDFSinglePageViewer({
    container: container as HTMLDivElement,
    viewer: viewerElement as HTMLDivElement,
    eventBus: pdfEventBus,
    linkService,
    removePageBorders: false,
    textLayerMode: 1,
    annotationMode: 0,
    enableAutoLinking: false,
  })
  linkService.setViewer(pdfViewer)

  pdfEventBus.on("pagesinit", () => {
    applyFitWidth()
  })

  pdfEventBus.on("textlayerrendered", () => {
    rebuildPageStatesFromDom()
    attachHighlightTapListeners()
    renderHighlights()
  })

  const resizeObserver = new ResizeObserver(() => {
    applyFitWidth()
  })
  resizeObserver.observe(container)

  attachPinchZoom(container)
}

function attachPinchZoom(container: HTMLElement): void {
  let initialPinchDistance = 0
  let lastPinchScale = 1

  const reset = (): void => {
    initialPinchDistance = 0
    lastPinchScale = 1
  }

  container.addEventListener(
    "touchstart",
    (e: TouchEvent) => {
      if (e.touches.length === 2) {
        initialPinchDistance = Math.hypot(
          e.touches[1].pageX - e.touches[0].pageX,
          e.touches[1].pageY - e.touches[0].pageY
        )
      } else {
        reset()
      }
    },
    { passive: true }
  )

  container.addEventListener(
    "touchmove",
    (e: TouchEvent) => {
      if (initialPinchDistance <= 0 || e.touches.length < 2) return
      e.preventDefault()
      const pinchDistance = Math.hypot(
        e.touches[1].pageX - e.touches[0].pageX,
        e.touches[1].pageY - e.touches[0].pageY
      )
      lastPinchScale = pinchDistance / initialPinchDistance
    },
    { passive: false }
  )

  container.addEventListener(
    "touchend",
    () => {
      if (initialPinchDistance <= 0) return
      const viewer = pdfViewer
      if (viewer && lastPinchScale !== 1) {
        const newScale = Math.max(0.5, Math.min(4, viewer.currentScale * lastPinchScale))
        viewer.currentScale = newScale
      }
      reset()
    },
    { passive: true }
  )
}

function parsePageIndex(sectionKey: string): number | null {
  if (!sectionKey.startsWith("page-")) return null
  const parsed = Number.parseInt(sectionKey.replace("page-", ""), 10)
  return Number.isFinite(parsed) ? parsed : null
}

function comparePosition(
  leftSectionKey: string,
  leftOffset: number,
  rightSectionKey: string,
  rightOffset: number
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

function rangesOverlap(
  a: PdfSelectionSnapshot,
  b: PdfHighlightForRendering
): boolean {
  const aStartBeforeBEnd =
    comparePosition(
      a.startSectionKey,
      a.startOffsetInSection,
      b.endSectionKey,
      b.endOffsetInSection
    ) < 0
  const bStartBeforeAEnd =
    comparePosition(
      b.startSectionKey,
      b.startOffsetInSection,
      a.endSectionKey,
      a.endOffsetInSection
    ) < 0
  return aStartBeforeBEnd && bStartBeforeAEnd
}

function findPageStateByLayerNode(node: Node | null): PdfPageState | null {
  if (!node) return null
  const element =
    node instanceof Element
      ? node
      : node.parentElement
  const textLayer = element?.closest(".textLayer")
  if (!textLayer) return null
  return pageStates.find((state) => state.textLayerElement === textLayer) ?? null
}

function computeOffsetInLayer(
  textLayer: HTMLElement,
  node: Node,
  offset: number
): number {
  const range = document.createRange()
  range.selectNodeContents(textLayer)
  range.setEnd(node, offset)
  return range.toString().length
}

const HIGHLIGHT_OVERLAY_CLASS = "yaba-highlight-overlay"

function clearRenderedHighlights(): void {
  const root = getRootElement()
  root.querySelectorAll(`.${HIGHLIGHT_OVERLAY_CLASS}`).forEach((el) => el.remove())
}

function getTextLayerElements(): HTMLElement[] {
  const root = getRootElement()
  return Array.from(root.querySelectorAll(".textLayer")) as HTMLElement[]
}

function rebuildPageStatesFromDom(): void {
  pageStates = getTextLayerElements().map((textLayerElement) => {
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

function getOrCreateHighlightOverlayContainer(pageElement: HTMLElement): HTMLElement {
  let container = pageElement.querySelector(".yaba-highlight-overlays") as HTMLElement | null
  if (!container) {
    container = document.createElement("div")
    container.className = "yaba-highlight-overlays"
    container.style.cssText =
      "position:absolute;left:0;top:0;right:0;bottom:0;pointer-events:none;"
    const textLayer = pageElement.querySelector(".textLayer")
    if (textLayer) {
      textLayer.insertAdjacentElement("afterend", container)
    } else {
      pageElement.appendChild(container)
    }
  }
  return container
}

function createRangeByOffsets(
  textLayer: HTMLElement,
  startOffset: number,
  endOffset: number
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

function attachHighlightTapListeners(): void {
  const root = getRootElement()
  if (root.dataset.yabaHighlightTapAttached === "true") return
  root.dataset.yabaHighlightTapAttached = "true"
  root.addEventListener("click", (event) => {
    const target = event.target as HTMLElement
    const highlightElement = target.closest("[data-highlight-id]") as HTMLElement | null
    const highlightId = highlightElement?.dataset.highlightId
    if (!highlightId) return
    event.preventDefault()
    const win = window as Window & { YabaPdfBridge?: YabaPdfBridge }
    win.YabaPdfBridge?.onHighlightTap?.(highlightId)
  })
}

function renderHighlights(): void {
  clearRenderedHighlights()
  if (currentHighlights.length <= 0) return

  currentHighlights.forEach((highlight) => {
    const startPage = parsePageIndex(highlight.startSectionKey)
    const endPage = parsePageIndex(highlight.endSectionKey)
    if (startPage === null || endPage === null) return

    const pageRangeStart = Math.min(startPage, endPage)
    const pageRangeEnd = Math.max(startPage, endPage)
    const colorClass = colorRoleToClass[highlight.colorRole] ?? "yaba-highlight-yellow"

    for (let pageIndex = pageRangeStart; pageIndex <= pageRangeEnd; pageIndex += 1) {
      const pageState = pageStates.find((page) => page.pageIndex === pageIndex)
      if (!pageState) continue

      const pageTextLength = pageState.textContent.length
      const fromOffset =
        pageIndex === startPage ? highlight.startOffsetInSection : 0
      const toOffset =
        pageIndex === endPage ? highlight.endOffsetInSection : pageTextLength
      if (fromOffset >= toOffset) continue

      const range = createRangeByOffsets(
        pageState.textLayerElement,
        fromOffset,
        toOffset
      )
      if (!range) continue

      const rects = range.getClientRects()
      if (rects.length === 0) continue

      const pageElement = pageState.textLayerElement.closest(".page") as HTMLElement | null
      if (!pageElement) continue

      const overlayContainer = getOrCreateHighlightOverlayContainer(pageElement)
      const pageRect = pageElement.getBoundingClientRect()
      for (let i = 0; i < rects.length; i += 1) {
        const rect = rects[i]
        if (rect.width <= 0 || rect.height <= 0) continue
        const overlay = document.createElement("div")
        overlay.className = `yaba-highlight-overlay yaba-highlight ${colorClass}`
        overlay.dataset.highlightId = highlight.id
        overlay.style.cssText = `
          position:absolute;
          left:${rect.left - pageRect.left}px;
          top:${rect.top - pageRect.top}px;
          width:${rect.width}px;
          height:${rect.height}px;
          pointer-events:auto;
        `
        overlayContainer.appendChild(overlay)
      }
    }
  })
}

function buildSpanRanges(textLayerElement: HTMLElement): { textContent: string; textSpanRanges: PdfTextSpanRange[] } {
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

async function renderPdfPages(pdfUrl: string): Promise<void> {
  if (!pdfViewer) buildViewerShell()
  const viewer = pdfViewer
  if (!viewer) throw new Error("PDF viewer not initialized")

  if (currentPdfDocument) {
    await currentPdfDocument.destroy().catch(() => undefined)
    currentPdfDocument = null
  }

  const loadingTask = getDocument(pdfUrl)
  const pdfDocument = await loadingTask.promise
  currentPdfDocument = pdfDocument
  viewer.setDocument(pdfDocument)
  await viewer.pagesPromise
  rebuildPageStatesFromDom()
  attachHighlightTapListeners()
  renderHighlights()
}

function getSelectionSnapshot(): PdfSelectionSnapshot | null {
  const selection = window.getSelection()
  if (!selection || selection.rangeCount <= 0 || selection.isCollapsed) return null
  const range = selection.getRangeAt(0)
  const startPageState = findPageStateByLayerNode(range.startContainer)
  const endPageState = findPageStateByLayerNode(range.endContainer)
  if (!startPageState || !endPageState) return null

  const selectedText = selection.toString().trim()
  if (!selectedText) return null

  const startOffsetInSection = computeOffsetInLayer(
    startPageState.textLayerElement,
    range.startContainer,
    range.startOffset
  )
  const endOffsetInSection = computeOffsetInLayer(
    endPageState.textLayerElement,
    range.endContainer,
    range.endOffset
  )

  const prefixText = startPageState.textContent
    .slice(Math.max(0, startOffsetInSection - CONTEXT_WINDOW), startOffsetInSection)
    .trim()
  const suffixText = endPageState.textContent
    .slice(endOffsetInSection, Math.min(endPageState.textContent.length, endOffsetInSection + CONTEXT_WINDOW))
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

function getCanCreateHighlight(): boolean {
  const snapshot = getSelectionSnapshot()
  if (!snapshot) return false
  return !currentHighlights.some((highlight) => rangesOverlap(snapshot, highlight))
}

function setHighlights(highlightsJson: string): void {
  try {
    const parsed = highlightsJson.trim().length > 0
      ? (JSON.parse(highlightsJson) as PdfHighlightForRendering[])
      : []
    currentHighlights = parsed
  } catch {
    currentHighlights = []
  }
  renderHighlights()
}

function scrollToHighlight(highlightId: string): void {
  const highlightElement = document.querySelector(`[data-highlight-id="${highlightId}"]`) as HTMLElement | null
  highlightElement?.scrollIntoView({ behavior: "smooth", block: "center" })
}

export function initPdfViewerBridge(
  platform: Platform,
  appearance: AppearanceMode
): void {
  currentPlatform = platform
  currentAppearance = appearance
  applyTheme(currentPlatform, currentAppearance, null)
  buildViewerShell()

  const win = window as Window & { YabaPdfBridge?: YabaPdfBridge }
  win.YabaPdfBridge = {
    isReady: () => isReady,
    setPdfUrl(pdfUrl: string): boolean {
      if (!pdfUrl || currentPdfUrl === pdfUrl && pageStates.length > 0) return true
      currentPdfUrl = pdfUrl
      void renderPdfPages(pdfUrl).catch((error) => {
        console.error("Failed to render PDF", error)
      })
      return true
    },
    getSelectionSnapshot,
    getCanCreateHighlight,
    setHighlights,
    scrollToHighlight,
    getCurrentPageNumber: () => (pdfViewer?.currentPageNumber ?? 1),
    getPageCount: () => (pdfViewer?.pagesCount ?? 0),
    nextPage(): boolean {
      const viewer = pdfViewer
      if (!viewer || viewer.pagesCount <= 0) return false
      const next = Math.min(viewer.currentPageNumber + 1, viewer.pagesCount)
      if (next === viewer.currentPageNumber) return false
      viewer.currentPageNumber = next
      return true
    },
    prevPage(): boolean {
      const viewer = pdfViewer
      if (!viewer || viewer.pagesCount <= 0) return false
      const prev = Math.max(viewer.currentPageNumber - 1, 1)
      if (prev === viewer.currentPageNumber) return false
      viewer.currentPageNumber = prev
      return true
    },
    setPlatform(nextPlatform: Platform): void {
      currentPlatform = nextPlatform
      applyTheme(currentPlatform, currentAppearance, null)
    },
    setAppearance(nextAppearance: AppearanceMode): void {
      currentAppearance = nextAppearance
      applyTheme(currentPlatform, currentAppearance, null)
    },
  }
  isReady = true
}
