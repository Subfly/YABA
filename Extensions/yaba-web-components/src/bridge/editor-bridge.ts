import type { Crepe } from "@milkdown/crepe"
import { editorViewCtx } from "@milkdown/core"
import type { EditorView } from "@milkdown/prose/view"
import { Selection, TextSelection } from "@milkdown/prose/state"
import DOMPurify from "dompurify"
import showdown from "showdown"
import { replaceAll, replaceRange } from "@milkdown/utils"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme, parseUrlParams } from "@/theme"
import {
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
} from "@/theme/reader-document-vars"
import { getSelectionSnapshot, type SelectionSnapshot } from "./selection-extractor"
import {
  selectionOverlapsAnnotationLink,
  setStoredAnnotations,
  stripAnnotationLinksForId,
  syncAnnotationAnchorDom,
  type AnnotationForRendering,
} from "@/milkdown/annotation-links"
import { setNoteEditorPlaceholderText } from "@/milkdown/note-placeholder"
import {
  rewriteAssetPathsInMarkdown,
  rewriteAssetPathsInReaderHtml,
  normalizeMarkdownAssetPathsForPersistence,
  collectUsedInlineAssetSrcsFromMarkdown,
} from "@/milkdown/markdown-assets"
import { runEditorDispatch, applyAnnotationLinkToSelection } from "@/milkdown/milkdown-dispatch"
import type { EditorCommandPayload } from "./editor-command-payload"
export type { EditorCommandPayload } from "./editor-command-payload"
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
import { exportMarkdownFromCrepe, startEditorPdfExportJob } from "./editor-export"
import { getActiveFormattingJson } from "@/milkdown/milkdown-formatting"
import { postToYabaNativeHost } from "./yaba-native-host"
import { ANNOTATION_HREF_PREFIX } from "@/milkdown/yaba-href"

export type ReaderTheme = "system" | "dark" | "light" | "sepia"
export type ReaderFontSize = "small" | "medium" | "large"
export type ReaderLineHeight = "normal" | "relaxed"

export interface ReaderPreferences {
  theme: ReaderTheme
  fontSize: ReaderFontSize
  lineHeight: ReaderLineHeight
}

const EMPTY_MARKDOWN = ""

/** Set when [setDocumentJson] / [setReaderHtml] runs with options; used to resolve inline image src and normalize saves. */
let lastAssetsBaseUrl: string | undefined

let crepeInstance: Crepe | null = null

