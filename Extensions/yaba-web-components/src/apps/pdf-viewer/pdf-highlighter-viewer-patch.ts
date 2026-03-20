import { PdfHighlighter } from "react-pdf-highlighter"

/**
 * Exposes the internal PDF.js viewer (`PDFSinglePageViewer` after Vite transform in
 * `vite.config.ts`) for YABA bridge (page nav, metrics).
 *
 * Must be imported before the viewer mounts.
 */
declare global {
  interface Window {
    __YABA_PDF_VIEWER__?: {
      currentPageNumber: number
      pagesCount: number
      currentScale: number
      currentScaleValue: string
      getPageView: (index: number) => unknown
      container: HTMLElement
      eventBus?: { on: (n: string, fn: () => void) => void; off: (n: string, fn: () => void) => void }
    }
  }
}

type InstanceWithViewer = { viewer?: Window["__YABA_PDF_VIEWER__"] }
type ViewerWithContainer = NonNullable<Window["__YABA_PDF_VIEWER__"]> & {
  viewer?: HTMLElement
}

const proto = PdfHighlighter.prototype as unknown as {
  init: () => Promise<void>
}

const originalInit = proto.init

proto.init = async function patchedInit(this: InstanceWithViewer) {
  await originalInit.call(this)
  if (this.viewer) {
    const viewer = this.viewer as ViewerWithContainer
    window.__YABA_PDF_VIEWER__ = viewer
    viewer.container.classList.add("yaba-pdf-scroll-container")
    viewer.viewer?.classList.add("yaba-pdf-viewer-content")
  }
}
