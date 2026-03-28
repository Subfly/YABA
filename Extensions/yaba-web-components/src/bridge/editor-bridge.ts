import type { Editor, JSONContent } from "@tiptap/core"
import DOMPurify from "dompurify"
import showdown from "showdown"
import { Selection, TextSelection } from "@tiptap/pm/state"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme, parseUrlParams } from "@/theme"
import {
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
} from "@/theme/reader-document-vars"
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
import {
  publishShellLoad,
  scheduleNoteAutosaveAfterEditorActivity,
  setNoteEditorAutosaveIdleEnabled,
} from "./shell-host-events"
import { publishToc, resetPublishedToc, type TocJson } from "./toc-host-events"
import { exportMarkdownBundleFromEditor, exportPdfBase64FromEditor } from "./editor-export"

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
  getSelectedText: () => string
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
  navigateToTocItem: (id: string, extrasJson?: string | null) => void
  /** JSON string: `{ markdown, assets: [{ relativePath, dataBase64 }] }` for Save Copy → MD. */
  exportMarkdownBundleJson: () => Promise<string>
  /** Base64 PDF bytes (no data: prefix) for Save Copy → PDF. */
  exportPdfBase64: () => Promise<string>
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
  | { type: "insertLink"; text: string; url: string }
  | { type: "updateLink"; text: string; url: string; pos: number }
  | { type: "removeLink"; pos: number }
  | {
      type: "insertMention"
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      type: "updateMention"
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
      pos: number
    }
  | { type: "removeMention"; pos: number }
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
/** First successful or failed application of document/HTML to the shell (one-shot per page load). */
let editorShellLoadNotified = false
/** Debounced ToC publish (matches note autosave idle ~1s). */
let tocPublishTimer: ReturnType<typeof setTimeout> | null = null
const TOC_PUBLISH_DEBOUNCE_MS = 1000

function clearTocPublishTimer(): void {
  if (tocPublishTimer !== null) {
    clearTimeout(tocPublishTimer)
    tocPublishTimer = null
  }
}

function scheduleHeadingTocPublish(): void {
  const page = typeof document !== "undefined" ? document.body?.dataset.yabaPage : undefined
  if (page !== "editor" && page !== "viewer") return
  clearTocPublishTimer()
  tocPublishTimer = setTimeout(() => {
    tocPublishTimer = null
    publishHeadingTocFromEditor()
  }, TOC_PUBLISH_DEBOUNCE_MS)
}

function buildHeadingTocJson(ed: Editor): TocJson | null {
  const doc = ed.state.doc
  const headings: { pos: number; level: number; text: string }[] = []
  doc.descendants((node, pos) => {
    if (node.type.name === "heading") {
      const level = Math.min(6, Math.max(1, Number(node.attrs.level) || 1))
      const text = node.textContent.trim()
      if (text.length > 0) {
        headings.push({ pos, level, text })
      }
    }
    return true
  })
  if (headings.length === 0) return null

  type Item = {
    id: string
    title: string
    level: number
    children: Item[]
    extrasJson?: string | null
  }
  const root: Item[] = []
  const stack: { level: number; children: Item[] }[] = [{ level: 0, children: root }]
  let idCounter = 0
  for (const h of headings) {
    const id = `toc-h-${idCounter++}`
    const extrasJson = JSON.stringify({ pos: h.pos })
    const item: Item = {
      id,
      title: h.text,
      level: h.level,
      children: [],
      extrasJson,
    }
    while (stack.length > 1 && stack[stack.length - 1].level >= h.level) {
      stack.pop()
    }
    stack[stack.length - 1].children.push(item)
    stack.push({ level: h.level, children: item.children })
  }
  return { items: root }
}

/**
 * Same traversal and filters as [buildHeadingTocJson] (`heading` nodes with non-empty trimmed text).
 * Resolves `toc-h-${n}` at navigation time so we do not rely on [extrasJson] `pos`, which drifts after
 * document updates (annotations, marks) — same idea as [scrollToAnnotation] using live identity, not stale coords.
 */