function getBridgeView(): EditorView | null {
  const c = crepeInstance
  if (!c) return null
  try {
    return c.editor.action((ctx) => ctx.get(editorViewCtx))
  } catch {
    return null
  }
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

let editorShellLoadNotified = false
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

function buildHeadingTocJson(view: EditorView): TocJson | null {
  const doc = view.state.doc
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

function findHeadingDocPosForTocItemId(view: EditorView, tocItemId: string): number | null {
  const m = /^toc-h-(\d+)$/.exec(tocItemId)
  if (!m) return null
  const targetIndex = parseInt(m[1], 10)
  if (targetIndex < 0 || !Number.isFinite(targetIndex)) return null
  const headingStarts: number[] = []
  view.state.doc.descendants((node, pos) => {
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

function findHeadingElementForTocItemId(view: EditorView, tocItemId: string): HTMLElement | null {
  const headingIndex = getHeadingIndexFromTocItemId(tocItemId)
  if (headingIndex == null) return null
  const headingElements = Array.from(
    view.dom.querySelectorAll<HTMLElement>("h1, h2, h3, h4, h5, h6")
  ).filter((el) => el.textContent?.trim().length)
  return headingElements[headingIndex] ?? null
}

function scrollHeadingIntoEditorView(view: EditorView, headingEl: HTMLElement): void {
  const container =
    (view.dom.closest("[data-yaba-editor-root]") as HTMLElement | null) ??
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
  const view = getBridgeView()
  if (!view) {
    publishToc(null)
    return
  }
  const toc = buildHeadingTocJson(view)
  publishToc(toc)
}

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
  const view = getBridgeView()
  if (!view) return
  const { anchor, head } = view.state.selection
  lastStoredCursor = { anchor, head }
}

function clearSelectionToCaretAtStart(view: EditorView): void {
  const pos = Selection.atStart(view.state.doc).from
  const selection = TextSelection.create(view.state.doc, pos)
  const tr = view.state.tr.setSelection(selection)
  view.dispatch(tr)
}

function applyInitialFocusStateAfterContent(crepe: Crepe): void {
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    clearSelectionToCaretAtStart(view)
    const { anchor, head } = view.state.selection
    lastStoredCursor = { anchor, head }
    view.dom.blur()
  })
  const container =
    (document.querySelector("[data-yaba-editor-root]") as HTMLElement | null) ??
    (document.querySelector(".yaba-editor-container") as HTMLElement | null)
  container?.scrollTo({ top: 0, behavior: "auto" })
}

function getCanCreateAnnotationForCurrentSelection(): boolean {
  const view = getBridgeView()
  const snap = getSelectionSnapshot(view)
  if (!snap || !view) return false
  const { from, to } = view.state.selection
  if (from === to) return false
  const page = document.body?.dataset.yabaPage
  if (page === "editor") return true
  return !selectionOverlapsAnnotationLink(view.state.doc, from, to)
}

function publishCurrentEditorState(): void {
  publishEditorHostState(getBridgeView(), getCanCreateAnnotationForCurrentSelection)
}

function focusEditorRestoringCursor(): void {
  const crepe = crepeInstance
  if (!crepe) return
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    try {
      if (lastStoredCursor) {
        const size = view.state.doc.content.size
        const from = Math.max(0, Math.min(lastStoredCursor.anchor, size))
        const to = Math.max(0, Math.min(lastStoredCursor.head, size))
        const tr = view.state.tr.setSelection(TextSelection.create(view.state.doc, from, to))
        view.dispatch(tr)
      }
    } catch {
      /* ignore */
    }
    view.focus()
  })
}

function withPreparedEditorSelection(run: (editor: Crepe) => void): void {
  const c = crepeInstance
  if (!c) return
  focusEditorRestoringCursor()
  run(c)
  captureStoredCursorFromEditor()
  publishCurrentEditorState()
}

const pasteHtmlConverter = new showdown.Converter({
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
})

function decodeHtmlEntities(value: string): string {
  const textarea = document.createElement("textarea")
  textarea.innerHTML = value
  return textarea.value
}

function applySupSubMarkdownSyntax(value: string): string {
  const withSup = value.replace(/(^|[^\^])\^([^^\n]+)\^/g, "$1<sup>$2</sup>")
  return withSup.replace(/(^|[^~])~([^~\n]+)~(?!~)/g, "$1<sub>$2</sub>")
}

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

function insertPastedTextAsMarkdown(crepe: Crepe, clipboardText: string): void {
  const trimmed = clipboardText.trim()
  if (trimmed.length === 0) return

  const sanitizedText = DOMPurify.sanitize(trimmed, { USE_PROFILES: { html: true } })
  const decodedText = decodeHtmlEntities(sanitizedText)
  const markdownReadyText = applySupSubMarkdownSyntax(decodedText)
  let html = pasteHtmlConverter.makeHtml(markdownReadyText)
  html = DOMPurify.sanitize(html, { USE_PROFILES: { html: true } })
  if (!html.trim()) {
    crepe.editor.action((ctx) => {
      const view = ctx.get(editorViewCtx)
      const { from, to } = view.state.selection
      view.dispatch(view.state.tr.insertText(decodedText, from, to))
    })
    return
  }

  const md = pasteHtmlConverter.makeMarkdown(html).trim()
  crepe.editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    const { from, to } = view.state.selection
    replaceRange(md || decodedText, { from, to })(ctx)
  })
}

function getEditorDom(crepe: Crepe): HTMLElement | null {
  try {
    return crepe.editor.action((ctx) => ctx.get(editorViewCtx).dom as HTMLElement)
  } catch {
    return null
  }
}

