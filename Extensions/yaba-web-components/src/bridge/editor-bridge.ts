import type { LexicalEditor } from "lexical"
import { $getSelection, $isRangeSelection } from "lexical"
import {
  $convertFromMarkdownString,
  $convertToMarkdownString,
  TRANSFORMERS,
} from "@lexical/markdown"
import { INSERT_HORIZONTAL_RULE_COMMAND } from "@lexical/react/LexicalHorizontalRuleNode"
import {
  FORMAT_TEXT_COMMAND,
  UNDO_COMMAND,
  REDO_COMMAND,
  INDENT_CONTENT_COMMAND,
  OUTDENT_CONTENT_COMMAND,
} from "lexical"
import {
  INSERT_UNORDERED_LIST_COMMAND,
  INSERT_ORDERED_LIST_COMMAND,
} from "@lexical/list"
import { TOGGLE_LINK_COMMAND } from "@lexical/link"
import { $setBlocksType } from "@lexical/selection"
import { $createQuoteNode } from "@lexical/rich-text"
import type { Platform, AppearanceMode } from "@/theme"
import { applyTheme } from "@/theme"
import { IMAGE } from "@/lexical/markdown-transformers"
import { TABLE } from "@/lexical/table-transformer"
import { INSERT_EXCALIDRAW_COMMAND } from "@/lexical/plugins/ExcalidrawPlugin"

const YABA_TRANSFORMERS = [...TRANSFORMERS, IMAGE, TABLE]

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

let editorInstance: LexicalEditor | null = null
let platform: Platform = "compose"
let appearance: AppearanceMode = "auto"
let cursorColor: string | null = null

export function initEditorBridge(editor: LexicalEditor): void {
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
      editorInstance?.update(() => {
        $convertFromMarkdownString(content, YABA_TRANSFORMERS)
      })
    },
    getMarkdown: () => {
      let result = ""
      editorInstance?.getEditorState().read(() => {
        result = $convertToMarkdownString(YABA_TRANSFORMERS)
      })
      return result
    },
    focus: () => editorInstance?.focus(),
    blur: () => editorInstance?.blur(),
    dispatch: (cmd: EditorCommandPayload) => {
      const ed = editorInstance
      if (!ed) return
      ed.update(() => {
        const selection = $getSelection()
        switch (cmd.type) {
          case "toggleBold":
            ed.dispatchCommand(FORMAT_TEXT_COMMAND, "bold")
            break
          case "toggleItalic":
            ed.dispatchCommand(FORMAT_TEXT_COMMAND, "italic")
            break
          case "toggleUnderline":
            ed.dispatchCommand(FORMAT_TEXT_COMMAND, "underline")
            break
          case "toggleStrikethrough":
            ed.dispatchCommand(FORMAT_TEXT_COMMAND, "strikethrough")
            break
          case "toggleCode":
            ed.dispatchCommand(FORMAT_TEXT_COMMAND, "code")
            break
          case "toggleQuote":
            if (selection && $isRangeSelection(selection)) {
              $setBlocksType(selection, () => $createQuoteNode())
            }
            break
          case "insertHr":
            ed.dispatchCommand(INSERT_HORIZONTAL_RULE_COMMAND, undefined)
            break
          case "toggleBulletedList":
            ed.dispatchCommand(INSERT_UNORDERED_LIST_COMMAND, undefined)
            break
          case "toggleNumberedList":
            ed.dispatchCommand(INSERT_ORDERED_LIST_COMMAND, undefined)
            break
          case "indent":
            ed.dispatchCommand(INDENT_CONTENT_COMMAND, undefined)
            break
          case "outdent":
            ed.dispatchCommand(OUTDENT_CONTENT_COMMAND, undefined)
            break
          case "undo":
            ed.dispatchCommand(UNDO_COMMAND, undefined)
            break
          case "redo":
            ed.dispatchCommand(REDO_COMMAND, undefined)
            break
          case "insertLink":
            ed.dispatchCommand(TOGGLE_LINK_COMMAND, cmd.url)
            break
          case "removeLink":
            ed.dispatchCommand(TOGGLE_LINK_COMMAND, null)
            break
          case "openExcalidraw":
            ed.dispatchCommand(INSERT_EXCALIDRAW_COMMAND, undefined)
            break
        }
      })
    },
  }
}