function findHeadingDocPosForTocItemId(ed: Editor, tocItemId: string): number | null {
  const m = /^toc-h-(\d+)$/.exec(tocItemId)
  if (!m) return null
  const targetIndex = parseInt(m[1], 10)
  if (targetIndex < 0 || !Number.isFinite(targetIndex)) return null
  const headingStarts: number[] = []
  ed.state.doc.descendants((node, pos) => {
    if (node.type.name === "heading") {
      const text = node.textContent.trim()
      if (text.length > 0) {
        headingStarts.push(pos)
      }
    }
    return true
  })
  return headingStarts[targetIndex] ?? null
}

function getHeadingIndexFromTocItemId(tocItemId: string): number | null {
  const m = /^toc-h-(\d+)$/.exec(tocItemId)
  if (!m) return null
  const index = parseInt(m[1], 10)
  if (!Number.isFinite(index) || index < 0) return null
  return index
}

function findHeadingElementForTocItemId(ed: Editor, tocItemId: string): HTMLElement | null {
  const headingIndex = getHeadingIndexFromTocItemId(tocItemId)
  if (headingIndex == null) return null
  const headingElements = Array.from(
    ed.view.dom.querySelectorAll<HTMLElement>("h1, h2, h3, h4, h5, h6")
  ).filter((el) => el.textContent?.trim().length)
  return headingElements[headingIndex] ?? null
}

function scrollHeadingIntoEditorView(ed: Editor, headingEl: HTMLElement): void {
  const container =
    (ed.view.dom.closest("[data-yaba-editor-root]") as HTMLElement | null) ??
    (document.querySelector(".yaba-editor-container") as HTMLElement | null)

  if (!container) {
    headingEl.scrollIntoView({ behavior: "smooth", block: "center" })
    return
  }

  const containerRect = container.getBoundingClientRect()
  const headingRect = headingEl.getBoundingClientRect()
  const desiredTop =
    container.scrollTop +
    (headingRect.top - containerRect.top) -
    (container.clientHeight / 2 - headingRect.height / 2)
  const maxTop = Math.max(0, container.scrollHeight - container.clientHeight)
  const top = Math.max(0, Math.min(desiredTop, maxTop))
  container.scrollTo({ top, behavior: "smooth" })
}

function publishHeadingTocFromEditor(): void {
  const ed = editorInstance
  if (!ed) {
    publishToc(null)
    return
  }
  const toc = buildHeadingTocJson(ed)
  publishToc(toc)
}
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
    applyReaderThemeCssVars(readerPreferences.theme)
  }

  systemColorSchemeMedia.addEventListener("change", onChange)
  systemColorSchemeListener = onChange
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

function insertInlineMathKeepingCaretAfterNode(ed: Editor, latex: string): void {
  const inlineMathType = ed.state.schema.nodes.inlineMath
  if (!inlineMathType) {
    ed.commands.insertInlineMath({ latex })
    return
  }

  const { from, to } = ed.state.selection
  const mathNode: ProseMirrorNode = inlineMathType.create({ latex })
  const tr = ed.state.tr.replaceRangeWith(from, to, mathNode)
  const caretPos = from + mathNode.nodeSize
  tr.setSelection(TextSelection.create(tr.doc, caretPos))
  ed.view.dispatch(tr)
}

/** Clipboard paste normalization converter (input treated as HTML-oriented content). */
const pasteHtmlConverter = new showdown.Converter({
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
})

/** DOMPurify on plain text can escape markdown markers (`>` -> `&gt;`); decode before Showdown. */
function decodeHtmlEntities(value: string): string {
  const textarea = document.createElement("textarea")
  textarea.innerHTML = value
  return textarea.value
}

/** Minimal markdown-it style helpers for superscript/subscript before Showdown conversion. */
function applySupSubMarkdownSyntax(value: string): string {
  const withSup = value.replace(/(^|[^\^])\^([^^\n]+)\^/g, "$1<sup>$2</sup>")
  return withSup.replace(/(^|[^~])~([^~\n]+)~(?!~)/g, "$1<sub>$2</sub>")
}

/**
 * Serialize the current selection to Markdown using the TipTap Markdown extension.
 */
