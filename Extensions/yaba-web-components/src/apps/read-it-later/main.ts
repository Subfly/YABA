import { initReadItLaterBridge } from "@/bridge/read-it-later-bridge"
import { parseUrlParams, applyTheme } from "@/theme"
import "../shared/global.css"
import "./read-it-later.css"

try {
  const params = parseUrlParams()
  applyTheme(params.platform, params.appearance, params.cursorColor)
} catch (e) {
  // eslint-disable-next-line no-console
  console.error("[YABA read-it-later] theme", e)
}

const root = document.getElementById("yaba-read-it-later-root")
if (root) {
  initReadItLaterBridge(() =>
    document.getElementById("yaba-read-it-later-content") as HTMLElement | null,
  )
}
