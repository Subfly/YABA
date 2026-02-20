import React from "react"
import ReactDOM from "react-dom/client"
import { EditorApp } from "./EditorApp"
import { parseUrlParams, applyTheme } from "@/theme"
import "../shared/global.css"

const params = parseUrlParams()
applyTheme(params.platform, params.appearance, params.cursorColor)

const root = document.getElementById("root")
if (root) {
  ReactDOM.createRoot(root).render(
    <React.StrictMode>
      <EditorApp />
    </React.StrictMode>
  )
}
