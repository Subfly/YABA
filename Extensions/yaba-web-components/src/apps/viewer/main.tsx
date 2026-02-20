import React from "react"
import ReactDOM from "react-dom/client"
import { ViewerApp } from "./ViewerApp"
import { parseUrlParams, applyTheme } from "@/theme"
import "../shared/global.css"

const params = parseUrlParams()
applyTheme(params.platform, params.appearance, params.cursorColor)

const root = document.getElementById("root")
if (root) {
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <ViewerApp />
    </React.StrictMode>
  )
}
