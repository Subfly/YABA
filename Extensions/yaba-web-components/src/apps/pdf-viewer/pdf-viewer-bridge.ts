import { createRoot } from "react-dom/client"
import { createElement } from "react"
import {
  type AppearanceMode,
  applyTheme,
  type Platform,
} from "@/theme"
import YabaPdfViewerApp from "./YabaPdfViewerApp"
import { pdfBridgeRuntime } from "./pdf-bridge-runtime"
import {
  getCanCreateAnnotationFromRoot,
  getSelectionSnapshotFromRoot,
  type PdfHighlightForRendering,
  type PdfSelectionSnapshot,
} from "./pdf-text-utils"
import { postToYabaNativeHost } from "@/bridge/yaba-native-host"

export type { PdfHighlightForRendering, PdfSelectionSnapshot } from "./pdf-text-utils"

interface YabaPdfBridge {
  isReady: () => boolean
  setPdfUrl: (pdfUrl: string) => boolean
  getSelectionSnapshot: () => PdfSelectionSnapshot | null
  getCanCreateAnnotation: () => boolean
  setAnnotations: (annotationsJson: string) => void
  scrollToAnnotation: (annotationId: string) => void
  getCurrentPageNumber: () => number
  getPageCount: () => number
  nextPage: () => boolean
  prevPage: () => boolean
  setPlatform: (platform: Platform) => void
  setAppearance: (appearance: AppearanceMode) => void
  onAnnotationTap?: (id: string) => void
  navigateToTocItem: (id: string, extrasJson?: string | null) => void
}

let isReady = false
let currentPlatform: Platform = "android"
let currentAppearance: AppearanceMode = "auto"
let currentPdfUrl: string | null = null
let currentHighlights: PdfHighlightForRendering[] = []
function getRootElement(): HTMLElement {
  const root = document.getElementById("pdf-root")
  if (!root) throw new Error("Missing #pdf-root")
  root.classList.add("yaba-pdf-root")
  return root
}

function pushHighlightsToReact(parsed: PdfHighlightForRendering[]): void {
  if (pdfBridgeRuntime.setYabaHighlights) {
    pdfBridgeRuntime.setYabaHighlights(parsed)
  } else {
    pdfBridgeRuntime.pendingYabaHighlights = parsed
  }
}

function pushPdfUrlToReact(url: string): void {
  if (pdfBridgeRuntime.setPdfUrl) {
    pdfBridgeRuntime.setPdfUrl(url)
  } else {
    pdfBridgeRuntime.pendingPdfUrl = url
  }
}

export function initPdfViewerBridge(
  platform: Platform,
  appearance: AppearanceMode,
): void {
  currentPlatform = platform
  currentAppearance = appearance
  applyTheme(currentPlatform, currentAppearance, null)

  const el = getRootElement()
  createRoot(el).render(createElement(YabaPdfViewerApp))

  const win = window as Window & { YabaPdfBridge?: YabaPdfBridge }
  win.YabaPdfBridge = {
    isReady: () => isReady,
    setPdfUrl(pdfUrl: string): boolean {
      if (!pdfUrl) return false
      if (currentPdfUrl === pdfUrl && isReady) return true
      currentPdfUrl = pdfUrl
      pushPdfUrlToReact(pdfUrl)
      return true
    },
    getSelectionSnapshot(): PdfSelectionSnapshot | null {
      const root = document.getElementById("pdf-root")
      if (!root) return null
      return getSelectionSnapshotFromRoot(root)
    },
    getCanCreateAnnotation(): boolean {
      const root = document.getElementById("pdf-root")
      if (!root) return false
      return getCanCreateAnnotationFromRoot(root, currentHighlights)
    },
    setAnnotations(annotationsJson: string): void {
      try {
        const parsed = annotationsJson.trim().length > 0
          ? (JSON.parse(annotationsJson) as PdfHighlightForRendering[])
          : []
        currentHighlights = parsed
      } catch {
        currentHighlights = []
      }
      pushHighlightsToReact(currentHighlights)
    },
    scrollToAnnotation(annotationId: string): void {
      const lib = pdfBridgeRuntime.lastLibHighlights.find((h) => h.id === annotationId)
      const scroll = pdfBridgeRuntime.scrollToLib
      if (lib && scroll) {
        scroll(lib)
        return
      }
      const safeId = annotationId.replace(/\\/g, "\\\\").replace(/"/g, '\\"')
      const el = document.querySelector(
        `[data-highlight-id="${safeId}"]`,
      ) as HTMLElement | null
      el?.scrollIntoView({ behavior: "smooth", block: "center" })
    },
    getCurrentPageNumber(): number {
      return window.__YABA_PDF_VIEWER__?.currentPageNumber ?? 1
    },
    getPageCount(): number {
      return window.__YABA_PDF_VIEWER__?.pagesCount ?? 0
    },
    nextPage(): boolean {
      const viewer = window.__YABA_PDF_VIEWER__
      if (!viewer || viewer.pagesCount <= 0) return false
      const next = Math.min(viewer.currentPageNumber + 1, viewer.pagesCount)
      if (next === viewer.currentPageNumber) return false
      viewer.currentPageNumber = next
      return true
    },
    prevPage(): boolean {
      const viewer = window.__YABA_PDF_VIEWER__
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
    navigateToTocItem(_id: string, extrasJson?: string | null): void {
      try {
        const raw = extrasJson?.trim()
        if (!raw) return
        const o = JSON.parse(raw) as { page?: number }
        const page = o.page
        if (typeof page !== "number" || !Number.isFinite(page)) return
        const viewer = window.__YABA_PDF_VIEWER__
        if (!viewer || viewer.pagesCount <= 0) return
        const p = Math.max(1, Math.min(Math.floor(page), viewer.pagesCount))
        viewer.currentPageNumber = p
      } catch {
        /* ignore */
      }
    },
  }
  isReady = true
  postToYabaNativeHost({ type: "bridgeReady", feature: "pdf" })
}
