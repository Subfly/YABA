import { useCallback, useMemo, useRef } from "react"
import type { ExcalidrawImperativeAPI, ExcalidrawInitialDataState } from "@excalidraw/excalidraw/types"
import { Excalidraw, FONT_FAMILY } from "@excalidraw/excalidraw"
import "@excalidraw/excalidraw/index.css"
import "./canvas-host.css"
import { initCanvasBridge, onCanvasChanged } from "@/bridge/canvas-bridge"
import { parseMentionFromLink } from "@/bridge/canvas-inline"
import { postToYabaNativeHost } from "@/bridge/yaba-native-host"

function CanvasApp() {
  const rootRef = useRef<HTMLDivElement>(null)

  /** Must be referentially stable — new objects each render make Excalidraw re-sync and can loop with onChange. */
  const initialData = useMemo<ExcalidrawInitialDataState>(
    () => ({
      elements: [],
      appState: {
        currentItemFontFamily: FONT_FAMILY.Excalifont,
        gridModeEnabled: true,
        objectsSnapModeEnabled: true,
      },
    }),
    [],
  )

  const uiOptions = useMemo(
    () => ({
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
    }),
    [],
  ) as any

  const excalidrawAPI = useCallback((api: ExcalidrawImperativeAPI | null) => {
    if (!api) return
    initCanvasBridge(api)
  }, [])

  const handleCanvasChange = useCallback(() => {
    onCanvasChanged()
  }, [])

  const handleLinkOpen = useCallback((element: unknown, event: CustomEvent<{ nativeEvent: unknown }>) => {
    const el = element as { link?: unknown; text?: unknown; id?: unknown }
    const link = typeof el.link === "string" ? el.link : ""
    if (link === "") return
    event?.preventDefault?.()

    const text = typeof el.text === "string" ? el.text : ""
    const elementId = typeof el.id === "string" ? el.id : ""
    if (elementId === "") return

    const mention = parseMentionFromLink(link)
    if (mention) {
      postToYabaNativeHost({
        type: "canvasMentionTap",
        elementId,
        text: mention.text,
        bookmarkId: mention.bookmarkId,
        bookmarkKindCode: mention.bookmarkKindCode,
        bookmarkLabel: mention.bookmarkLabel,
      })
      return
    }

    postToYabaNativeHost({
      type: "canvasLinkTap",
      elementId,
      text,
      url: link,
    })
  }, [])

  const renderTopRightUI = useCallback(() => null, [])

  return (
    <div
      ref={rootRef}
      data-yaba-canvas
      style={{
        position: "relative",
        width: "100%",
        height: "100%",
        minHeight: "100%",
        backgroundColor: "transparent",
      }}
    >
      <Excalidraw
        zenModeEnabled
        handleKeyboardGlobally
        renderTopRightUI={renderTopRightUI}
        initialData={initialData}
        excalidrawAPI={excalidrawAPI}
        onChange={handleCanvasChange}
        onLinkOpen={handleLinkOpen}
        UIOptions={uiOptions}
      />
    </div>
  )
}

export { CanvasApp }
