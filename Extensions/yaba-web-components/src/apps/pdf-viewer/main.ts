import { applyTheme, parseUrlParams } from "@/theme"
import "pdfjs-dist/legacy/web/pdf_viewer.css"
import "../shared/global.css"
import "./pdf-viewer.css"
import { initPdfViewerBridge } from "./pdf-viewer-bridge"

const params = parseUrlParams()
applyTheme(params.platform, params.appearance, params.cursorColor)
initPdfViewerBridge(params.platform, params.appearance)
