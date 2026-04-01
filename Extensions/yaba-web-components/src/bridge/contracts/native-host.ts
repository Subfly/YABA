import type { EditorFormattingState } from "../editor-formatting"
import type { TocJson } from "../toc-host-events"

export type YabaNativeHostFeature = "editor" | "viewer" | "pdf" | "epub" | "converter" | "canvas"

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
