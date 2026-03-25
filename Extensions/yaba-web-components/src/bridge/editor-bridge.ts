import type { Editor } from "@tiptap/core"
import { Selection, TextSelection } from "@tiptap/pm/state"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme, parseUrlParams } from "@/theme"
import {
  getSelectionSnapshot,
  type SelectionSnapshot,
} from "./selection-extractor"
import {
  ANNOTATIONS_META_KEY,
  selectionOverlapsYabaAnnotationMark,
  setStoredAnnotations,
  type AnnotationForRendering,
} from "@/tiptap/extensions/annotation-decorations"
import { YabaAnnotationMarkName } from "@/tiptap/extensions/yaba-annotation-mark"
import { setNoteEditorPlaceholderText } from "@/tiptap/editor-extensions"
import { getActiveFormattingJson } from "./editor-formatting"
import {
  publishEditorHostState,
  resetPublishedEditorHostState,
} from "./editor-host-events"

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
  getCanCreateAnnotation: () => boolean
  setAnnotations: (annotationsJson: string) => void
  scrollToAnnotation: (annotationId: string) => void
  setPlatform: (platform: Platform) => void
  setAppearance: (mode: AppearanceMode) => void
  setCursorColor: (color: string) => void
  setReaderPreferences: (preferences: Partial<ReaderPreferences>) => void
  setEditable: (isEditable: boolean) => void
  setPlaceholder: (placeholder: string) => void
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
  /** Apply `yabaAnnotation` mark with attrs `{ id }` to the current non-empty selection (viewer / link PDF readable). */
  applyAnnotationToSelection: (annotationId: string) => boolean
  /** Remove all `yabaAnnotation` marks with the given id from the document. Returns number of text nodes updated. */
  removeAnnotationFromDocument: (annotationId: string) => number
  onAnnotationTap?: (id: string) => void
}

function removeAnnotationMarksWithId(editor: Editor | null, annotationId: string): number {
  if (!editor) return 0
  const { state } = editor
  const markType = state.schema.marks[YabaAnnotationMarkName]
  if (!markType) return 0
  let count = 0
  const tr = state.tr
  state.doc.descendants((node, pos) => {
    if (!node.isText) return
    const mark = node.marks.find((m) => m.type === markType && m.attrs.id === annotationId)
    if (mark) {
      tr.removeMark(pos, pos + node.nodeSize, markType)
      count += 1
    }
  })
  if (count > 0) {
    editor.view.dispatch(tr)
  }
  return count
}

export type EditorCommandPayload =
  | { type: "toggleBold" }
  | { type: "toggleItalic" }
  | { type: "toggleUnderline" }
  | { type: "toggleStrikethrough" }
  | { type: "toggleSubscript" }
  | { type: "toggleSuperscript" }
  | { type: "toggleCode" }
  | { type: "toggleCodeBlock" }
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
  | { type: "updateInlineMath"; latex: string; pos: number }
  | { type: "updateBlockMath"; latex: string; pos: number }
  | { type: "insertText"; text: string }
  | { type: "setHeading"; level: number }
  | { type: "insertTable"; rows: number; cols: number; withHeaderRow?: boolean }
  | { type: "insertImage"; src: string }
  | { type: "addRowBefore" }
  | { type: "addRowAfter" }
  | { type: "deleteRow" }
  | { type: "addColumnBefore" }
  | { type: "addColumnAfter" }
  | { type: "deleteColumn" }
  | { type: "setTextHighlight"; colorRole: string }
  | { type: "unsetTextHighlight" }
  | { type: "toggleTextHighlight" }

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

/** `setContent` often leaves a range selection; collapse to a caret so toggles don't apply to the whole doc. */
function clearSelectionToCaretAtStart(editor: Editor): void {
  const pos = Selection.atStart(editor.state.doc).from
  const selection = TextSelection.create(editor.state.doc, pos)
  const tr = editor.state.tr.setSelection(selection)
  editor.view.dispatch(tr)
}

/**
 * After loading/replacing content: normalize to a caret at doc start, remember it, then blur.
 * We prefer an explicit user tap plus placeholder text over auto-focusing the note editor on load.
 */
function applyInitialFocusStateAfterContent(editor: Editor): void {
  clearSelectionToCaretAtStart(editor)
  captureStoredCursorFromEditor()
  editor.commands.blur()
}

function getCanCreateAnnotationForCurrentSelection(): boolean {
  const snap = getSelectionSnapshot(editorInstance)
  if (!snap) return false
  const ed = editorInstance
  if (!ed) return false
  const { from, to } = ed.state.selection
  if (from === to) return false
  const page = document.body?.dataset.yabaPage
  if (page === "editor") return true
  const overlaps = selectionOverlapsYabaAnnotationMark(ed.state.doc, from, to)
  return !overlaps
}

