import type { Editor } from "@tiptap/core"

export interface EditorFormattingState {
  bold: boolean
  italic: boolean
  underline: boolean
  strikethrough: boolean
  subscript: boolean
  superscript: boolean
  code: boolean
  codeBlock: boolean
  blockquote: boolean
  bulletList: boolean
  orderedList: boolean
  taskList: boolean
  canUndo: boolean
  canRedo: boolean
  canIndent: boolean
  canOutdent: boolean
  inTable: boolean
  canAddRowBefore: boolean
  canAddRowAfter: boolean
  canDeleteRow: boolean
  canAddColumnBefore: boolean
  canAddColumnAfter: boolean
  canDeleteColumn: boolean
  inlineMath: boolean
  blockMath: boolean
  textHighlight: boolean
}

export function getEmptyFormattingState(): EditorFormattingState {
  return {
    bold: false,
    italic: false,
    underline: false,
    strikethrough: false,
    subscript: false,
    superscript: false,
    code: false,
    codeBlock: false,
    blockquote: false,
    bulletList: false,
    orderedList: false,
    taskList: false,
    canUndo: false,
    canRedo: false,
    canIndent: false,
    canOutdent: false,
    inTable: false,
    canAddRowBefore: false,
    canAddRowAfter: false,
    canDeleteRow: false,
    canAddColumnBefore: false,
    canAddColumnAfter: false,
    canDeleteColumn: false,
    inlineMath: false,
    blockMath: false,
    textHighlight: false,
  }
}

export function getActiveFormattingState(editor: Editor | null): EditorFormattingState {
  if (!editor) return getEmptyFormattingState()
  const inTable = editor.isActive("table")
  return {
    bold: editor.isActive("bold"),
    italic: editor.isActive("italic"),
    underline: editor.isActive("underline"),
    strikethrough: editor.isActive("strike"),
    subscript: editor.isActive("subscript"),
    superscript: editor.isActive("superscript"),
    code: editor.isActive("code"),
    codeBlock: editor.isActive("codeBlock"),
    blockquote: editor.isActive("blockquote"),
    bulletList: editor.isActive("bulletList"),
    orderedList: editor.isActive("orderedList"),
    taskList: editor.isActive("taskList"),
    inlineMath: editor.isActive("inlineMath"),
    blockMath: editor.isActive("blockMath"),
    canUndo: editor.can().undo(),
    canRedo: editor.can().redo(),
    canIndent: editor.can().sinkListItem("listItem"),
    canOutdent: editor.can().liftListItem("listItem"),
    inTable,
    canAddRowBefore: inTable && editor.can().addRowBefore(),
    canAddRowAfter: inTable && editor.can().addRowAfter(),
    canDeleteRow: inTable && editor.can().deleteRow(),
    canAddColumnBefore: inTable && editor.can().addColumnBefore(),
    canAddColumnAfter: inTable && editor.can().addColumnAfter(),
    canDeleteColumn: inTable && editor.can().deleteColumn(),
    textHighlight: editor.isActive("highlight"),
  }
}

export function getActiveFormattingJson(editor: Editor | null): string {
  return JSON.stringify(getActiveFormattingState(editor))
}
