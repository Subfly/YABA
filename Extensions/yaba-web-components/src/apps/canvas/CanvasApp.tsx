import { useCallback, useEffect, useMemo, useRef, useState } from "react"
import type { ExcalidrawImperativeAPI, ExcalidrawInitialDataState } from "@excalidraw/excalidraw/types"
import { Excalidraw, FONT_FAMILY } from "@excalidraw/excalidraw"
import "@excalidraw/excalidraw/index.css"
import "./canvas-host.css"
import { initCanvasBridge, onCanvasChanged } from "@/bridge/canvas-bridge"
import { CanvasLinkBadgeLayer } from "./CanvasLinkBadgeLayer"

function CanvasApp() {
  const rootRef = useRef<HTMLDivElement>(null)
  const [excalidrawApi, setExcalidrawApi] = useState<ExcalidrawImperativeAPI | null>(null)
  const [badgeRevision, setBadgeRevision] = useState(0)
  const badgeRafRef = useRef<number | null>(null)

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
    setExcalidrawApi((prev) => (prev != null ? prev : api))
    initCanvasBridge(api)
  }, [])

  const handleCanvasChange = useCallback(() => {
    onCanvasChanged()
    if (badgeRafRef.current != null) {
      cancelAnimationFrame(badgeRafRef.current)
    }
    badgeRafRef.current = requestAnimationFrame(() => {
      badgeRafRef.current = null
      setBadgeRevision((r) => r + 1)
    })
  }, [])

  const renderTopRightUI = useCallback(() => null, [])

  useEffect(() => {
    const el = rootRef.current
    if (!el) return
    const preventContextMenu = (e: Event) => e.preventDefault()
    el.addEventListener("contextmenu", preventContextMenu)
    return () => el.removeEventListener("contextmenu", preventContextMenu)
  }, [])

  useEffect(
    () => () => {
      if (badgeRafRef.current != null) cancelAnimationFrame(badgeRafRef.current)
    },
    [],
  )

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
        UIOptions={uiOptions}
      />
      <CanvasLinkBadgeLayer api={excalidrawApi} revision={badgeRevision} />
    </div>
  )
}

export { CanvasApp }
