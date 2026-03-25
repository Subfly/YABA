import ePub, { type Book, type Contents, type Location, type Rendition } from "epubjs"
import { type AppearanceMode, applyTheme, type Platform } from "@/theme"

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

function applyEpubTypography(root: HTMLElement, prefs: ReaderPreferencesInput): void {
  const fontSizeMap: Record<string, string> = {
    small: "0.9rem",
    medium: "1.05rem",
    large: "1.2rem",
  }
  const lineHeightMap: Record<string, string> = {
    normal: "1.45",
    relaxed: "1.75",
  }
  const fs = prefs.fontSize ?? "medium"
  const lh = prefs.lineHeight ?? "normal"
  root.style.setProperty("--yaba-reader-font-size", fontSizeMap[fs] ?? fontSizeMap.medium)
  root.style.setProperty("--yaba-reader-line-height", lineHeightMap[lh] ?? lineHeightMap.normal)
}

export function initEpubViewerBridge(platform: Platform, appearance: AppearanceMode): void {
  currentPlatform = platform
  currentAppearance = appearance
  applyTheme(currentPlatform, currentAppearance, null)

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
            const style = doc.createElement("style")
            style.textContent = `
              html, body {
                font-family: "Quicksand", -apple-system, BlinkMacSystemFont, sans-serif !important;
                font-size: var(--yaba-reader-font-size, 1.05rem) !important;
                line-height: var(--yaba-reader-line-height, 1.45) !important;
              }
            `
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
      applyTheme(currentPlatform, currentAppearance, null)
    },
    setAppearance(appearance: AppearanceMode): void {
      currentAppearance = appearance
      applyTheme(currentPlatform, currentAppearance, null)
    },
    setReaderPreferences(prefs: Partial<ReaderPreferencesInput>): void {
      const root = document.getElementById("epub-root")
      if (!root) return
      applyEpubTypography(root, prefs)
    },
  }
  shellReady = true
}
