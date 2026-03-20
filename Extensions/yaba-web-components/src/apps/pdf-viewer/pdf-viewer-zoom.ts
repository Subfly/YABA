/**
 * Pinch zoom on PDF.js scroll container.
 * - Min scale is current "page-width" fit.
 * - Max scale is 3x page-width fit.
 */

export const SCALE_PAGE_WIDTH = "page-width"
export const MAX_ZOOM_FACTOR = 3

const RESIZE_DEBOUNCE_MS = 180
const PAGE_CHANGE_DEBOUNCE_MS = 60
const BIND_POLL_MS = 64

type ViewerLike = {
  currentScale: number
  currentScaleValue: string
  container: HTMLElement
  pagesCount?: number
  eventBus?: {
    on: (name: string, fn: (...args: unknown[]) => void) => void
    off: (name: string, fn: (...args: unknown[]) => void) => void
  }
}

type PinchState = {
  active: boolean
  initialDistance: number
  startScale: number
  latestDistance: number
}

function getViewer(): ViewerLike | null {
  const viewer = window.__YABA_PDF_VIEWER__ as ViewerLike | undefined
  return viewer ?? null
}

function getScrollContainer(viewer: ViewerLike): HTMLElement | null {
  if (viewer.container) return viewer.container
  const pdfViewerInner = document.querySelector("#pdf-root .pdfViewer") as HTMLElement | null
  return pdfViewerInner?.parentElement ?? null
}

function distanceBetweenTouches(e: TouchEvent): number {
  return Math.hypot(
    e.touches[1].clientX - e.touches[0].clientX,
    e.touches[1].clientY - e.touches[0].clientY,
  )
}

function clampScale(baseFitScale: number, requested: number): number {
  const minScale = baseFitScale
  const maxScale = baseFitScale * MAX_ZOOM_FACTOR
  return Math.min(maxScale, Math.max(minScale, requested))
}

/**
 * Re-evaluates page-width baseline and keeps current relative zoom ratio.
 */
function refreshBaseline(
  viewer: ViewerLike,
  baseFitScaleRef: { current: number },
  onDone?: () => void,
): void {
  const oldBase = baseFitScaleRef.current
  const ratioBefore = oldBase > 0 && viewer.currentScale > 0
    ? viewer.currentScale / oldBase
    : 1

  viewer.currentScaleValue = SCALE_PAGE_WIDTH

  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const newBase = viewer.currentScale
      if (!(newBase > 0) || !Number.isFinite(newBase)) return
      baseFitScaleRef.current = newBase
      viewer.currentScale = clampScale(newBase, ratioBefore * newBase)
      onDone?.()
    })
  })
}

export interface AttachPdfPinchZoomOptions {
  /** Called when scale changes so highlight layers can refresh */
  onScaleChanged?: () => void
}

