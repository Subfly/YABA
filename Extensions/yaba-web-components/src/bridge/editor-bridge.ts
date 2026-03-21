import type { Editor } from "@tiptap/core"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme } from "@/theme"
import {
  getSelectionSnapshot,
  type SelectionSnapshot,
} from "./selection-extractor"
import {
  getHighlightRanges,
  getStoredHighlights,
  HIGHLIGHTS_META_KEY,
  setStoredHighlights,
  type HighlightForRendering,
} from "@/tiptap/extensions/highlight-decorations"

export type ReaderTheme = "system" | "dark" | "light" | "sepia"
export type ReaderFontSize = "small" | "medium" | "large"
export type ReaderLineHeight = "normal" | "relaxed"

export interface ReaderPreferences {
  theme: ReaderTheme
  fontSize: ReaderFontSize
  lineHeight: ReaderLineHeight
}

const EMPTY_DOC_JSON = '{"type":"doc","content":[]}'

function rewriteAssetPathsInDocumentJson(json: string, assetsBaseUrl: string): string {
  if (!json.includes("../assets/")) return json
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return json.replaceAll("../assets/", `${base}assets/`)
}

function rewriteAssetPathsInReaderHtml(html: string, assetsBaseUrl: string): string {
  if (!html.includes("../assets/")) return html
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return html.replaceAll("../assets/", `${base}assets/`)
}

export interface YabaEditorBridge {
  isReady: () => boolean
  getSelectionSnapshot: () => SelectionSnapshot | null
  getCanCreateHighlight: () => boolean
  setHighlights: (highlightsJson: string) => void
  scrollToHighlight: (highlightId: string) => void
  /** True when editor document JSON differs from last [flush] / [setDocumentJson] baseline. */
  isDirty: () => boolean
  /** Marks current document as clean for [isDirty] (after native save). */
  flush: () => void
  setPlatform: (platform: Platform) => void
  setAppearance: (mode: AppearanceMode) => void
  setCursorColor: (color: string) => void
  setReaderPreferences: (preferences: Partial<ReaderPreferences>) => void
  setEditable: (isEditable: boolean) => void
  /** TipTap/ProseMirror document as JSON string. */
  setDocumentJson: (documentJson: string, options?: { assetsBaseUrl?: string }) => void
  /** Sanitized reader HTML for the read-only viewer (TipTap parses HTML → document). */
  setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => void
  getDocumentJson: () => string
  focus: () => void
  blur: () => void
  dispatch: (command: EditorCommandPayload) => void
}

export type EditorCommandPayload =
  | { type: "toggleBold" }
  | { type: "toggleItalic" }
  | { type: "toggleUnderline" }
  | { type: "toggleStrikethrough" }
  | { type: "toggleSubscript" }
  | { type: "toggleSuperscript" }
  | { type: "toggleCode" }
  | { type: "toggleQuote" }
  | { type: "insertHr" }
  | { type: "toggleBulletedList" }
  | { type: "toggleNumberedList" }
  | { type: "indent" }
  | { type: "outdent" }
  | { type: "undo" }
  | { type: "redo" }
  | { type: "insertLink"; url: string }
  | { type: "removeLink" }
  | { type: "insertYouTube"; url: string }
  | { type: "insertInlineMath"; latex: string }
  | { type: "insertBlockMath"; latex: string }

let editorInstance: Editor | null = null
let lastPersistedDocumentJson = EMPTY_DOC_JSON
let platform: Platform = "compose"
let appearance: AppearanceMode = "auto"
let cursorColor: string | null = null
let readerPreferences: ReaderPreferences = {
  theme: "system",
  fontSize: "medium",
  lineHeight: "normal",
}
let systemColorSchemeMedia: MediaQueryList | null = null
let systemColorSchemeListener: (() => void) | null = null

const readerFontSizeCssByMode: Record<ReaderFontSize, string> = {
  small: "16px",
  medium: "18px",
  large: "22px",
}

const readerLineHeightCssByMode: Record<ReaderLineHeight, string> = {
  normal: "1.6",
  relaxed: "1.8",
}

