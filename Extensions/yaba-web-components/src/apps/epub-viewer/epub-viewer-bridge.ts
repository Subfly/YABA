import ePub, { type Book, type Contents, type Location, type Rendition } from "epubjs"
import { type AppearanceMode, applyTheme, type Platform } from "@/theme"
import {
  applyBaseThemeForReaderTheme,
  applyReaderThemeCssVars,
  applyReaderTypographyCssVars,
  type ReaderThemeName,
} from "@/theme/reader-document-vars"
import { getEpubContentOverrideCss } from "./epub-content-styles"

interface EpubHighlightInput {
  id: string
  colorRole: string
  cfiRange: string
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
const annotationCfiById = new Map<string, string>()

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
  const names = [
    "--yaba-reader-font-size",
    "--yaba-reader-line-height",
    "--yaba-reader-bg",
    "--yaba-reader-on-bg",
    "--yaba-font-family",
    "--yaba-primary",
  ]
  iframeRoot.setAttribute("data-yaba-reader-theme", mergedReaderPrefs.theme)
  for (const name of names) {
    const v = getComputedStyle(host).getPropertyValue(name).trim()
    if (v !== "") iframeRoot.style.setProperty(name, v)
  }
}

function getRenditionContentsList(r: Rendition): Contents[] {
  const raw = r.getContents() as unknown
  return Array.isArray(raw) ? (raw as Contents[]) : []
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

function registerSelectionClearOnTapAway(contents: Contents): void {
  let t: ReturnType<typeof setTimeout> | null = null
  const onSel = (): void => {
    if (t) clearTimeout(t)
    t = setTimeout(() => {
      t = null
      const sel = contents.window.getSelection()
      if (!sel || sel.rangeCount === 0 || sel.isCollapsed || !sel.toString().trim()) {
        lastSelection = null
      }
    }, 120)
  }
  contents.document.addEventListener("selectionchange", onSel, { passive: true })
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

  const win = window as Window & { YabaEpubBridge?: YabaEpubBridge }
  win.YabaEpubBridge = {
    isReady: () => shellReady,
    setEpubUrl(url: string): boolean {
      if (!url) return false
      void (async () => {
        try {
          clearHighlights()
          lastSelection = null
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
          if (!root) return
          root.innerHTML = ""

          book = ePub(url)
          await book.ready
          const spineItems = await book.loaded.spine
          spineLength = Math.max(1, spineItems.length)

          rendition = book.renderTo(root, {
            width: "100%",
            height: "100%",
            flow: "paginated",
            allowScriptedContent: false,
          })

          rendition.hooks.content.register((contents: Contents) => {
            const doc = contents.document
            syncReaderVarsFromHostToIframeDoc(doc)
            registerSelectionClearOnTapAway(contents)
            const style = doc.createElement("style")
            style.id = "yaba-epub-content-style"
            style.textContent = getEpubContentOverrideCss()
            doc.head.appendChild(style)
          })

          rendition.on("relocated", (location: Location) => {
            currentPageNum = (location.start.index ?? 0) + 1
          })

          rendition.on("selected", (cfiRange: string, contents: { window: Window }) => {
            const selectedText = contents.window.getSelection()?.toString() ?? ""
            if (cfiRange && selectedText.trim().length > 0) {
              lastSelection = { cfiRange, selectedText: selectedText.trim() }
            }
          })

          await rendition.display()
          currentPageNum = 1
          syncReaderVarsIntoAllEpubContents()
        } catch (e) {
          console.error("EPUB load failed", e)
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
      if (!rendition) return
      clearHighlights()
      let list: EpubHighlightInput[] = []
      try {
        list = annotationsJson.trim().length > 0 ? (JSON.parse(annotationsJson) as EpubHighlightInput[]) : []
      } catch {
        list = []
      }
      for (const h of list) {
        if (!h.cfiRange) continue
        annotationCfiById.set(h.id, h.cfiRange)
        try {
          rendition.annotations.add(
            "highlight",
            h.cfiRange,
            { id: h.id },
            (e: Event) => {
              e.stopPropagation()
              win.YabaEpubBridge?.onAnnotationTap?.(h.id)
            },
          )
        } catch {
          /* ignore invalid cfi */
        }
      }
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
  }
  shellReady = true
}
