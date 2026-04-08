import { Excalidraw } from "@excalidraw/excalidraw"
import "@excalidraw/excalidraw/index.css"
import "./canvas-host.css"
import { initCanvasBridge, onCanvasChanged } from "@/bridge/canvas-bridge"

function CanvasApp() {
  return (
    <div
      data-yaba-canvas
      style={{ width: "100%", height: "100%", minHeight: "100%", backgroundColor: "transparent" }}
    >
      <Excalidraw
        zenModeEnabled
        handleKeyboardGlobally
        renderTopRightUI={() => null}
        excalidrawAPI={(api) => {
          if (!api) return
          initCanvasBridge(api)
        }}
        onChange={() => {
          onCanvasChanged()
        }}
        UIOptions={{
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
        }}
      />
    </div>
  )
}

export { CanvasApp }
