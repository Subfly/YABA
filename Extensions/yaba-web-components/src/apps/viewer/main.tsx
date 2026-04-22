import React from "react"
import ReactDOM from "react-dom/client"
import { ViewerApp } from "./ViewerApp"
import { parseUrlParams, applyTheme } from "@/theme"
import "../shared/global.css"

const root = document.getElementById("root")
if (root) {
  try {
    const params = parseUrlParams()
    applyTheme(params.platform, params.appearance, params.cursorColor)
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error("[YABA viewer] theme", e)
  }
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <ViewerApp />
    </React.StrictMode>
  )
}
