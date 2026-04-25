import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme, parseUrlParams } from "@/theme"
import {
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
} from "@/theme/reader-document-vars"
import type { EditorCommandPayload } from "./editor-bridge"
import { getEmptyFormattingState } from "./editor-formatting"
import { getDomSelectionSnapshot } from "./dom-selection-snapshot"
import { postToYabaNativeHost } from "./yaba-native-host"
import { publishShellLoad } from "./shell-host-events"
import {
  applyAnnotationColorDecorations,
  domSelectionOverlapsAnnotation,
  unwrapAnnotationMarks,
  wrapSelectionInAnnotation,
} from "./read-it-later-annotations"
import type { AnnotationForRendering } from "./read-it-later-types"
import { resetReadItLaterTocState, scheduleReadItLaterTocPublish } from "./read-it-later-toc"
import type { SelectionSnapshot } from "./selection-snapshot"

export type ReaderTheme = "system" | "dark" | "light" | "sepia"
export type ReaderFontSize = "small" | "medium" | "large"
export type ReaderLineHeight = "normal" | "relaxed"

export interface ReaderPreferences {
  theme: ReaderTheme
  fontSize: ReaderFontSize
  lineHeight: ReaderLineHeight
}

const EMPTY_DOC_JSON = '{"type":"doc","content":[]}'

let contentRoot: HTMLElement | null = null
let shellLoadNotified = false
let platform: Platform = "android"
let appearance: AppearanceMode = "auto"
let cursorColor: string | null = null
let readerPreferences: ReaderPreferences = {
  theme: "system",
  fontSize: "medium",
  lineHeight: "normal",
}
let storedAnnotations: AnnotationForRendering[] = []
let systemColorSchemeMedia: MediaQueryList | null = null
let systemColorSchemeListener: (() => void) | null = null

let lastReaderMetricsJson: string | null = null

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

function applyReaderPreferences(): void {
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
  applyReaderThemeCssVars(readerPreferences.theme)
  applyReaderTypographyCssVars(readerPreferences)
}

function publishReadItLaterMetrics(): void {
  const can = getCanCreateAnnotationInner()
  const payload = {
    type: "readerMetrics" as const,
    canCreateAnnotation: can,
    currentPage: 1,
    pageCount: 1,
  }
  const json = JSON.stringify(payload)
  if (json === lastReaderMetricsJson) return
  lastReaderMetricsJson = json
  postToYabaNativeHost(payload)
}

function rewriteAssetPathsInReaderHtml(html: string, assetsBaseUrl: string): string {
  if (!html.includes("../assets/")) return html
  const base = assetsBaseUrl.replace(/\/?$/, "/")
  return html.replaceAll("../assets/", `${base}assets/`)
}

/** Block network and data-URL images so the WebView does not fetch remote content. */
function applyLocalImagePolicy(root: HTMLElement): void {
  root.querySelectorAll("img").forEach((img) => {
    const src = img.getAttribute("src")?.trim() ?? ""
    if (!src) return
    if (src.startsWith("http://") || src.startsWith("https://")) {
      img.removeAttribute("src")
      img.setAttribute("data-yaba-blocked", "remote")
    } else if (src.startsWith("data:")) {
      img.removeAttribute("src")
      img.setAttribute("data-yaba-blocked", "data-url")
    }
  })
}

function getCanCreateAnnotationInner(): boolean {
  if (!contentRoot) return false
  const sel = window.getSelection()
  if (!sel || sel.rangeCount === 0) return false
  const range = sel.getRangeAt(0)
  if (range.collapsed) return false
  if (!contentRoot.contains(range.commonAncestorContainer)) return false
  if (domSelectionOverlapsAnnotation(contentRoot, range)) return false
  return true
}

