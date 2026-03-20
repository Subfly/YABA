import { useCallback, useEffect, useLayoutEffect, useMemo, useState } from "react"
import {
  Highlight,
  PdfHighlighter,
  PdfLoader,
  type IHighlight,
} from "react-pdf-highlighter"
import "react-pdf-highlighter/dist/style.css"

import { pdfBridgeRuntime } from "./pdf-bridge-runtime"
import {
  type PdfViewerLike,
  yabaHighlightsToLibraryHighlights,
} from "./pdf-yaba-highlights"
import type { PdfHighlightForRendering } from "./pdf-text-utils"
import "./pdf-highlighter-viewer-patch"

// Bundled worker matching `react-pdf-highlighter` / nested pdfjs-dist 4.4.x (offline-safe).
import pdfWorkerUrl from "../../../node_modules/react-pdf-highlighter/node_modules/pdfjs-dist/build/pdf.worker.min.mjs?url"

const SCALE_PAGE_WIDTH = "page-width"

function colorClassForRole(role: string): string {
  const map: Record<string, string> = {
    NONE: "yaba-highlight-yellow",
    BLUE: "yaba-highlight-blue",
    BROWN: "yaba-highlight-brown",
    CYAN: "yaba-highlight-cyan",
    GRAY: "yaba-highlight-gray",
    GREEN: "yaba-highlight-green",
    INDIGO: "yaba-highlight-indigo",
    MINT: "yaba-highlight-mint",
    ORANGE: "yaba-highlight-orange",
    PINK: "yaba-highlight-pink",
    PURPLE: "yaba-highlight-purple",
    RED: "yaba-highlight-red",
    TEAL: "yaba-highlight-teal",
    YELLOW: "yaba-highlight-yellow",
  }
  return map[role] ?? "yaba-highlight-yellow"
}

function YabaPdfHighlighterPane({
  pdfDocument,
  yabaHighlights,
}: {
  /** pdf.js document from `react-pdf-highlighter` (pdfjs-dist 4.x); avoid typing against root pdfjs 5.x. */
  pdfDocument: object
  yabaHighlights: PdfHighlightForRendering[]
}) {
  const [layerTick, setLayerTick] = useState(0)

  const colorById = useMemo(() => {
    const m = new Map<string, string>()
    yabaHighlights.forEach((h) => m.set(h.id, h.colorRole))
    return m
  }, [yabaHighlights])

  useEffect(() => {
    let cleared = false
    let off: (() => void) | undefined
    const timer = window.setInterval(() => {
      if (cleared) return
      const v = window.__YABA_PDF_VIEWER__
      const bus = v?.eventBus
      if (!bus) return
      window.clearInterval(timer)
      const handler = () => setLayerTick((t) => t + 1)
      bus.on("textlayerrendered", handler)
      off = () => {
        try {
          bus.off("textlayerrendered", handler)
        } catch {
          /* ignore */
        }
      }
      handler()
    }, 32)
    return () => {
      cleared = true
      window.clearInterval(timer)
      off?.()
    }
  }, [pdfDocument])

  const libHighlights: IHighlight[] = useMemo(() => {
    const viewer = window.__YABA_PDF_VIEWER__
    if (!viewer?.pagesCount) return []
    void layerTick
    return yabaHighlightsToLibraryHighlights(viewer as PdfViewerLike, yabaHighlights)
  }, [pdfDocument, yabaHighlights, layerTick])

  useEffect(() => {
    pdfBridgeRuntime.lastLibHighlights = libHighlights
  }, [libHighlights])

  const scrollRefHandler = useCallback((scrollTo: (h: IHighlight) => void) => {
    pdfBridgeRuntime.scrollToLib = scrollTo
  }, [])

  return (
    <PdfHighlighter
      pdfDocument={pdfDocument as never}
      pdfScaleValue={SCALE_PAGE_WIDTH}
      highlights={libHighlights}
      scrollRef={scrollRefHandler}
      onScrollChange={() => setLayerTick((t) => t + 1)}
      enableAreaSelection={() => false}
      onSelectionFinished={(_position, _content, hideTipAndSelection) => {
        hideTipAndSelection()
        return null
      }}
      highlightTransform={(
        highlight,
        _index,
        _setTip,
        _hideTip,
        _viewportToScaled,
        _screenshot,
        isScrolledTo,
      ) => {
        const role = colorById.get(highlight.id) ?? "YELLOW"
        const colorClass = colorClassForRole(role)
        return (
          <div
            className={`yaba-rph-highlight-wrap ${colorClass}`}
            data-highlight-id={highlight.id}
          >
            <Highlight
              isScrolledTo={isScrolledTo}
              comment={{ text: "", emoji: "" }}
              position={highlight.position}
              onClick={() => {
                const win = window as Window & {
                  YabaPdfBridge?: { onHighlightTap?: (id: string) => void }
                }
                win.YabaPdfBridge?.onHighlightTap?.(highlight.id)
              }}
            />
          </div>
        )
      }}
    />
  )
}

export default function YabaPdfViewerApp() {
  const [pdfUrl, setPdfUrlState] = useState("")
  const [yabaHighlights, setYabaHighlightsState] = useState<PdfHighlightForRendering[]>([])

  useLayoutEffect(() => {
    pdfBridgeRuntime.setPdfUrl = (url: string) => {
      setPdfUrlState(url)
    }
    pdfBridgeRuntime.setYabaHighlights = (next: PdfHighlightForRendering[]) => {
      setYabaHighlightsState(next)
    }

    if (pdfBridgeRuntime.pendingPdfUrl) {
      setPdfUrlState(pdfBridgeRuntime.pendingPdfUrl)
      pdfBridgeRuntime.pendingPdfUrl = null
    }
    if (pdfBridgeRuntime.pendingYabaHighlights) {
      setYabaHighlightsState(pdfBridgeRuntime.pendingYabaHighlights)
      pdfBridgeRuntime.pendingYabaHighlights = null
    }

    return () => {
      pdfBridgeRuntime.setPdfUrl = null
      pdfBridgeRuntime.setYabaHighlights = null
    }
  }, [])

  useEffect(() => {
    pdfBridgeRuntime.yabaHighlights = yabaHighlights
  }, [yabaHighlights])

  if (!pdfUrl) {
    return null
  }

  return (
    <div className="yaba-pdf-react-root">
      <PdfLoader url={pdfUrl} workerSrc={pdfWorkerUrl} beforeLoad={null}>
        {(pdfDocument) => (
          <YabaPdfHighlighterPane
            pdfDocument={pdfDocument}
            yabaHighlights={yabaHighlights}
          />
        )}
      </PdfLoader>
    </div>
  )
}
