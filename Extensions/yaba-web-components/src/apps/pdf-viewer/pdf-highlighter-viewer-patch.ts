import { PdfHighlighter } from "react-pdf-highlighter"

/**
 * Exposes the internal PDF.js `PDFViewer` instance for YABA bridge (page nav, metrics).
 * Must be imported before the viewer mounts.
 */
declare global {
  interface Window {
    __YABA_PDF_VIEWER__?: {
      currentPageNumber: number
      pagesCount: number
      getPageView: (index: number) => unknown
      container: HTMLElement
      eventBus?: { on: (n: string, fn: () => void) => void; off: (n: string, fn: () => void) => void }
    }
  }
}

type InstanceWithViewer = { viewer?: Window["__YABA_PDF_VIEWER__"] }

const proto = PdfHighlighter.prototype as unknown as {
  init: () => Promise<void>
}

const originalInit = proto.init

proto.init = async function patchedInit(this: InstanceWithViewer) {
  await originalInit.call(this)
  if (this.viewer) {
    window.__YABA_PDF_VIEWER__ = this.viewer
  }
}
