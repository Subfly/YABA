import type { IHighlight } from "react-pdf-highlighter"
import type { PdfHighlightForRendering } from "./pdf-text-utils"

/**
 * Shared state between the React PDF island and `window.YabaPdfBridge` (non-React).
 */
export const pdfBridgeRuntime = {
  lastLibHighlights: [] as IHighlight[],
  scrollToLib: null as null | ((highlight: IHighlight) => void),
  yabaHighlights: [] as PdfHighlightForRendering[],
  setPdfUrl: null as null | ((url: string) => void),
  setYabaHighlights: null as null | ((highlights: PdfHighlightForRendering[]) => void),
  /** Set before React registers handlers (first WebView injection). */
  pendingPdfUrl: null as string | null,
  pendingYabaHighlights: null as PdfHighlightForRendering[] | null,
}