function clearSystemColorSchemeListener(): void {
  if (!systemColorSchemeMedia || !systemColorSchemeListener) return

  systemColorSchemeMedia.removeEventListener("change", systemColorSchemeListener)
  systemColorSchemeListener = null
  systemColorSchemeMedia = null
}

function ensureSystemColorSchemeListener(): void {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return
  if (systemColorSchemeListener) return

  systemColorSchemeMedia = window.matchMedia("(prefers-color-scheme: dark)")
  const onChange = () => {
    if (readerPreferences.theme !== "system") return
    applyTheme(platform, appearance, cursorColor)
    applyReaderThemeVars(readerPreferences.theme)
  }

  systemColorSchemeMedia.addEventListener("change", onChange)
  systemColorSchemeListener = onChange
}

function applyReaderThemeVars(theme: ReaderTheme): void {
  const root = document.documentElement

  if (theme === "system") {
    root.style.setProperty("--yaba-reader-bg", "transparent")
    root.style.setProperty("--yaba-reader-on-bg", "var(--yaba-on-bg)")
    return
  }

  if (theme === "dark" || theme === "light") {
    root.style.setProperty("--yaba-reader-bg", "var(--yaba-bg)")
    root.style.setProperty("--yaba-reader-on-bg", "var(--yaba-on-bg)")
    return
  }

  root.style.setProperty("--yaba-reader-bg", "#f4ecd8")
  root.style.setProperty("--yaba-reader-on-bg", "#5b4636")
}

function applyReaderTypographyVars(prefs: ReaderPreferences): void {
  const root = document.documentElement
  root.style.setProperty("--yaba-reader-font-size", readerFontSizeCssByMode[prefs.fontSize])
  root.style.setProperty("--yaba-reader-line-height", readerLineHeightCssByMode[prefs.lineHeight])
}

function applyReaderPreferences(): void {
  const page = document.body?.dataset.yabaPage
  /** Read-it-later viewer + note editor: same reader theme + typography pipeline (incl. automatic / system). */
  const useReaderAppearancePipeline = page === "viewer" || page === "editor"

  if (useReaderAppearancePipeline) {
    if (readerPreferences.theme === "system") {
      applyTheme(platform, appearance, cursorColor)
      if (appearance === "auto") ensureSystemColorSchemeListener()
      else clearSystemColorSchemeListener()
    } else if (readerPreferences.theme === "dark") {
      applyTheme(platform, "dark", cursorColor)
      clearSystemColorSchemeListener()
    } else if (readerPreferences.theme === "light") {
      applyTheme(platform, "light", cursorColor)
      clearSystemColorSchemeListener()
    } else {
      applyTheme(platform, "light", cursorColor)
      clearSystemColorSchemeListener()
    }
  } else {
    applyTheme(platform, appearance, cursorColor)
    clearSystemColorSchemeListener()
  }

  applyReaderThemeVars(readerPreferences.theme)
  applyReaderTypographyVars(readerPreferences)
}