function applyHtmlToContent(html: string, options?: { assetsBaseUrl?: string }): void {
  if (!contentRoot) return
  const root = contentRoot
  let payload = html?.trim() ? html : "<p></p>"
  if (options?.assetsBaseUrl) {
    payload = rewriteAssetPathsInReaderHtml(payload, options.assetsBaseUrl)
  }
  root.innerHTML = payload
  applyLocalImagePolicy(root)
  if (storedAnnotations.length > 0) {
    applyAnnotationColorDecorations(root, storedAnnotations)
  }
}

function wireImageErrorHandlers(root: HTMLElement): void {
  const onErr = (ev: Event) => {
    const img = ev.target as HTMLImageElement
    if (img.tagName !== "IMG") return
    if (img.dataset.yabaRilError === "1") return
    img.dataset.yabaRilError = "1"
    img.removeAttribute("src")
  }
  root.addEventListener("error", onErr, true)
}

function onAnnotationClick(ev: MouseEvent): void {
  const t = (ev.target as HTMLElement | null)?.closest?.(
    ".yaba-annotation-decoration[data-annotation-id]",
  ) as HTMLElement | null
  if (!t) return
  const id = t.getAttribute("data-annotation-id")
  if (!id) return
  ev.preventDefault()
  const win = window as Window & {
    YabaReadItLaterBridge?: YabaReadItLaterBridge
  }
  win.YabaReadItLaterBridge?.onAnnotationTap?.(id)
}

