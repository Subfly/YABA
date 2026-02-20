import type { Editor } from "@tiptap/core"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme } from "@/theme"

export interface YabaEditorBridge {
  isReady: () => boolean
  setPlatform: (platform: Platform) => void
  setAppearance: (mode: AppearanceMode) => void
  setCursorColor: (color: string) => void
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

export function initEditorBridge(editor: Editor): void {
  editorInstance = editor
  const win = window as Window & { YabaEditorBridge?: YabaEditorBridge }
  win.YabaEditorBridge = {
    isReady: () => !!editorInstance,
    setPlatform: (p: Platform) => {
      platform = p
      applyTheme(platform, appearance, cursorColor)
    },
    setAppearance: (mode: AppearanceMode) => {
      appearance = mode
      applyTheme(platform, appearance, cursorColor)
    },
    setCursorColor: (color: string) => {
      cursorColor = color
      document.documentElement.style.setProperty("--yaba-cursor", color)
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
}
