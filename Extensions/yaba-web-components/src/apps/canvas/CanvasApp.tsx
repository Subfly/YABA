import { useEffect, useRef } from "react"
import { Excalidraw, FONT_FAMILY } from "@excalidraw/excalidraw"
import "@excalidraw/excalidraw/index.css"
import "./canvas-host.css"
import { initCanvasBridge, onCanvasChanged } from "@/bridge/canvas-bridge"
import { installCanvasContextMenuSeparatorCleanup } from "./contextMenuHiddenTestIds"

function CanvasApp() {
  const rootRef = useRef<HTMLDivElement>(null)
  const uiOptions = {
    canvasActions: {
      loadScene: false,
      saveAsImage: false,
      saveToActiveFile: false,
      export: false,
      clearCanvas: false,
      toggleTheme: false,
      changeViewBackgroundColor: false,
    },
    tools: {
      image: false,
    },
  } as any

  useEffect(() => {
    const el = rootRef.current
    if (!el) return
    return installCanvasContextMenuSeparatorCleanup(el)
  }, [])

  return (
    <div
      ref={rootRef}
      data-yaba-canvas
      style={{ width: "100%", height: "100%", minHeight: "100%", backgroundColor: "transparent" }}
    >
      <Excalidraw
        zenModeEnabled
        handleKeyboardGlobally
        renderTopRightUI={() => null}
        initialData={{
          elements: [],
          appState: {
            currentItemFontFamily: FONT_FAMILY.Excalifont,
          },
        }}
        excalidrawAPI={(api) => {
          if (!api) return
          initCanvasBridge(api)
        }}
        onChange={() => {
          onCanvasChanged()
        }}
        UIOptions={uiOptions}
      />
    </div>
  )
}

export { CanvasApp }