export interface YabaReadItLaterBridge {
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
  setDocumentJson: (documentJson: string) => void
  setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => void
  setHtml: (html: string, options?: { assetsBaseUrl?: string }) => void
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

function applyWebChromeInsetsToDocument(topChromeInsetPx: number): void {
  const r = document.documentElement
  const total = Math.max(0, Math.round(topChromeInsetPx))
  r.style.setProperty("--yaba-web-chrome-status-bar", `${total}px`)
  r.style.setProperty("--yaba-web-chrome-top-bar", "0px")
  r.style.setProperty("--yaba-web-chrome-safe-area-top-additional", "0px")
}

/**
 * Init after the content container is in the document (see read-it-later main).
 */
export function initReadItLaterBridge(getRoot: () => HTMLElement | null): void {
  contentRoot = getRoot()
  if (!contentRoot) {
    // eslint-disable-next-line no-console
    console.error("[YABA read-it-later] missing #yaba-read-it-later-content")
  }
  const params = parseUrlParams()
  platform = params.platform
  appearance = params.appearance
  cursorColor = params.cursorColor
  try {
    applyTheme(platform, appearance, cursorColor)
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error("[YABA read-it-later] theme", e)
  }
  applyReaderPreferences()
  if (contentRoot) {
    wireImageErrorHandlers(contentRoot)
    contentRoot.addEventListener("click", onAnnotationClick)
    document.addEventListener("selectionchange", () => {
      publishReadItLaterMetrics()
    })
  }

  const commitReaderHtml = (html: string, options?: { assetsBaseUrl?: string }): void => {
    if (!contentRoot) return
    try {
      resetReadItLaterTocState()
      applyHtmlToContent(html, options)
      if (!shellLoadNotified) {
        shellLoadNotified = true
        publishShellLoad("loaded")
      }
      scheduleReadItLaterTocPublish(contentRoot)
      publishReadItLaterMetrics()
    } catch {
      if (!shellLoadNotified) {
        shellLoadNotified = true
        publishShellLoad("error")
      }
    }
  }

  const win = window as Window & { YabaReadItLaterBridge?: YabaReadItLaterBridge }
  win.YabaReadItLaterBridge = {
    isReady: () => !!contentRoot,
    getSelectionSnapshot: () => (contentRoot ? getDomSelectionSnapshot(contentRoot) : null),
    getSelectedText: () => {
      const s = getDomSelectionSnapshot(contentRoot)
      return s?.selectedText ?? ""
    },
    getCanCreateAnnotation: () => getCanCreateAnnotationInner(),
    setAnnotations: (annotationsJson: string) => {
      if (!contentRoot) return
      try {
        const annotations: AnnotationForRendering[] =
          annotationsJson && annotationsJson.trim() ? JSON.parse(annotationsJson) : []
        storedAnnotations = annotations
        applyAnnotationColorDecorations(contentRoot, storedAnnotations)
      } catch {
        storedAnnotations = []
      }
    },
    scrollToAnnotation: (annotationId: string) => {
      if (!contentRoot) return
      const el = contentRoot.querySelector(
        `.yaba-annotation-decoration[data-annotation-id="${cssEscapeForSelector(annotationId)}"]`,
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
      readerPreferences = { ...readerPreferences, ...prefs }
      applyReaderPreferences()
    },
    setEditable: () => {
      /* read-only surface */
    },
    setPlaceholder: () => {
      /* no inline placeholder on passive HTML */
    },
    setDocumentJson: (documentJson: string) => {
      if (!contentRoot) return
      void documentJson
      contentRoot.innerHTML = "<p></p>"
      if (!shellLoadNotified) {
        shellLoadNotified = true
        publishShellLoad("loaded")
      }
      scheduleReadItLaterTocPublish(contentRoot)
    },
    setReaderHtml: (html: string, options?: { assetsBaseUrl?: string }) => {
      commitReaderHtml(html, options)
    },
    setHtml: (html: string, options?: { assetsBaseUrl?: string }) => {
      commitReaderHtml(html, options)
    },
    getDocumentJson: () => EMPTY_DOC_JSON,
    getUsedInlineAssetSrcs: () => "[]",
    getActiveFormatting: () => {
      return JSON.stringify(getEmptyFormattingState())
    },
    focus: () => {
      contentRoot?.focus()
    },
    unFocus: () => {
      contentRoot?.blur()
    },
    dispatch: () => {
      /* no rich-text commands on passive reader */
    },
    applyAnnotationToSelection: (annotationId: string) => {
      if (!contentRoot) return false
      const sel = window.getSelection()
      if (!sel || sel.rangeCount === 0) return false
      const range = sel.getRangeAt(0)
      const ok = wrapSelectionInAnnotation(range, annotationId, contentRoot)
      if (ok) {
        applyAnnotationColorDecorations(contentRoot, storedAnnotations)
        publishReadItLaterMetrics()
        scheduleReadItLaterTocPublish(contentRoot)
      }
      sel.removeAllRanges()
      return ok
    },
    removeAnnotationFromDocument: (annotationId: string) => {
      if (!contentRoot) return 0
      const n = unwrapAnnotationMarks(contentRoot, annotationId)
      if (n > 0) {
        scheduleReadItLaterTocPublish(contentRoot)
        publishReadItLaterMetrics()
      }
      return n
    },
    navigateToTocItem: (tocItemId: string) => {
      if (!contentRoot) return
      const m = /^toc-h-(\d+)$/.exec(tocItemId)
      if (!m) return
      const index = parseInt(m[1], 10)
      if (!Number.isFinite(index) || index < 0) return
      const headingEls = Array.from(
        contentRoot.querySelectorAll<HTMLElement>("h1, h2, h3, h4, h5, h6"),
      ).filter((el) => (el.textContent || "").trim().length > 0)
      const he = headingEls[index] ?? null
      he?.scrollIntoView({ behavior: "smooth", block: "center" })
    },
    exportMarkdown: () => "",
    startPdfExportJob: () => {
      /* native handles exports later */
    },
  }
  postToYabaNativeHost({ type: "bridgeReady", feature: "read-it-later" })
  if (contentRoot) {
    publishReadItLaterMetrics()
  }
}

function cssEscapeForSelector(value: string): string {
  if (typeof CSS !== "undefined" && typeof CSS.escape === "function") {
    return CSS.escape(value)
  }
  return value.replace(/["\\]/g, "\\$&")
}