export function attachPdfPinchZoom(options: AttachPdfPinchZoomOptions = {}): () => void {
  const { onScaleChanged } = options
  const baseFitScaleRef = { current: 0 }
  const pinch: PinchState = {
    active: false,
    initialDistance: 0,
    startScale: 1,
    latestDistance: 0,
  }

  let scrollContainer: HTMLElement | null = null
  let resizeObserver: ResizeObserver | null = null
  let resizeDebounceTimer: number | null = null
  let pageChangeTimer: number | null = null
  let bindPollTimer: number | null = null
  let pinchFrame: number | null = null

  const endPinch = (): void => {
    pinch.active = false
    pinch.initialDistance = 0
    pinch.latestDistance = 0
    pinch.startScale = 1
    scrollContainer?.classList.remove("yaba-pdf-pinching")
  }

  const applyPinchFrame = (): void => {
    pinchFrame = null
    if (!pinch.active || pinch.initialDistance <= 0 || pinch.latestDistance <= 0) return
    const viewer = getViewer()
    if (!viewer || baseFitScaleRef.current <= 0) return
    const scaleFactor = pinch.latestDistance / pinch.initialDistance
    const targetScale = clampScale(baseFitScaleRef.current, pinch.startScale * scaleFactor)
    if (Math.abs(viewer.currentScale - targetScale) > 0.0001) {
      viewer.currentScale = targetScale
    }
  }

  const schedulePinchFrame = (): void => {
    if (pinchFrame !== null) return
    pinchFrame = requestAnimationFrame(applyPinchFrame)
  }

  const onTouchStart = (e: TouchEvent): void => {
    if (e.touches.length !== 2) {
      if (pinch.active) {
        schedulePinchFrame()
      }
      return
    }
    const viewer = getViewer()
    if (!viewer || baseFitScaleRef.current <= 0) return
    const initialDistance = distanceBetweenTouches(e)
    if (!(initialDistance > 0) || !Number.isFinite(initialDistance)) return

    pinch.active = true
    pinch.initialDistance = initialDistance
    pinch.latestDistance = initialDistance
    pinch.startScale = viewer.currentScale
    scrollContainer?.classList.add("yaba-pdf-pinching")
  }

  const onTouchMove = (e: TouchEvent): void => {
    if (!pinch.active || e.touches.length !== 2) return
    e.preventDefault()
    pinch.latestDistance = distanceBetweenTouches(e)
    schedulePinchFrame()
  }

  const onTouchEndOrCancel = (e: TouchEvent): void => {
    if (!pinch.active || e.touches.length >= 2) return
    if (pinchFrame !== null) {
      cancelAnimationFrame(pinchFrame)
      pinchFrame = null
    }
    applyPinchFrame()
    endPinch()
    onScaleChanged?.()
  }

  const onPagesInit = (): void => {
    const viewer = getViewer()
    if (!viewer) return
    refreshBaseline(viewer, baseFitScaleRef, onScaleChanged)
  }

  const onPageChanging = (): void => {
    if (pageChangeTimer !== null) {
      window.clearTimeout(pageChangeTimer)
    }
    pageChangeTimer = window.setTimeout(() => {
      pageChangeTimer = null
      const viewer = getViewer()
      if (!viewer) return
      refreshBaseline(viewer, baseFitScaleRef, onScaleChanged)
    }, PAGE_CHANGE_DEBOUNCE_MS)
  }

  const onResizeDebounced = (): void => {
    if (resizeDebounceTimer !== null) {
      window.clearTimeout(resizeDebounceTimer)
    }
    resizeDebounceTimer = window.setTimeout(() => {
      resizeDebounceTimer = null
      const viewer = getViewer()
      if (!viewer) return
      refreshBaseline(viewer, baseFitScaleRef, onScaleChanged)
    }, RESIZE_DEBOUNCE_MS)
  }

  const unbindScrollContainer = (container: HTMLElement | null): void => {
    if (!container) return
    container.removeEventListener("touchstart", onTouchStart)
    container.removeEventListener("touchmove", onTouchMove)
    container.removeEventListener("touchend", onTouchEndOrCancel)
    container.removeEventListener("touchcancel", onTouchEndOrCancel)
    container.classList.remove("yaba-pdf-scroll-container")
    container.classList.remove("yaba-pdf-pinching")
  }

  const bindIfReady = (): void => {
    const viewer = getViewer()
    if (!viewer) return
    const nextContainer = getScrollContainer(viewer)
    if (!nextContainer) return

    if (scrollContainer !== nextContainer) {
      unbindScrollContainer(scrollContainer)
      scrollContainer = nextContainer
      scrollContainer.classList.add("yaba-pdf-scroll-container")
      scrollContainer.addEventListener("touchstart", onTouchStart, { passive: true })
      scrollContainer.addEventListener("touchmove", onTouchMove, { passive: false })
      scrollContainer.addEventListener("touchend", onTouchEndOrCancel, { passive: true })
      scrollContainer.addEventListener("touchcancel", onTouchEndOrCancel, { passive: true })

      if (resizeObserver) {
        resizeObserver.disconnect()
      }
      resizeObserver = new ResizeObserver(onResizeDebounced)
      resizeObserver.observe(scrollContainer)
    }

    if (viewer.eventBus) {
      try {
        viewer.eventBus.off("pagesinit", onPagesInit)
        viewer.eventBus.off("pagechanging", onPageChanging)
      } catch {
        /* ignore */
      }
      viewer.eventBus.on("pagesinit", onPagesInit)
      viewer.eventBus.on("pagechanging", onPageChanging)
    }

    if (baseFitScaleRef.current <= 0) {
      refreshBaseline(viewer, baseFitScaleRef, onScaleChanged)
    }
  }

  bindPollTimer = window.setInterval(() => {
    bindIfReady()
    const viewer = getViewer()
    if (scrollContainer && viewer && (viewer.pagesCount ?? 0) > 0) {
      window.clearInterval(bindPollTimer!)
      bindPollTimer = null
    }
  }, BIND_POLL_MS)

  return () => {
    if (pinchFrame !== null) {
      cancelAnimationFrame(pinchFrame)
      pinchFrame = null
    }
    if (pageChangeTimer !== null) {
      window.clearTimeout(pageChangeTimer)
      pageChangeTimer = null
    }
    if (resizeDebounceTimer !== null) {
      window.clearTimeout(resizeDebounceTimer)
      resizeDebounceTimer = null
    }
    if (bindPollTimer !== null) {
      window.clearInterval(bindPollTimer)
      bindPollTimer = null
    }
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }

    const viewer = getViewer()
    if (viewer?.eventBus) {
      try {
        viewer.eventBus.off("pagesinit", onPagesInit)
        viewer.eventBus.off("pagechanging", onPageChanging)
      } catch {
        /* ignore */
      }
    }

    endPinch()
    unbindScrollContainer(scrollContainer)
    scrollContainer = null
  }
}
