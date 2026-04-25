import type { EditorFormattingState } from "../editor-formatting"
import type { TocJson } from "../toc-host-events"

export type YabaNativeHostFeature = "editor" | "read-it-later" | "epub" | "reader-converter" | "canvas"

/** Single envelope for all web -> native host events. */
export type YabaNativeHostPayload =
  | { type: "bridgeReady"; feature: YabaNativeHostFeature }
  | { type: "shellLoad"; result: "loaded" | "error" }
  | { type: "toc"; toc: TocJson | null }
  | { type: "noteAutosaveIdle" }
  | { type: "canvasAutosaveIdle" }
  | {
      type: "readerMetrics"
      canCreateAnnotation: boolean
      currentPage: number
      pageCount: number
      /** Rich-text editor toolbar state; omitted in PDF/EPUB/readable viewer. */
      formatting?: EditorFormattingState
    }
  | {
      type: "canvasMetrics"
      activeTool:
        | "selection"
        | "hand"
        | "draw"
        | "eraser"
        | "line"
        | "arrow"
        | "text"
        | "frame"
        | "rectangle"
        | "diamond"
        | "ellipse"
      hasSelection: boolean
      canUndo: boolean
      canRedo: boolean
      gridModeEnabled: boolean
      objectsSnapModeEnabled: boolean
    }
  /** Selection-driven style snapshot for native options sheet (YABA palette codes + slots). */
  | {
      type: "canvasStyleState"
      hasSelection: boolean
      selectionCount: number
      /** Excalidraw element `type` values present in the selection (unique). */
      selectionElementTypes: string[]
      /** First selected element's `type` (for display / primary row). */
      primaryElementType: string
      /** True when selection contains more than one distinct element type. */
      elementTypeMixed: boolean
      /**
       * Which option sections the native sheet should render. Source of truth from web.
       * e.g. stroke, background, strokeWidth, strokeStyle, sloppiness, edges, fontSize,
       * opacity, layers, delete, arrowType, startArrowhead, endArrowhead
       */
      availableOptionGroups: string[]
      strokeYabaCode: number
      backgroundYabaCode: number
      strokeWidthKey: "thin" | "bold" | "extraBold"
      strokeStyle: "solid" | "dashed" | "dotted"
      roughnessKey: "architect" | "artist" | "cartoonist"
      edgeKey: "sharp" | "round"
      fontSizeKey: "S" | "M" | "L" | "XL"
      /** 0–10 → Excalidraw opacity 0–100 in steps of 10 */
      opacityStep: number
      mixedStroke: boolean
      mixedBackground: boolean
      mixedStrokeWidth: boolean
      mixedStrokeStyle: boolean
      mixedRoughness: boolean
      mixedEdge: boolean
      mixedFontSize: boolean
      mixedOpacity: boolean
      /** Linear (`line` / `arrow`) routing: sharp straight, curved, or elbow orthogonal. */
      arrowTypeKey: "sharp" | "curved" | "elbow"
      mixedArrowType: boolean
      /** Excalidraw `Arrowhead` string or "none" for null. */
      startArrowheadKey: string
      endArrowheadKey: string
      mixedStartArrowhead: boolean
      mixedEndArrowhead: boolean
      /** Allowed arrowhead keys for start/end pickers (Excalidraw union + "none"). */
      availableStartArrowheads: string[]
      availableEndArrowheads: string[]
      /** Excalidraw fill hatch / pattern (meaningful when fill color is not transparent). */
      fillStyleKey: "solid" | "hachure" | "cross-hatch" | "zigzag"
      mixedFillStyle: boolean
    }
  | { type: "annotationTap"; id: string }
  | { type: "mathTap"; kind: "inline" | "block"; pos: number; latex: string }
  | {
      type: "inlineLinkTap"
      pos: number
      text: string
      url: string
    }
  | {
      type: "inlineMentionTap"
      pos: number
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      type: "canvasLinkTap"
      elementId: string
      text: string
      url: string
    }
  | {
      type: "canvasMentionTap"
      elementId: string
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      type: "converterJob"
      jobId: string
      kind: "html" | "pdf" | "epub"
      status: "pending" | "done" | "error"
      outputJson?: string
      error?: string
    }
  /** Async result of [window.YabaEditorBridge.startPdfExportJob] (html2pdf.js). */
  | {
      type: "editorPdfExport"
      jobId: string
      status: "done" | "error"
      pdfBase64?: string
      error?: string
    }
