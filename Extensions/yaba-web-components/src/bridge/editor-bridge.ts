import type { Editor } from "@tiptap/core"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme, parseUrlParams } from "@/theme"
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

/** Set when [setDocumentJson] / [setReaderHtml] runs with options; used to resolve inline image src and normalize saves. */
let lastAssetsBaseUrl: string | undefined

function rewriteAssetPathsInDocumentJson(json: string, assetsBaseUrl: string): string {
  if (!json.includes("../assets/")) return json
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return json.replaceAll("../assets/", `${base}assets/`)
}

/** Same rewrite as document load — [insertImage] must use this or images 404 (relative to editor origin). */
function resolveImageSrcForEditor(src: string): string {
  if (!lastAssetsBaseUrl || !src.includes("../assets/")) return src
  const base = lastAssetsBaseUrl.replace(/\/?$/, "/")
  return src.replaceAll("../assets/", `${base}assets/`)
}

/** Persist canonical `../assets/…` paths (matches on-disk JSON and cross-platform loads). */
function normalizeDocumentJsonAssetPathsForPersistence(json: string): string {
  if (!lastAssetsBaseUrl || !json.includes("assets/")) return json
  const base = lastAssetsBaseUrl.replace(/\/?$/, "/")
  const absolutePrefix = `${base}assets/`
  if (!json.includes(absolutePrefix)) return json
  return json.replaceAll(absolutePrefix, "../assets/")
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
  /** JSON string of active marks / undo availability for native toolbars. */
  getActiveFormatting: () => string
  /** Restores DOM focus; reapplies the last remembered text cursor when possible. */
  focus: () => void
  /** Blurs the editor and dismisses the keyboard; remembers cursor first for [focus]. */
  unFocus: () => void
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
  | { type: "toggleTaskList" }
  | { type: "indent" }
  | { type: "outdent" }
  | { type: "undo" }
  | { type: "redo" }
  | { type: "insertLink"; url: string }
  | { type: "removeLink" }
  | { type: "insertInlineMath"; latex: string }
  | { type: "insertBlockMath"; latex: string }
  /** Plain text at selection (e.g. markdown `# ` for headings). */
  | { type: "insertText"; text: string }
  | { type: "insertTable"; rows: number; cols: number; withHeaderRow?: boolean }
  | { type: "insertImage"; src: string }
  | { type: "addRowBefore" }
  | { type: "addRowAfter" }
  | { type: "deleteRow" }
  | { type: "addColumnBefore" }
  | { type: "addColumnAfter" }
  | { type: "deleteColumn" }

let editorInstance: Editor | null = null
/** Latest ProseMirror selection (anchor/head) for restoring the caret after [unFocus] / native chrome. */
let lastStoredCursor: { anchor: number; head: number } | null = null
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

function captureStoredCursorFromEditor(): void {
  const ed = editorInstance
  if (!ed) return
  const { anchor, head } = ed.state.selection
  lastStoredCursor = { anchor, head }
}

function focusEditorRestoringCursor(): void {
  const ed = editorInstance
  if (!ed) return
  try {
    if (lastStoredCursor) {
      const size = ed.state.doc.content.size
      const from = Math.max(0, Math.min(lastStoredCursor.anchor, size))
      const to = Math.max(0, Math.min(lastStoredCursor.head, size))
      ed.chain().setTextSelection({ from, to }).focus().run()
      return
    }
  } catch {
    // Invalid range after doc changes — fall through to plain focus.
  }
  ed.commands.focus()
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
  captureStoredCursorFromEditor()
  editor.on("selectionUpdate", () => {
    captureStoredCursorFromEditor()
  })
  const urlParams = parseUrlParams()
  platform = urlParams.platform
  appearance = urlParams.appearance
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
      if (options?.assetsBaseUrl) {
        lastAssetsBaseUrl = options.assetsBaseUrl
      }
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
    },
    setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => {
      if (options?.assetsBaseUrl) {
        lastAssetsBaseUrl = options.assetsBaseUrl
      }
      let payload = html?.trim() ? html : "<p></p>"
      if (options?.assetsBaseUrl) {
        payload = rewriteAssetPathsInReaderHtml(payload, options.assetsBaseUrl)
      }
      editorInstance?.commands.setContent(payload, { emitUpdate: false })
    },
    getDocumentJson: () => {
      const raw = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
      return normalizeDocumentJsonAssetPathsForPersistence(raw)
    },
    getActiveFormatting: () => {
      const ed = editorInstance
      if (!ed) {
        return JSON.stringify({
          bold: false,
          italic: false,
          underline: false,
          strikethrough: false,
          subscript: false,
          superscript: false,
          code: false,
          blockquote: false,
          bulletList: false,
          orderedList: false,
          taskList: false,
          canUndo: false,
          canRedo: false,
          canIndent: false,
          canOutdent: false,
          inTable: false,
          canAddRowBefore: false,
          canAddRowAfter: false,
          canDeleteRow: false,
          canAddColumnBefore: false,
          canAddColumnAfter: false,
          canDeleteColumn: false,
        })
      }
      const inTable = ed.isActive("table")
      return JSON.stringify({
        bold: ed.isActive("bold"),
        italic: ed.isActive("italic"),
        underline: ed.isActive("underline"),
        strikethrough: ed.isActive("strike"),
        subscript: ed.isActive("subscript"),
        superscript: ed.isActive("superscript"),
        code: ed.isActive("code"),
        blockquote: ed.isActive("blockquote"),
        bulletList: ed.isActive("bulletList"),
        orderedList: ed.isActive("orderedList"),
        taskList: ed.isActive("taskList"),
        canUndo: ed.can().undo(),
        canRedo: ed.can().redo(),
        canIndent: ed.can().sinkListItem("listItem"),
        canOutdent: ed.can().liftListItem("listItem"),
        inTable,
        canAddRowBefore: inTable && ed.can().addRowBefore(),
        canAddRowAfter: inTable && ed.can().addRowAfter(),
        canDeleteRow: inTable && ed.can().deleteRow(),
        canAddColumnBefore: inTable && ed.can().addColumnBefore(),
        canAddColumnAfter: inTable && ed.can().addColumnAfter(),
        canDeleteColumn: inTable && ed.can().deleteColumn(),
      })
    },
    focus: () => {
      focusEditorRestoringCursor()
    },
    unFocus: () => {
      captureStoredCursorFromEditor()
      editorInstance?.commands.blur()
    },
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
        case "toggleTaskList":
          chain.toggleTaskList().run()
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
        case "insertInlineMath":
          ed.commands.insertInlineMath({ latex: cmd.latex })
          break
        case "insertBlockMath":
          ed.commands.insertBlockMath({ latex: cmd.latex })
          break
        case "insertTable": {
          const rows = Math.max(1, Math.min(20, Math.floor(cmd.rows)))
          const cols = Math.max(1, Math.min(20, Math.floor(cmd.cols)))
          ed.commands.insertTable({
            rows,
            cols,
            withHeaderRow: cmd.withHeaderRow ?? false,
          })
          break
        }
        case "insertImage":
          ed.commands.setImage({ src: resolveImageSrcForEditor(cmd.src) })
          break
        case "addRowBefore":
          ed.commands.addRowBefore()
          break
        case "addRowAfter":
          ed.commands.addRowAfter()
          break
        case "deleteRow":
          ed.commands.deleteRow()
          break
        case "addColumnBefore":
          ed.commands.addColumnBefore()
          break
        case "addColumnAfter":
          ed.commands.addColumnAfter()
          break
        case "deleteColumn":
          ed.commands.deleteColumn()
          break
        case "insertText": {
          const { from, to } = ed.state.selection
          const tr = ed.state.tr.insertText(cmd.text, from, to)
          ed.view.dispatch(tr)
          ed.commands.focus()
          break
        }
      }
    },
  }

  applyReaderPreferences()
}
