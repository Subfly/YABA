import type { Platform, AppearanceMode } from "@/theme"
import type { SelectionSnapshot } from "./selection-extractor"
import type { EditorCommandPayload } from "./editor-command-payload"

export type ReaderTheme = "system" | "dark" | "light" | "sepia"
export type ReaderFontSize = "small" | "medium" | "large"
export type ReaderLineHeight = "normal" | "relaxed"

export interface ReaderPreferences {
  theme: ReaderTheme
  fontSize: ReaderFontSize
  lineHeight: ReaderLineHeight
}

export interface YabaEditorBridge {
  isReady: () => boolean
  getSelectionSnapshot: () => SelectionSnapshot | null
  getSelectedText: () => string
  getCanCreateAnnotation: () => boolean
  setAnnotations: (annotationsJson: string) => void
  scrollToAnnotation: (annotationId: string) => void
  setPlatform: (platform: Platform) => void
  setAppearance: (mode: AppearanceMode) => void
  setCursorColor: (color: string) => void
  setWebChromeInsets: (topChromeInsetPx: number) => void
  setReaderPreferences: (preferences: Partial<ReaderPreferences>) => void
  setEditable: (isEditable: boolean) => void
  setPlaceholder: (placeholder: string) => void
  /** Markdown string (legacy name `documentJson`). */
  setDocumentJson: (documentJson: string, options?: { assetsBaseUrl?: string }) => void
  /** Sanitized reader HTML → converted to Markdown for the read-only viewer. */
  setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => void
  getDocumentJson: () => string
  getUsedInlineAssetSrcs: () => string
  getActiveFormatting: () => string
  focus: () => void
  unFocus: () => void
  dispatch: (command: EditorCommandPayload) => void
  applyAnnotationToSelection: (annotationId: string) => boolean
  removeAnnotationFromDocument: (annotationId: string) => number
  onAnnotationTap?: (id: string) => void
  navigateToTocItem: (id: string, extrasJson?: string | null) => void
  exportMarkdown: () => string
  startPdfExportJob: (jobId: string) => void
}