export function initEditorBridge(editor: Editor): void {
  editorInstance = editor
  lastPersistedDocumentJson = JSON.stringify(editor.getJSON())
  const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
  win.YabaEditorBridge = {
    isReady: () => !!editorInstance,
    getSelectionSnapshot: () => getSelectionSnapshot(editorInstance),
    getCanCreateHighlight: () => {
      const snap = getSelectionSnapshot(editorInstance)
      if (!snap) return false
      const ed = editorInstance
      if (!ed) return false
      const { from, to } = ed.state.selection
      if (from === to) return false
      const ranges = getHighlightRanges(ed.state.doc, getStoredHighlights())
      const overlaps = ranges.some((r) => from < r.to && to > r.from)
      return !overlaps
    },
    setHighlights: (highlightsJson: string) => {
      try {
        const highlights: HighlightForRendering[] =
          highlightsJson && highlightsJson.trim()
            ? JSON.parse(highlightsJson)
            : []
        setStoredHighlights(highlights)
        if (editorInstance) {
          const tr = editorInstance.state.tr
          tr.setMeta(HIGHLIGHTS_META_KEY, highlights)
          editorInstance.view.dispatch(tr)
        }
      } catch {
        setStoredHighlights([])
      }
    },
    scrollToHighlight: (highlightId: string) => {
      const el = document.querySelector(
        `.yaba-highlight[data-highlight-id="${highlightId}"]`
      ) as HTMLElement | null
      el?.scrollIntoView({ behavior: "smooth", block: "center" })
    },
    setPlatform: (p: Platform) => {
      platform = p
      applyReaderPreferences()
    },
    setAppearance: (mode: AppearanceMode) => {
      appearance = mode
      applyReaderPreferences()
    },
    setCursorColor: (color: string) => {
      cursorColor = color
      applyReaderPreferences()
    },
    setReaderPreferences: (prefs: Partial<ReaderPreferences>) => {
      readerPreferences = {
        ...readerPreferences,
        ...prefs,
      }
      applyReaderPreferences()
    },
    setEditable: (isEditable: boolean) => {
      editorInstance?.setEditable(isEditable)
    },
    setDocumentJson: (documentJson: string, options?: { assetsBaseUrl?: string }) => {
      let payload = documentJson?.trim() ? documentJson : EMPTY_DOC_JSON
      if (options?.assetsBaseUrl) {
        payload = rewriteAssetPathsInDocumentJson(payload, options.assetsBaseUrl)
      }
      let doc: Record<string, unknown>
      try {
        doc = JSON.parse(payload) as Record<string, unknown>
      } catch {
        doc = JSON.parse(EMPTY_DOC_JSON) as Record<string, unknown>
      }
      editorInstance?.commands.setContent(doc, { emitUpdate: false })
      lastPersistedDocumentJson = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
    },
    setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => {
      let payload = html?.trim() ? html : "<p></p>"
      if (options?.assetsBaseUrl) {
        payload = rewriteAssetPathsInReaderHtml(payload, options.assetsBaseUrl)
      }
      editorInstance?.commands.setContent(payload, { emitUpdate: false })
      lastPersistedDocumentJson = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
    },
    getDocumentJson: () => {
      return JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
    },
    isDirty: () => {
      const current = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
      return current !== lastPersistedDocumentJson
    },
    flush: () => {
      lastPersistedDocumentJson = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
    },
    focus: () => editorInstance?.commands.focus(),
    blur: () => editorInstance?.commands.blur(),
    dispatch: (cmd: EditorCommandPayload) => {
      const ed = editorInstance
      if (!ed) return

      const chain = ed.chain().focus()

      switch (cmd.type) {
        case "toggleBold":
          chain.toggleBold().run()
          break
        case "toggleItalic":
          chain.toggleItalic().run()
          break
        case "toggleUnderline":
          chain.toggleUnderline().run()
          break
        case "toggleSubscript":
          chain.toggleSubscript().run()
          break
        case "toggleSuperscript":
          chain.toggleSuperscript().run()
          break
        case "toggleStrikethrough":
          chain.toggleStrike().run()
          break
        case "toggleCode":
          chain.toggleCode().run()
          break
        case "toggleQuote":
          chain.toggleBlockquote().run()
          break
        case "insertHr":
          chain.setHorizontalRule().run()
          break
        case "toggleBulletedList":
          chain.toggleBulletList().run()
          break
        case "toggleNumberedList":
          chain.toggleOrderedList().run()
          break
        case "indent":
          chain.sinkListItem("listItem").run()
          break
        case "outdent":
          chain.liftListItem("listItem").run()
          break
        case "undo":
          chain.undo().run()
          break
        case "redo":
          chain.redo().run()
          break
        case "insertLink":
          chain.setLink({ href: cmd.url }).run()
          break
        case "removeLink":
          chain.unsetLink().run()
          break
        case "insertYouTube":
          ed.commands.setYoutubeVideo({ src: cmd.url })
          break
        case "insertInlineMath":
          ed.commands.insertInlineMath({ latex: cmd.latex })
          break
        case "insertBlockMath":
          ed.commands.insertBlockMath({ latex: cmd.latex })
          break
      }
    },
  }

  applyReaderPreferences()
}
