import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react"
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

const VIEWER_EVENT_NAMES = [
  "pagesinit",
  "pagerendered",
  "textlayerrendered",
  "pagechanging",
  "scalechanging",
] as const

type ViewerBus = {
  on: (name: string, fn: (...args: unknown[]) => void) => void
  off: (name: string, fn: (...args: unknown[]) => void) => void
}

type ViewerWithBus = PdfViewerLike & {
  pagesCount?: number
  eventBus?: ViewerBus
}

function getViewerWithBus(): ViewerWithBus | null {
  const viewer = window.__YABA_PDF_VIEWER__ as ViewerWithBus | undefined
  return viewer ?? null
}

function emitHighlightTap(id: string): void {
  const win = window as Window & {
    YabaPdfBridge?: { onHighlightTap?: (highlightId: string) => void }
  }
  win.YabaPdfBridge?.onHighlightTap?.(id)
}

function YabaPdfHighlighterPane({
  pdfDocument,
  yabaHighlights,
}: {
  /** pdf.js document from `react-pdf-highlighter` (pdfjs-dist 4.x); avoid typing against root pdfjs 5.x. */
  pdfDocument: object
  yabaHighlights: PdfHighlightForRendering[]
}) {
  const [viewerRevision, setViewerRevision] = useState(0)
  const highlightCacheRef = useRef(new Map<string, IHighlight>())

  const bumpViewerRevision = useCallback(() => {
    setViewerRevision((v) => v + 1)
  }, [])

  const colorById = useMemo(() => {
    const m = new Map<string, string>()
    yabaHighlights.forEach((h) => m.set(h.id, h.colorRole))
    return m
  }, [yabaHighlights])

  useEffect(() => {
    let disposed = false
    let bus: ViewerBus | null = null
    let bindTimer: number | null = null
    const onViewerEvent = (): void => {
      bumpViewerRevision()
    }

    const unbindBus = (): void => {
      if (!bus) return
      for (const eventName of VIEWER_EVENT_NAMES) {
        try {
          bus.off(eventName, onViewerEvent)
        } catch {
          /* ignore */
        }
      }
      bus = null
    }

    const tryBind = (): void => {
      if (disposed) return
      const viewer = getViewerWithBus()
      const nextBus = viewer?.eventBus
      if (!nextBus || nextBus === bus) return
      unbindBus()
      bus = nextBus
      for (const eventName of VIEWER_EVENT_NAMES) {
        bus.on(eventName, onViewerEvent)
      }
      onViewerEvent()
      if ((viewer.pagesCount ?? 0) > 0 && bindTimer !== null) {
        window.clearInterval(bindTimer)
        bindTimer = null
      }
    }

    bindTimer = window.setInterval(tryBind, 64)
    tryBind()

    return () => {
      disposed = true
      if (bindTimer !== null) {
        window.clearInterval(bindTimer)
      }
      unbindBus()
    }
  }, [pdfDocument, bumpViewerRevision])

  useEffect(() => {
    const validIds = new Set(yabaHighlights.map((h) => h.id))
    for (const id of highlightCacheRef.current.keys()) {
      if (!validIds.has(id)) {
        highlightCacheRef.current.delete(id)
      }
    }
  }, [yabaHighlights])

  const libHighlights: IHighlight[] = useMemo(() => {
    const viewer = getViewerWithBus()
    if (!viewer?.pagesCount) return []
    const next = yabaHighlightsToLibraryHighlights(viewer as PdfViewerLike, yabaHighlights)
    const nextById = new Map(next.map((h) => [h.id, h]))
    return yabaHighlights
      .map((source) => nextById.get(source.id) ?? highlightCacheRef.current.get(source.id))
      .filter((highlight): highlight is IHighlight => Boolean(highlight))
  }, [pdfDocument, yabaHighlights, viewerRevision])

  useEffect(() => {
    for (const highlight of libHighlights) {
      highlightCacheRef.current.set(highlight.id, highlight)
    }
  }, [libHighlights])

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
      onScrollChange={bumpViewerRevision}
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
              onClick={() => emitHighlightTap(highlight.id)}
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