function getSelectionMarkdownFromEditor(ed: Editor): string {
  const { from, to } = ed.state.selection
  if (from === to) return ""
  try {
    const slice = ed.state.doc.slice(from, to)
    const json = slice.content.toJSON() as JSONContent[] | JSONContent | null | undefined
    const content: JSONContent[] = Array.isArray(json) ? json : json != null ? [json] : []
    const docJson: JSONContent = { type: "doc", content }
    const pmDoc = ed.state.schema.nodeFromJSON(docJson)

    const storage = (
      ed as unknown as {
        storage?: {
          markdown?: {
            serializer?: { serialize?: (doc: ProseMirrorNode) => string }
          }
        }
      }
    ).storage?.markdown
    const serializedByStorage = storage?.serializer?.serialize?.(pmDoc)
    if (serializedByStorage != null && serializedByStorage.trim().length > 0) {
      return serializedByStorage
    }

    const mgr = ed.markdown as { serialize?: (doc: JSONContent | ProseMirrorNode) => string } | undefined
    if (mgr?.serialize) {
      const serializedByManager = mgr.serialize(pmDoc)
      if (serializedByManager.trim().length > 0) return serializedByManager
      const serializedJson = mgr.serialize(docJson)
      if (serializedJson.trim().length > 0) return serializedJson
    }
  } catch {
    /* fall through */
  }
  return ed.state.doc.textBetween(from, to, "\n")
}

/**
 * Replace selection with pasted text interpreted by Markdown pipeline:
 * DOMPurify(text) -> Showdown(makeHtml) -> DOMPurify(html) -> TipTap insert.
 */
function insertPastedTextAsHtml(ed: Editor, clipboardText: string): void {
  const { from, to } = ed.state.selection
  const trimmed = clipboardText.trim()
  if (trimmed.length === 0) return

  const sanitizedText = DOMPurify.sanitize(trimmed, { USE_PROFILES: { html: true } })
  const decodedText = decodeHtmlEntities(sanitizedText)
  const markdownReadyText = applySupSubMarkdownSyntax(decodedText)
  let html = pasteHtmlConverter.makeHtml(markdownReadyText)
  html = DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
  if (!html.trim()) {
    const tr = ed.state.tr.insertText(decodedText, from, to)
    ed.view.dispatch(tr)
    return
  }

  ed.chain().focus().insertContentAt({ from, to }, html).run()
}

/** Clipboard source for paste: always treat clipboard content as text. */
function getClipboardTextForPaste(ev: ClipboardEvent): string {
  const plain = ev.clipboardData?.getData("text/plain")
  if (plain != null && plain.trim() !== "") return plain

  const html = ev.clipboardData?.getData("text/html")
  if (html == null || html.trim() === "") return ""

  try {
    const doc = new DOMParser().parseFromString(html, "text/html")
    return (doc.body?.textContent ?? "").trim()
  } catch {
    return html
      .replace(/<[^>]+>/g, " ")
      .replace(/&nbsp;/g, " ")
      .replace(/\s+/g, " ")
      .trim()
  }
}

/**
 * Default copy = Markdown on clipboard; default paste = always consume clipboard as text,
 * sanitize text via DOMPurify, normalize with Showdown, then insert sanitized HTML.
 */
function wireDefaultMarkdownClipboard(ed: Editor): void {
  const dom = ed.view.dom

  const onCopy = (event: Event) => {
    const ev = event as ClipboardEvent
    if (!ed.isEditable) return
    const { from, to } = ed.state.selection
    if (from === to) return
    const md = getSelectionMarkdownFromEditor(ed)
    if (md.length === 0) return
    ev.preventDefault()
    ev.clipboardData?.setData("text/plain", md)
    ev.clipboardData?.setData("text/markdown", md)
  }

  dom.addEventListener("copy", onCopy)

  const handlePasteWithMarkdownPipeline = (ev: ClipboardEvent): boolean => {
    if (!ed.isEditable) return false
    const text = getClipboardTextForPaste(ev)
    if (text.trim() === "") return false
    ev.preventDefault()
    insertPastedTextAsHtml(ed, text)
    return true
  }

  /**
   * Prefer ProseMirror `handlePaste` (TipTap-supported override point). Keep `handleDOMEvents.paste`
   * as a fallback for environments where only DOM paste events fire.
   */
  const prevProps = ed.options.editorProps
  const inheritedHandlePaste = prevProps.handlePaste
  const inheritedDomPaste = prevProps.handleDOMEvents?.paste
  ed.setOptions({
    editorProps: {
      ...prevProps,
      handlePaste: (view, event, slice) => {
        const ev = event as ClipboardEvent
        if (handlePasteWithMarkdownPipeline(ev)) return true
        return inheritedHandlePaste?.(view, event, slice) ?? false
      },
      handleDOMEvents: {
        ...prevProps.handleDOMEvents,
        paste: (view, event) => {
          const ev = event as ClipboardEvent
          if (handlePasteWithMarkdownPipeline(ev)) return true
          return inheritedDomPaste?.(view, event) ?? false
        },
      },
    },
  })
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

  applyReaderThemeCssVars(readerPreferences.theme)
  applyReaderTypographyCssVars(readerPreferences)
}

