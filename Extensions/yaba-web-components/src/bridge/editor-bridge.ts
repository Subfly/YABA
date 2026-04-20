import type { Crepe } from "@milkdown/crepe"
import { editorViewCtx } from "@milkdown/core"
import { getSelectionSnapshot } from "./selection-extractor"
import DOMPurify from "dompurify"
import { replaceAll } from "@milkdown/utils"
import { parseUrlParams } from "@/theme"
import {
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
import { resetPublishedEditorHostState } from "./editor-host-events"
import {
  publishShellLoad,
  scheduleNoteAutosaveAfterEditorActivity,
  setNoteEditorAutosaveIdleEnabled,
} from "./shell-host-events"
import { resetPublishedToc } from "./toc-host-events"
import { exportMarkdownFromCrepe, startEditorPdfExportJob } from "./editor-export"
import { getActiveFormattingJson } from "@/milkdown/milkdown-formatting"
import { postToYabaNativeHost } from "./yaba-native-host"
import { ANNOTATION_HREF_PREFIX } from "@/milkdown/yaba-href"
import {
  crepeInstance,
  setCrepeInstance,
  getBridgeView,
  setLastAssetsBaseUrl,
  getLastAssetsBaseUrl,
} from "./editor-bridge-shared"
import type {
  ReaderTheme,
  ReaderFontSize,
  ReaderLineHeight,
  ReaderPreferences,
  YabaEditorBridge,
} from "./editor-bridge-types"
export type { ReaderTheme, ReaderFontSize, ReaderLineHeight, ReaderPreferences, YabaEditorBridge }
import {
  initAppearanceFromUrl,
  applyReaderPreferences,
  setBridgePlatform,
  setBridgeAppearance,
  setBridgeCursorColor,
  mergeBridgeReaderPreferences,
} from "./editor-bridge-appearance"
import {
  wireDefaultMarkdownClipboard,
  wireNativeContentTaps,
  applyWebChromeInsetsToDocument,
  captureStoredCursorFromEditor,
  applyInitialFocusStateAfterContent,
  publishCurrentEditorState,
  focusEditorRestoringCursor,
  withPreparedEditorSelection,
  getCanCreateAnnotationForCurrentSelection,
} from "./editor-bridge-dom"
import {
  scheduleHeadingTocPublish,
  publishHeadingTocFromEditor,
  resetTocPublishScheduling,
  navigateToTocItemInView,
} from "./editor-bridge-toc"
import { readerHtmlToMarkdown } from "./converter-bridge"

const EMPTY_MARKDOWN = ""

let editorShellLoadNotified = false

export function initEditorBridge(crepe: Crepe): void {
  setCrepeInstance(crepe)
  editorShellLoadNotified = false
  resetTocPublishScheduling()
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
  initAppearanceFromUrl(urlParams.platform, urlParams.appearance)

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
        `a[href="${ANNOTATION_HREF_PREFIX}${annotationId}"], a[data-annotation-id="${annotationId}"]`,
      ) as HTMLElement | null
      el?.scrollIntoView({ behavior: "smooth", block: "center" })
    },
    setPlatform: (p) => {
      setBridgePlatform(p)
      applyReaderPreferences()
    },
    setAppearance: (mode) => {
      setBridgeAppearance(mode)
      applyReaderPreferences()
    },
    setCursorColor: (color: string) => {
      setBridgeCursorColor(color)
      applyReaderPreferences()
    },
    setWebChromeInsets: (topChromeInsetPx: number) => {
      applyWebChromeInsetsToDocument(topChromeInsetPx)
    },
    setReaderPreferences: (prefs: Partial<ReaderPreferences>) => {
      mergeBridgeReaderPreferences(prefs)
      applyReaderPreferences()
    },
    setEditable: (isEditable: boolean) => {
      crepeInstance?.setReadonly(!isEditable)
    },
    setPlaceholder: (placeholder: string) => {
      setNoteEditorPlaceholderText(placeholder)
      publishCurrentEditorState()
    },
    setDocumentJson: (markdown: string, options?: { assetsBaseUrl?: string }) => {
      const c = crepeInstance
      if (!c) return
      try {
        setNoteEditorAutosaveIdleEnabled(false)
        if (options?.assetsBaseUrl) {
          setLastAssetsBaseUrl(options.assetsBaseUrl)
        }
        let payload = markdown?.trim() ? markdown : EMPTY_MARKDOWN
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
          setLastAssetsBaseUrl(options.assetsBaseUrl)
        }
        let payload = html?.trim() ? html : "<p></p>"
        if (options?.assetsBaseUrl) {
          payload = rewriteAssetPathsInReaderHtml(payload, options.assetsBaseUrl)
        }
        const safe = DOMPurify.sanitize(payload, { USE_PROFILES: { html: true } })
        const md = readerHtmlToMarkdown(safe).trim()
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
      return normalizeMarkdownAssetPathsForPersistence(raw, getLastAssetsBaseUrl())
    },
    getUsedInlineAssetSrcs: () => {
      const raw = normalizeMarkdownAssetPathsForPersistence(crepeInstance?.getMarkdown() ?? "", getLastAssetsBaseUrl())
      const list = collectUsedInlineAssetSrcsFromMarkdown(raw, getLastAssetsBaseUrl())
      return JSON.stringify(list)
    },
    getActiveFormatting: () => getActiveFormattingJson(getBridgeView()),
    focus: () => {
      focusEditorRestoringCursor(crepeInstance)
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
      withPreparedEditorSelection(crepeInstance, (c) => {
        const page = document.body?.dataset.yabaPage
        runEditorDispatch(c.editor, cmd, getLastAssetsBaseUrl(), page)
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
    navigateToTocItem: (id: string, extrasJson?: string | null) => {
      const view = getBridgeView()
      if (!view) return
      navigateToTocItemInView(view, id, extrasJson)
    },
    exportMarkdown: () => {
      const c = crepeInstance
      if (!c) return ""
      try {
        return exportMarkdownFromCrepe(c, getLastAssetsBaseUrl())
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