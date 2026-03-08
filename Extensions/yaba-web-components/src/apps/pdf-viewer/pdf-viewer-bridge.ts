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
  PDFViewer,
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

const PDF_SCALE = 1.35
const CONTEXT_WINDOW = 30

let isReady = false
let currentPlatform: Platform = "compose"
let currentAppearance: AppearanceMode = "auto"
let currentPdfUrl: string | null = null
let currentHighlights: PdfHighlightForRendering[] = []
let currentPdfDocument: PDFDocumentProxy | null = null
let pdfViewer: PDFViewer | null = null
let pdfEventBus: EventBus | null = null
let pageStates: PdfPageState[] = []

function getRootElement(): HTMLElement {
  const root = document.getElementById("pdf-root")
  if (!root) throw new Error("Missing #pdf-root")
  root.classList.add("yaba-pdf-root")
  return root
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
  pdfViewer = new PDFViewer({
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

  pdfEventBus.on("textlayerrendered", () => {
    rebuildPageStatesFromDom()
    attachHighlightTapListeners()
    renderHighlights()
  })
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

function clearRenderedHighlights(): void {
  pageStates.forEach((state) => {
    state.textSpanRanges.forEach(({ element }) => {
      element.classList.remove(
        "yaba-highlight",
        "yaba-highlight-yellow",
        "yaba-highlight-blue",
        "yaba-highlight-brown",
        "yaba-highlight-cyan",
        "yaba-highlight-gray",
        "yaba-highlight-green",
        "yaba-highlight-indigo",
        "yaba-highlight-mint",
        "yaba-highlight-orange",
        "yaba-highlight-pink",
        "yaba-highlight-purple",
        "yaba-highlight-red",
        "yaba-highlight-teal"
      )
      element.removeAttribute("data-highlight-id")
    })
  })
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

function attachHighlightTapListeners(): void {
  pageStates.forEach((state) => {
    if (state.textLayerElement.dataset.yabaTapListenerAttached === "true") return
    state.textLayerElement.dataset.yabaTapListenerAttached = "true"
    state.textLayerElement.addEventListener("click", (event) => {
      const target = event.target as HTMLElement
      const highlightElement = target.closest("[data-highlight-id]") as HTMLElement | null
      const highlightId = highlightElement?.dataset.highlightId
      if (!highlightId) return
      event.preventDefault()
      const win = window as Window & { YabaPdfBridge?: YabaPdfBridge }
      win.YabaPdfBridge?.onHighlightTap?.(highlightId)
    })
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
    for (let pageIndex = pageRangeStart; pageIndex <= pageRangeEnd; pageIndex += 1) {
      const pageState = pageStates.find((page) => page.pageIndex === pageIndex)
      if (!pageState) continue

      const pageTextLength = pageState.textContent.length
      const fromOffset =
        pageIndex === startPage ? highlight.startOffsetInSection : 0
      const toOffset =
        pageIndex === endPage ? highlight.endOffsetInSection : pageTextLength
      if (fromOffset >= toOffset) continue

      const colorClass = colorRoleToClass[highlight.colorRole] ?? "yaba-highlight-yellow"
      pageState.textSpanRanges.forEach((spanRange) => {
        const overlaps = spanRange.start < toOffset && spanRange.end > fromOffset
        if (!overlaps) return
        spanRange.element.classList.add("yaba-highlight", colorClass)
        if (!spanRange.element.dataset.highlightId) {
          spanRange.element.dataset.highlightId = highlight.id
        }
      })
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
  viewer.currentScale = PDF_SCALE
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