export function initEditorBridge(editor: Editor): void {
  editorInstance = editor
  editorShellLoadNotified = false
  clearTocPublishTimer()
  resetPublishedToc()
  setNoteEditorAutosaveIdleEnabled(false)
  resetPublishedEditorHostState()
  editor.on("selectionUpdate", () => {
    captureStoredCursorFromEditor()
    publishCurrentEditorState()
  })
  editor.on("transaction", () => {
    publishCurrentEditorState()
    scheduleNoteAutosaveAfterEditorActivity()
    scheduleHeadingTocPublish()
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
    getSelectedText: () => {
      const ed = editorInstance
      if (!ed) return ""
      const { from, to } = ed.state.selection
      if (from === to) return ""
      return ed.state.doc.textBetween(from, to, "\n").trim()
    },
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
      try {
        setNoteEditorAutosaveIdleEnabled(false)
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
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("loaded")
        }
        queueMicrotask(() => {
          publishHeadingTocFromEditor()
          if (document.body?.dataset.yabaPage === "editor") {
            setNoteEditorAutosaveIdleEnabled(true)
          }
        })
      } catch {
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("error")
        }
      }
    },
    setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => {
      try {
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
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("loaded")
        }
        queueMicrotask(() => {
          publishHeadingTocFromEditor()
        })
      } catch {
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("error")
        }
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
            ed.commands.insertYabaInlineLink({ text: cmd.text, url: cmd.url })
            break
          case "updateLink":
            ed.commands.updateYabaInlineLinkAt({
              text: cmd.text,
              url: cmd.url,
              pos: cmd.pos,
            })
            break
          case "removeLink":
            ed.commands.removeYabaInlineLinkAt({ pos: cmd.pos })
            break
          case "insertMention":
            ed.commands.insertYabaInlineMention({
              text: cmd.text,
              bookmarkId: cmd.bookmarkId,
              bookmarkKindCode: cmd.bookmarkKindCode,
              bookmarkLabel: cmd.bookmarkLabel,
            })
            break
          case "updateMention":
            ed.commands.updateYabaInlineMentionAt({
              text: cmd.text,
              bookmarkId: cmd.bookmarkId,
              bookmarkKindCode: cmd.bookmarkKindCode,
              bookmarkLabel: cmd.bookmarkLabel,
              pos: cmd.pos,
            })
            break
          case "removeMention":
            ed.commands.removeYabaInlineMentionAt({ pos: cmd.pos })
            break
          case "insertInlineMath":
            insertInlineMathKeepingCaretAfterNode(ed, cmd.latex)
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
    navigateToTocItem: (id: string, _extrasJson?: string | null) => {
      const ed = editorInstance
      if (!ed) return
      const nodePos = findHeadingDocPosForTocItemId(ed, id)
      if (nodePos == null) return
      try {
        const docSize = ed.state.doc.content.size
        const headingEl = findHeadingElementForTocItemId(ed, id)
        queueMicrotask(() => {
          if (headingEl) {
            scrollHeadingIntoEditorView(ed, headingEl)
          }
        })
        const inside = Math.min(nodePos + 1, docSize)
        const sel = TextSelection.create(ed.state.doc, inside)
        ed.view.dispatch(ed.state.tr.setSelection(sel))
      } catch {
        /* ignore */
      }
    },
    exportMarkdownBundleJson: async () => {
      const ed = editorInstance
      if (!ed) return JSON.stringify({ markdown: "", assets: [] })
      const bundle = await exportMarkdownBundleFromEditor(ed)
      return JSON.stringify(bundle)
    },
    exportPdfBase64: async () => {
      const ed = editorInstance
      if (!ed) return ""
      return exportPdfBase64FromEditor(ed)
    },
  }

  wireDefaultMarkdownClipboard(editor)
  applyReaderPreferences()
}