function publishCurrentEditorState(): void {
  publishEditorHostState(editorInstance, getCanCreateAnnotationForCurrentSelection)
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

function withPreparedEditorSelection(run: (editor: Editor) => void): void {
  const ed = editorInstance
  if (!ed) return
  focusEditorRestoringCursor()
  run(ed)
  captureStoredCursorFromEditor()
  publishCurrentEditorState()
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
  resetPublishedEditorHostState()
  editor.on("selectionUpdate", () => {
    captureStoredCursorFromEditor()
    publishCurrentEditorState()
  })
  editor.on("transaction", () => {
    publishCurrentEditorState()
  })
  editor.on("focus", () => {
    publishCurrentEditorState()
  })
  editor.on("blur", () => {
    publishCurrentEditorState()
  })
  queueMicrotask(() => {
    applyInitialFocusStateAfterContent(editor)
    publishCurrentEditorState()
  })
  const urlParams = parseUrlParams()
  platform = urlParams.platform
  appearance = urlParams.appearance
  const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
  win.YabaEditorBridge = {
    isReady: () => !!editorInstance,
    getSelectionSnapshot: () => getSelectionSnapshot(editorInstance),
    getCanCreateAnnotation: () => getCanCreateAnnotationForCurrentSelection(),
    setAnnotations: (annotationsJson: string) => {
      try {
        const annotations: AnnotationForRendering[] =
          annotationsJson && annotationsJson.trim()
            ? JSON.parse(annotationsJson)
            : []
        setStoredAnnotations(annotations)
        if (editorInstance) {
          const tr = editorInstance.state.tr
          tr.setMeta(ANNOTATIONS_META_KEY, annotations)
          editorInstance.view.dispatch(tr)
        }
      } catch {
        setStoredAnnotations([])
      }
    },
    scrollToAnnotation: (annotationId: string) => {
      const el = document.querySelector(
        `.yaba-annotation-decoration[data-annotation-id="${annotationId}"]`
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
    setPlaceholder: (placeholder: string) => {
      setNoteEditorPlaceholderText(placeholder)
      editorInstance?.view.dispatch(editorInstance.state.tr)
      publishCurrentEditorState()
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
      if (editorInstance) {
        applyInitialFocusStateAfterContent(editorInstance)
        publishCurrentEditorState()
      }
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
      if (editorInstance) {
        applyInitialFocusStateAfterContent(editorInstance)
        publishCurrentEditorState()
      }
    },
    getDocumentJson: () => {
      const raw = JSON.stringify(editorInstance?.getJSON() ?? JSON.parse(EMPTY_DOC_JSON))
      return normalizeDocumentJsonAssetPathsForPersistence(raw)
    },
    getActiveFormatting: () => {
      return getActiveFormattingJson(editorInstance)
    },
    focus: () => {
      focusEditorRestoringCursor()
      publishCurrentEditorState()
    },
    unFocus: () => {
      captureStoredCursorFromEditor()
      editorInstance?.commands.blur()
      publishCurrentEditorState()
    },
    dispatch: (cmd: EditorCommandPayload) => {
      withPreparedEditorSelection((ed) => {
        const chain = ed.chain()

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
          case "toggleCodeBlock":
            chain.toggleCodeBlock().run()
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
          case "updateInlineMath":
            ed.commands.updateInlineMath({ latex: cmd.latex, pos: cmd.pos })
            break
          case "updateBlockMath":
            ed.commands.updateBlockMath({ latex: cmd.latex, pos: cmd.pos })
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
            break
          }
          case "setHeading": {
            const level = Math.min(6, Math.max(1, Math.floor(cmd.level))) as 1 | 2 | 3 | 4 | 5 | 6
            chain.setHeading({ level }).run()
            break
          }
          case "setTextHighlight": {
            const role = (cmd.colorRole || "YELLOW").toUpperCase()
            if (role === "NONE") {
              ed.chain().unsetHighlight().run()
            } else {
              ed.chain().setHighlight({ color: role }).run()
            }
            break
          }
          case "unsetTextHighlight":
            ed.chain().unsetHighlight().run()
            break
          case "toggleTextHighlight":
            ed.chain().toggleHighlight().run()
            break
        }
      })
    },
    applyAnnotationToSelection: (annotationId: string) => {
      const ed = editorInstance
      if (!ed) return false
      const { from, to } = ed.state.selection
      if (from === to) return false
      return ed.chain().focus().setMark(YabaAnnotationMarkName, { id: annotationId }).run()
    },
    removeAnnotationFromDocument: (annotationId: string) =>
      removeAnnotationMarksWithId(editorInstance, annotationId),
  }

  applyReaderPreferences()
}
