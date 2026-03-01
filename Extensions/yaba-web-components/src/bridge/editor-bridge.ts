import type { Editor } from "@tiptap/core"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme } from "@/theme"

export type ReaderTheme = "system" | "dark" | "light" | "sepia"
export type ReaderFontSize = "small" | "medium" | "large"
export type ReaderLineHeight = "normal" | "relaxed"

export interface ReaderPreferences {
  theme: ReaderTheme
  fontSize: ReaderFontSize
  lineHeight: ReaderLineHeight
}

export interface YabaEditorBridge {
  isReady: () => boolean
  setPlatform: (platform: Platform) => void
  setAppearance: (mode: AppearanceMode) => void
  setCursorColor: (color: string) => void
  setReaderPreferences: (preferences: Partial<ReaderPreferences>) => void
  setEditable: (isEditable: boolean) => void
  setMarkdown: (markdown: string, options?: { assetsBaseUrl?: string }) => void
  getMarkdown: () => string
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
  | { type: "openExcalidraw" }
  | { type: "insertYouTube"; url: string }
  | { type: "insertInlineMath"; latex: string }
  | { type: "insertBlockMath"; latex: string }

let editorInstance: Editor | null = null
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
  const isViewerPage = document.body?.dataset.yabaPage === "viewer"

  if (!isViewerPage) {
    applyTheme(platform, appearance, cursorColor)
    clearSystemColorSchemeListener()
  } else if (readerPreferences.theme === "system") {
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

  applyReaderThemeVars(readerPreferences.theme)
  applyReaderTypographyVars(readerPreferences)
}

export function initEditorBridge(editor: Editor): void {
  editorInstance = editor
  const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
  win.YabaEditorBridge = {
    isReady: () => !!editorInstance,
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
    setMarkdown: (markdown: string, options?: { assetsBaseUrl?: string }) => {
      let content = markdown
      if (options?.assetsBaseUrl && content.includes("../assets/")) {
        const base = options.assetsBaseUrl.replace(/\/?$/, "/")
        content = content.replace(/\]\(\.\.\/assets\//g, `](${base}assets/`)
      }
      editorInstance?.commands.setContent(content, {
        contentType: "markdown",
        emitUpdate: false,
      })
    },
    getMarkdown: () => {
      return editorInstance?.getMarkdown() ?? ""
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
        case "openExcalidraw":
          ed.commands.insertExcalidraw()
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
