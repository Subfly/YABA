import { applyTheme, parseUrlParams } from "@/theme"
import "../shared/global.css"
import "./epub-viewer.css"
import { initEpubViewerBridge } from "./epub-viewer-bridge"

const params = parseUrlParams()
applyTheme(params.platform, params.appearance, params.cursorColor)
initEpubViewerBridge(params.platform, params.appearance)