function wireDefaultMarkdownClipboard(crepe: Crepe): void {
  const dom = getEditorDom(crepe)
  if (!dom) return

  const onCopy = (event: Event) => {
    const ev = event as ClipboardEvent
    const view = getBridgeView()
    if (!view || !view.editable) return
    const { from, to } = view.state.selection
    if (from === to) return
    const md = view.state.doc.textBetween(from, to, "\n").trim()
    if (md.length === 0) return
    ev.preventDefault()
    ev.clipboardData?.setData("text/plain", md)
    ev.clipboardData?.setData("text/markdown", md)
  }

  const handlePasteWithMarkdownPipeline = (ev: ClipboardEvent): boolean => {
    const view = getBridgeView()
    if (!view?.editable) return false
    const text = getClipboardTextForPaste(ev)
    if (text.trim() === "") return false
    ev.preventDefault()
    insertPastedTextAsMarkdown(crepe, text)
    return true
  }

  dom.addEventListener("copy", onCopy)
  dom.addEventListener(
    "paste",
    (ev: ClipboardEvent) => {
      if (handlePasteWithMarkdownPipeline(ev)) {
        ev.stopPropagation()
      }
    },
    true
  )
}

function wireNativeContentTaps(crepe: Crepe): void {
  const root = getEditorDom(crepe)
  if (!root) return

  const onClick = (ev: MouseEvent) => {
    const target = ev.target as HTMLElement | null
    if (!target) return

    const ann = target.closest<HTMLAnchorElement>('a[href^="yaba-annotation:"]')
    if (ann) {
      const href = ann.getAttribute("href") ?? ""
      const id = href.replace(new RegExp(`^${ANNOTATION_HREF_PREFIX.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}`), "").split(/[?#]/)[0]
      postToYabaNativeHost({ type: "annotationTap", id })
      const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
      win.YabaEditorBridge?.onAnnotationTap?.(id)
      ev.preventDefault()
      return
    }

    const bm = target.closest<HTMLAnchorElement>('a[href^="bookmark:"]')
    if (bm) {
      const href = bm.getAttribute("href") ?? ""
      const bookmarkId = href.replace(/^bookmark:/, "")
      const text = bm.textContent ?? ""
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(bm, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "inlineMentionTap",
        pos,
        text,
        bookmarkId,
        bookmarkKindCode: 0,
        bookmarkLabel: text,
      })
      ev.preventDefault()
      return
    }

    const httpLink = target.closest<HTMLAnchorElement>("a[href]")
    if (httpLink) {
      const href = httpLink.getAttribute("href") ?? ""
      if (href.startsWith("yaba-annotation:") || href.startsWith("bookmark:")) return
      if (!/^https?:\/\//i.test(href) && !href.startsWith("mailto:")) return
      const text = httpLink.textContent ?? ""
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(httpLink, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "inlineLinkTap",
        pos,
        text,
        url: href,
      })
      ev.preventDefault()
      return
    }

    const katexEl = target.closest(".katex") as HTMLElement | null
    if (katexEl) {
      const annEl = katexEl.querySelector("annotation")
      const latex = annEl?.textContent?.trim() ?? ""
      const block = katexEl.closest(".katex-display")
      let pos = 0
      crepe.editor.action((ctx) => {
        const view = ctx.get(editorViewCtx)
        try {
          pos = view.posAtDOM(katexEl, 0)
        } catch {
          pos = 0
        }
      })
      postToYabaNativeHost({
        type: "mathTap",
        kind: block ? "block" : "inline",
        pos,
        latex,
      })
      ev.preventDefault()
    }
  }

  root.addEventListener("click", onClick, true)
}

function applyWebChromeInsetsToDocument(topChromeInsetPx: number): void {
  const root = document.documentElement
  const total = Math.max(0, Math.round(topChromeInsetPx))
  root.style.setProperty("--yaba-web-chrome-status-bar", `${total}px`)
  root.style.setProperty("--yaba-web-chrome-top-bar", "0px")
  root.style.setProperty("--yaba-web-chrome-safe-area-top-additional", "0px")
}

function applyReaderPreferences(): void {
  const page = document.body?.dataset.yabaPage
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

const readerHtmlToMd = new showdown.Converter({
  tables: true,
  tasklists: true,
  strikethrough: true,
  ghCodeBlocks: true,
})

export function initEditorBridge(crepe: Crepe): void {
  crepeInstance = crepe
  editorShellLoadNotified = false
  clearTocPublishTimer()
  resetPublishedToc()
  setNoteEditorAutosaveIdleEnabled(false)
  resetPublishedEditorHostState()

  crepe.on((listener) => {
    listener.markdownUpdated(() => {
      publishCurrentEditorState()
      scheduleNoteAutosaveAfterEditorActivity()
      scheduleHeadingTocPublish()
      syncAnnotationAnchorDom(document.querySelector("[data-yaba-editor-root]"))
    })
    listener.selectionUpdated(() => {
      captureStoredCursorFromEditor()
      publishCurrentEditorState()
    })
    listener.focus(() => {
      publishCurrentEditorState()
    })
    listener.blur(() => {
      publishCurrentEditorState()
    })
  })

  queueMicrotask(() => {
    applyInitialFocusStateAfterContent(crepe)
    publishCurrentEditorState()
  })

  const urlParams = parseUrlParams()
  platform = urlParams.platform
  appearance = urlParams.appearance

  wireDefaultMarkdownClipboard(crepe)
  wireNativeContentTaps(crepe)

  const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
  win.YabaEditorBridge = {
    isReady: () => !!crepeInstance,
    getSelectionSnapshot: () => getSelectionSnapshot(getBridgeView()),
    getSelectedText: () => {
      const view = getBridgeView()
      if (!view) return ""
      const { from, to } = view.state.selection
      if (from === to) return ""
      return view.state.doc.textBetween(from, to, "\n").trim()
    },
    getCanCreateAnnotation: () => getCanCreateAnnotationForCurrentSelection(),
    setAnnotations: (annotationsJson: string) => {
      try {
        const annotations: AnnotationForRendering[] =
          annotationsJson && annotationsJson.trim() ? JSON.parse(annotationsJson) : []
        setStoredAnnotations(annotations)
      } catch {
        setStoredAnnotations([])
      }
      syncAnnotationAnchorDom(document.querySelector("[data-yaba-editor-root]"))
      publishCurrentEditorState()
    },
    scrollToAnnotation: (annotationId: string) => {
      const el = document.querySelector(
        `a[href="${ANNOTATION_HREF_PREFIX}${annotationId}"], a[data-annotation-id="${annotationId}"]`
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
    setWebChromeInsets: (topChromeInsetPx: number) => {
      applyWebChromeInsetsToDocument(topChromeInsetPx)
    },
    setReaderPreferences: (prefs: Partial<ReaderPreferences>) => {
      readerPreferences = {
        ...readerPreferences,
        ...prefs,
      }
      applyReaderPreferences()
    },
    setEditable: (isEditable: boolean) => {
      crepeInstance?.setReadonly(!isEditable)
    },
    setPlaceholder: (placeholder: string) => {
      setNoteEditorPlaceholderText(placeholder)
      publishCurrentEditorState()
    },
    setDocumentJson: (documentJson: string, options?: { assetsBaseUrl?: string }) => {
      const c = crepeInstance
      if (!c) return
      try {
        setNoteEditorAutosaveIdleEnabled(false)
        if (options?.assetsBaseUrl) {
          lastAssetsBaseUrl = options.assetsBaseUrl
        }
        let payload = documentJson?.trim() ? documentJson : EMPTY_MARKDOWN
        if (payload.startsWith('{"type":"doc"')) {
          payload = EMPTY_MARKDOWN
        }
        if (options?.assetsBaseUrl) {
          payload = rewriteAssetPathsInMarkdown(payload, options.assetsBaseUrl)
        }
        c.editor.action(replaceAll(payload, true))
        applyInitialFocusStateAfterContent(c)
        publishCurrentEditorState()
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("loaded")
        }
        queueMicrotask(() => {
          publishHeadingTocFromEditor()
          syncAnnotationAnchorDom(document.querySelector("[data-yaba-editor-root]"))
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
      const c = crepeInstance
      if (!c) return
      try {
        if (options?.assetsBaseUrl) {
          lastAssetsBaseUrl = options.assetsBaseUrl
        }
        let payload = html?.trim() ? html : "<p></p>"
        if (options?.assetsBaseUrl) {
          payload = rewriteAssetPathsInReaderHtml(payload, options.assetsBaseUrl)
        }
        const safe = DOMPurify.sanitize(payload, { USE_PROFILES: { html: true } })
        const md = readerHtmlToMd.makeMarkdown(safe).trim()
        c.editor.action(replaceAll(md || EMPTY_MARKDOWN, true))
        applyInitialFocusStateAfterContent(c)
        publishCurrentEditorState()
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("loaded")
        }
        queueMicrotask(() => {
          publishHeadingTocFromEditor()
          syncAnnotationAnchorDom(document.querySelector("[data-yaba-editor-root]"))
        })
      } catch {
        if (!editorShellLoadNotified) {
          editorShellLoadNotified = true
          publishShellLoad("error")
        }
      }
    },
    getDocumentJson: () => {
      const raw = crepeInstance?.getMarkdown() ?? ""
      return normalizeMarkdownAssetPathsForPersistence(raw, lastAssetsBaseUrl)
    },
    getUsedInlineAssetSrcs: () => {
      const raw = normalizeMarkdownAssetPathsForPersistence(crepeInstance?.getMarkdown() ?? "", lastAssetsBaseUrl)
      const list = collectUsedInlineAssetSrcsFromMarkdown(raw, lastAssetsBaseUrl)
      return JSON.stringify(list)
    },
    getActiveFormatting: () => getActiveFormattingJson(getBridgeView()),
    focus: () => {
      focusEditorRestoringCursor()
      publishCurrentEditorState()
    },
    unFocus: () => {
      captureStoredCursorFromEditor()
      crepeInstance?.editor.action((ctx) => {
        ctx.get(editorViewCtx).dom.blur()
      })
      publishCurrentEditorState()
    },
    dispatch: (cmd: EditorCommandPayload) => {
      withPreparedEditorSelection((c) => {
        const page = document.body?.dataset.yabaPage
        runEditorDispatch(c.editor, cmd, lastAssetsBaseUrl, page)
      })
    },
    applyAnnotationToSelection: (annotationId: string) => {
      const c = crepeInstance
      if (!c) return false
      return applyAnnotationLinkToSelection(c.editor, annotationId)
    },
    removeAnnotationFromDocument: (annotationId: string) => {
      const c = crepeInstance
      if (!c) return 0
      const before = c.getMarkdown()
      const after = stripAnnotationLinksForId(before, annotationId)
      if (after === before) return 0
      c.editor.action(replaceAll(after, true))
      return 1
    },
    navigateToTocItem: (id: string, _extrasJson?: string | null) => {
      const view = getBridgeView()
      if (!view) return
      const nodePos = findHeadingDocPosForTocItemId(view, id)
      if (nodePos == null) return
      try {
        const docSize = view.state.doc.content.size
        const headingEl = findHeadingElementForTocItemId(view, id)
        queueMicrotask(() => {
          if (headingEl) {
            scrollHeadingIntoEditorView(view, headingEl)
          }
        })
        const inside = Math.min(nodePos + 1, docSize)
        const sel = TextSelection.create(view.state.doc, inside)
        view.dispatch(view.state.tr.setSelection(sel))
      } catch {
        /* ignore */
      }
    },
    exportMarkdown: () => {
      const c = crepeInstance
      if (!c) return ""
      try {
        return exportMarkdownFromCrepe(c, lastAssetsBaseUrl)
      } catch {
        return ""
      }
    },
    startPdfExportJob: (jobId: string) => {
      const c = crepeInstance
      if (!c || !jobId.trim()) return
      try {
        startEditorPdfExportJob(c, jobId)
      } catch {
        /* handled inside */
      }
    },
  }

  applyReaderPreferences()

  const page = document.body?.dataset.yabaPage
  if (page === "editor") {
    postToYabaNativeHost({ type: "bridgeReady", feature: "editor" })
  } else if (page === "viewer") {
    postToYabaNativeHost({ type: "bridgeReady", feature: "viewer" })
  }
}
