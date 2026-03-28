import ePub, { type Book, type Contents, type Location, type Rendition } from "epubjs"
import { type AppearanceMode, applyTheme, type Platform } from "@/theme"
import {
  applyBaseThemeForReaderTheme,
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
  type ReaderThemeName,
} from "@/theme/reader-document-vars"
import { getEpubContentOverrideCss } from "./epub-content-styles"
import { publishShellLoad } from "@/bridge/shell-host-events"
import { publishToc, resetPublishedToc, type TocItemJson, type TocJson } from "@/bridge/toc-host-events"

interface EpubHighlightInput {
  id: string
  colorRole: string
  cfiRange: string
}

/**
 * epub.js 0.3.x draws highlights as SVG marks; [iframe View#highlight] merges these attrs over defaults
 * (`fill: yellow`, `fill-opacity: 0.3`). Match PDF viewer + TipTap annotation palette (pdf-viewer.css).
 */
const EPUB_HIGHLIGHT_DEFAULT_CLASS = "epubjs-hl"

const colorRoleToEpubHighlightStyles: Record<string, Record<string, string>> = {
  NONE: {
    fill: "rgba(255, 214, 10, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  YELLOW: {
    fill: "rgba(255, 214, 10, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  BLUE: {
    fill: "rgba(88, 166, 255, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  BROWN: {
    fill: "rgba(193, 154, 107, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  CYAN: {
    fill: "rgba(0, 188, 212, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  GRAY: {
    fill: "rgba(158, 158, 158, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  GREEN: {
    fill: "rgba(102, 187, 106, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  INDIGO: {
    fill: "rgba(92, 107, 192, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  MINT: {
    fill: "rgba(38, 166, 154, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  ORANGE: {
    fill: "rgba(255, 167, 38, 0.35)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  PINK: {
    fill: "rgba(236, 64, 122, 0.3)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  PURPLE: {
    fill: "rgba(171, 71, 188, 0.3)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  RED: {
    fill: "rgba(239, 83, 80, 0.3)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
  TEAL: {
    fill: "rgba(38, 166, 154, 0.3)",
    "fill-opacity": "1",
    "mix-blend-mode": "multiply",
  },
}

function epubHighlightStylesForColorRole(colorRole: string | undefined): Record<string, string> {
  const key = (colorRole ?? "YELLOW").toUpperCase()
  return colorRoleToEpubHighlightStyles[key] ?? colorRoleToEpubHighlightStyles.YELLOW
}

interface ReaderPreferencesInput {
  theme?: string
  fontSize?: string
  lineHeight?: string
}

interface YabaEpubBridge {
  isReady: () => boolean
  setEpubUrl: (url: string) => boolean
  getSelectionSnapshot: () => Record<string, unknown> | null
  getCanCreateAnnotation: () => boolean
  setAnnotations: (annotationsJson: string) => void
  scrollToAnnotation: (annotationId: string) => void
  getCurrentPageNumber: () => number
  getPageCount: () => number
  nextPage: () => boolean
  prevPage: () => boolean
  setPlatform: (platform: Platform) => void
  setAppearance: (appearance: AppearanceMode) => void
  setReaderPreferences: (preferences: Partial<ReaderPreferencesInput>) => void
  onAnnotationTap?: (id: string) => void
  navigateToTocItem: (id: string, extrasJson?: string | null) => void
}

interface EpubNavItem {
  href?: string
  label?: string
  subitems?: EpubNavItem[]
}

/** True after [initEpubViewerBridge] installed `window.YabaEpubBridge` (matches PDF viewer — host must see ready before calling setEpubUrl). */
let shellReady = false
let currentPlatform: Platform = "compose"
let currentAppearance: AppearanceMode = "auto"
let book: Book | null = null
let rendition: Rendition | null = null
let lastSelection: { cfiRange: string; selectedText: string } | null = null
let currentPageNum = 1
let spineLength = 1
let lastRelocatedStartCfi: string | null = null
const annotationCfiById = new Map<string, string>()
/** When [setAnnotations] runs before async [setEpubUrl] finishes, host JSON is applied after [publishShellLoad]. */
let pendingAnnotationsJson: string | null = null
let viewportRecoveryTimer: ReturnType<typeof setTimeout> | null = null
let viewportStabilityHandlersInstalled = false
let annotationRefreshTimer: ReturnType<typeof setTimeout> | null = null
let lastAnnotationTapMeta: { id: string; atMs: number } | null = null
let lastAppliedAnnotations: EpubHighlightInput[] = []

/** Avoid duplicate selection listeners if content hook runs more than once per document. */
const selectionClearRegisteredDocs = new WeakSet<Document>()
const ANNOTATION_TAP_DEDUP_MS = 450

interface MergedEpubReaderPrefs {
  theme: ReaderThemeName
  fontSize: string
  lineHeight: string
}

let mergedReaderPrefs: MergedEpubReaderPrefs = {
  theme: "system",
  fontSize: "medium",
  lineHeight: "normal",
}

let epubSystemMedia: MediaQueryList | null = null
let epubSystemListener: (() => void) | null = null

function clearEpubSystemColorSchemeListener(): void {
  if (!epubSystemMedia || !epubSystemListener) return
  epubSystemMedia.removeEventListener("change", epubSystemListener)
  epubSystemListener = null
  epubSystemMedia = null
}

function ensureEpubSystemColorSchemeListener(): void {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return
  if (mergedReaderPrefs.theme !== "system") return
  if (epubSystemListener) return

  epubSystemMedia = window.matchMedia("(prefers-color-scheme: dark)")
  const onChange = () => {
    if (mergedReaderPrefs.theme !== "system") return
    applyTheme(currentPlatform, currentAppearance, null)
    applyReaderThemeCssVars("system")
    applyReaderTypographyCssVars(mergedReaderPrefs)
    syncReaderVarsIntoAllEpubContents()
  }
  epubSystemMedia.addEventListener("change", onChange)
  epubSystemListener = onChange
}

function normalizeReaderTheme(t: string | undefined): ReaderThemeName {
  if (t === "dark" || t === "light" || t === "sepia" || t === "system") return t
  return "system"
}

/** Applies shell palette + reader CSS vars on the host document (iframe content synced separately). */
function applyEpubReaderPipeline(): void {
  const theme = mergedReaderPrefs.theme
  applyBaseThemeForReaderTheme(currentPlatform, currentAppearance, theme, null)
  if (theme === "system") {
    if (currentAppearance === "auto") ensureEpubSystemColorSchemeListener()
    else clearEpubSystemColorSchemeListener()
  } else {
    clearEpubSystemColorSchemeListener()
  }
  applyReaderThemeCssVars(theme)
  applyReaderTypographyCssVars(mergedReaderPrefs)
  const root = document.getElementById("epub-root")
  if (root) {
    root.style.background = "var(--yaba-reader-bg, transparent)"
  }
  syncReaderVarsIntoAllEpubContents()
}

function syncReaderVarsFromHostToIframeDoc(doc: Document): void {
  const host = document.documentElement
  const iframeRoot = doc.documentElement
  const cs = getComputedStyle(host)

  iframeRoot.setAttribute("data-yaba-reader-theme", mergedReaderPrefs.theme)

  const shared = [
    "--yaba-reader-font-size",
    "--yaba-reader-line-height",
    "--yaba-font-family",
    "--yaba-primary",
  ] as const
  for (const name of shared) {
    const v = cs.getPropertyValue(name).trim()
    if (v !== "") iframeRoot.style.setProperty(name, v)
  }

  if (mergedReaderPrefs.theme === "system") {
    /**
     * Read-it-later [viewer.html] is one document: --yaba-reader-bg stays transparent and Compose shows through.
     * EPUB spine documents live in iframes; transparency does not reveal the host surface, so the canvas stays
     * effectively light and resolved --yaba-on-bg (e.g. dark mode) reads as pale text on white. Map system theme
     * to the same resolved shell palette as [applyTheme] (--yaba-bg / --yaba-on-bg) for correct contrast.
     */
    const shellBg = cs.getPropertyValue("--yaba-bg").trim()
    const shellOnBg = cs.getPropertyValue("--yaba-on-bg").trim()
    if (shellBg !== "") iframeRoot.style.setProperty("--yaba-reader-bg", shellBg)
    if (shellOnBg !== "") iframeRoot.style.setProperty("--yaba-reader-on-bg", shellOnBg)
  } else {
    const rb = cs.getPropertyValue("--yaba-reader-bg").trim()
    const rob = cs.getPropertyValue("--yaba-reader-on-bg").trim()
    if (rb !== "") iframeRoot.style.setProperty("--yaba-reader-bg", rb)
    if (rob !== "") iframeRoot.style.setProperty("--yaba-reader-on-bg", rob)
  }
}

function getRenditionContentsList(r: Rendition): Contents[] {
  const raw = r.getContents() as unknown
  return Array.isArray(raw) ? (raw as Contents[]) : []
}

function mapEpubNavToToc(items: EpubNavItem[], depth: number, path: string): TocItemJson[] {
  if (!Array.isArray(items) || items.length === 0) return []
  return items.map((item, i) => {
    const href = (item.href ?? "").trim()
    const id = `${path}-${i}-${href || "nohref"}`
    const title = (item.label ?? "").trim() || "Untitled"
    const extrasJson = href.length > 0 ? JSON.stringify({ href }) : null
    const children = mapEpubNavToToc(item.subitems ?? [], depth + 1, id)
    return {
      id,
      title,
      level: depth,
      children,
      extrasJson,
    }
  })
}

async function buildAndPublishEpubToc(b: Book): Promise<void> {
  try {
    resetPublishedToc()
    const navigation = await b.loaded.navigation
    const raw = navigation?.toc as EpubNavItem[] | undefined
    if (!raw || raw.length === 0) {
      publishToc({ items: [] })
      return
    }
    const items = mapEpubNavToToc(raw, 1, "epub")
    publishToc({ items } satisfies TocJson)
  } catch {
    publishToc({ items: [] })
  }
}

function syncReaderVarsIntoAllEpubContents(): void {
  const r = rendition
  if (!r) return
  for (const contents of getRenditionContentsList(r)) {
    try {
      syncReaderVarsFromHostToIframeDoc(contents.document)
    } catch {
      /* ignore */
    }
  }
}

function clearHighlights(): void {
  const r = rendition
  if (r) {
    for (const cfi of annotationCfiById.values()) {
      try {
        r.annotations.remove(cfi, "highlight")
      } catch {
        /* ignore */
      }
    }
  }
  annotationCfiById.clear()
}

function clearViewportRecoveryTimer(): void {
  if (viewportRecoveryTimer) {
    clearTimeout(viewportRecoveryTimer)
    viewportRecoveryTimer = null
  }
}

function clearAnnotationRefreshTimer(): void {
  if (annotationRefreshTimer) {
    clearTimeout(annotationRefreshTimer)
    annotationRefreshTimer = null
  }
}

function shouldDispatchAnnotationTap(annotationId: string): boolean {
  const now = typeof performance !== "undefined" ? performance.now() : Date.now()
  const last = lastAnnotationTapMeta
  if (last && last.id === annotationId && now - last.atMs < ANNOTATION_TAP_DEDUP_MS) {
    return false
  }
  lastAnnotationTapMeta = {
    id: annotationId,
    atMs: now,
  }
  return true
}

/**
 * Android IME insets can resize the WebView while creation UI is focused.
 * epub.js may relocate to a previous spread after that resize, so we redisplay
 * the last known CFI once the viewport settles.
 */
function scheduleViewportLocationRecovery(): void {
  clearViewportRecoveryTimer()
  viewportRecoveryTimer = setTimeout(() => {
    viewportRecoveryTimer = null
    const cfi = lastRelocatedStartCfi
    const r = rendition
    if (!r || !cfi) return
    void r.display(cfi)
  }, 220)
}

function ensureViewportStabilityHandlers(): void {
  if (viewportStabilityHandlersInstalled || typeof window === "undefined") return
  viewportStabilityHandlersInstalled = true
  const onResize = () => scheduleViewportLocationRecovery()
  window.addEventListener("resize", onResize, { passive: true })
  window.visualViewport?.addEventListener("resize", onResize, { passive: true })
}

/**
 * epub.js debounces selection internally (~250ms) before emitting `selected`.
 * A faster `selectionchange` handler can race and clear [lastSelection] before the bridge
 * receives the CFI, so we only clear after a longer debounce and only when the DOM
 * selection is actually gone (tap away / collapse).
 */
function registerSelectionClearWhenCollapsed(contents: Contents): void {
  const doc = contents.document
  if (selectionClearRegisteredDocs.has(doc)) return
  selectionClearRegisteredDocs.add(doc)

  let t: ReturnType<typeof setTimeout> | null = null
  const onSel = (): void => {
    if (t) clearTimeout(t)
    t = setTimeout(() => {
      t = null
      const sel = contents.window.getSelection()
      if (!sel || sel.rangeCount === 0 || sel.isCollapsed || !sel.toString().trim()) {
        lastSelection = null
      }
    }, 500)
  }
  doc.addEventListener("selectionchange", onSel, { passive: true })
}

export function initEpubViewerBridge(platform: Platform, appearance: AppearanceMode): void {
  currentPlatform = platform
  currentAppearance = appearance
  applyTheme(currentPlatform, currentAppearance, null)
  mergedReaderPrefs = {
    theme: "system",
    fontSize: "medium",
    lineHeight: "normal",
  }
  applyEpubReaderPipeline()
  ensureViewportStabilityHandlers()

  const win = window as Window & { YabaEpubBridge?: YabaEpubBridge }

  function renderHighlights(list: EpubHighlightInput[]): void {
    const r = rendition
    if (!r) return
    clearHighlights()
    for (const h of list) {
      if (!h.cfiRange) continue
      annotationCfiById.set(h.id, h.cfiRange)
      try {
        r.annotations.add(
          "highlight",
          h.cfiRange,
          { id: h.id },
          (e: Event) => {
            e.preventDefault()
            e.stopPropagation()
            if (!shouldDispatchAnnotationTap(h.id)) return
            win.YabaEpubBridge?.onAnnotationTap?.(h.id)
          },
          EPUB_HIGHLIGHT_DEFAULT_CLASS,
          epubHighlightStylesForColorRole(h.colorRole),
        )
      } catch {
        /* ignore invalid cfi */
      }
    }
  }

  function scheduleHighlightRefresh(delayMs = 100): void {
    clearAnnotationRefreshTimer()
    annotationRefreshTimer = setTimeout(() => {
      annotationRefreshTimer = null
      renderHighlights(lastAppliedAnnotations)
    }, delayMs)
  }

  function applyAnnotationsPayload(annotationsJson: string): void {
    let list: EpubHighlightInput[] = []
    try {
      list = annotationsJson.trim().length > 0 ? (JSON.parse(annotationsJson) as EpubHighlightInput[]) : []
    } catch {
      list = []
    }
    lastAppliedAnnotations = list
    renderHighlights(list)
  }

  win.YabaEpubBridge = {
    isReady: () => shellReady,
    setEpubUrl(url: string): boolean {
      if (!url) return false
      void (async () => {
        try {
          resetPublishedToc()
          clearHighlights()
          clearAnnotationRefreshTimer()
          lastAppliedAnnotations = []
          lastSelection = null
          clearViewportRecoveryTimer()
          lastRelocatedStartCfi = null
          lastAnnotationTapMeta = null
          if (rendition) {
            try {
              rendition.destroy()
            } catch {
              /* ignore */
            }
          }
          rendition = null
          if (book) {
            try {
              book.destroy()
            } catch {
              /* ignore */
            }
          }
          book = null

          const root = document.getElementById("epub-root")
          if (!root) {
            pendingAnnotationsJson = null
            publishShellLoad("error")
            return
          }
          root.innerHTML = ""

          book = ePub(url)
          await book.ready
          const spineItems = await book.loaded.spine
          spineLength = Math.max(1, spineItems.length)

          await buildAndPublishEpubToc(book)

          rendition = book.renderTo(root, {
            width: "100%",
            height: "100%",
            flow: "paginated",
            allowScriptedContent: false,
          })

          rendition.hooks.content.register((contents: Contents) => {
            const doc = contents.document
            syncReaderVarsFromHostToIframeDoc(doc)
            registerSelectionClearWhenCollapsed(contents)
            const style = doc.createElement("style")
            style.id = "yaba-epub-content-style"
            style.textContent = getEpubContentOverrideCss()
            doc.head.appendChild(style)
          })

          rendition.on("relocated", (location: Location) => {
            lastSelection = null
            const relocatedCfi = location.start.cfi
            if (typeof relocatedCfi === "string" && relocatedCfi.trim().length > 0) {
              lastRelocatedStartCfi = relocatedCfi
            }
            const relocatedIndex = location.start.index
            if (typeof relocatedIndex === "number" && Number.isFinite(relocatedIndex)) {
              currentPageNum = relocatedIndex + 1
            }
            // Re-apply highlights after pagination settles to keep CFI overlays aligned.
            scheduleHighlightRefresh(16)
          })

          rendition.on("selected", (cfiRange: string, contents: Contents) => {
            const selectedText = contents.window.getSelection()?.toString() ?? ""
            if (cfiRange && selectedText.trim().length > 0) {
              lastSelection = { cfiRange, selectedText: selectedText.trim() }
            }
          })

          rendition.on("rendered", () => {
            scheduleHighlightRefresh(16)
          })

          await rendition.display()
          currentPageNum = 1
          syncReaderVarsIntoAllEpubContents()
          publishShellLoad("loaded")
          if (pendingAnnotationsJson !== null) {
            const pending = pendingAnnotationsJson
            pendingAnnotationsJson = null
            applyAnnotationsPayload(pending)
          }
        } catch (e) {
          console.error("EPUB load failed", e)
          pendingAnnotationsJson = null
          resetPublishedToc()
          publishToc({ items: [] })
          publishShellLoad("error")
        }
      })()
      return true
    },
    getSelectionSnapshot(): Record<string, unknown> | null {
      if (!lastSelection) return null
      return {
        cfiRange: lastSelection.cfiRange,
        selectedText: lastSelection.selectedText,
        prefixText: "",
        suffixText: "",
      }
    },
    getCanCreateAnnotation(): boolean {
      return !!(lastSelection && lastSelection.selectedText.trim().length > 0)
    },
    setAnnotations(annotationsJson: string): void {
      if (!rendition) {
        pendingAnnotationsJson = annotationsJson
        return
      }
      pendingAnnotationsJson = null
      applyAnnotationsPayload(annotationsJson)
    },
    scrollToAnnotation(annotationId: string): void {
      const cfi = annotationCfiById.get(annotationId)
      if (cfi && rendition) {
        void rendition.display(cfi)
      }
    },
    getCurrentPageNumber(): number {
      return currentPageNum
    },
    getPageCount(): number {
      return spineLength
    },
    nextPage(): boolean {
      if (!rendition) return false
      void rendition.next()
      return true
    },
    prevPage(): boolean {
      if (!rendition) return false
      void rendition.prev()
      return true
    },
    setPlatform(platform: Platform): void {
      currentPlatform = platform
      applyEpubReaderPipeline()
    },
    setAppearance(appearance: AppearanceMode): void {
      currentAppearance = appearance
      applyEpubReaderPipeline()
    },
    setReaderPreferences(prefs: Partial<ReaderPreferencesInput>): void {
      mergedReaderPrefs = {
        theme: normalizeReaderTheme(prefs.theme ?? mergedReaderPrefs.theme),
        fontSize: prefs.fontSize ?? mergedReaderPrefs.fontSize,
        lineHeight: prefs.lineHeight ?? mergedReaderPrefs.lineHeight,
      }
      applyEpubReaderPipeline()
    },
    navigateToTocItem(_id: string, extrasJson?: string | null): void {
      const r = rendition
      if (!r) return
      try {
        const raw = extrasJson?.trim()
        if (!raw) return
        const o = JSON.parse(raw) as { href?: string }
        const href = o.href
        if (!href || typeof href !== "string") return
        void r.display(href)
      } catch {
        /* ignore */
      }
    },
  }
  shellReady = true
}
