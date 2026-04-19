export type EditorCommandPayload =
  | { type: "toggleBold" }
  | { type: "toggleItalic" }
  | { type: "toggleUnderline" }
  | { type: "toggleStrikethrough" }
  | { type: "toggleSubscript" }
  | { type: "toggleSuperscript" }
  | { type: "toggleCode" }
  | { type: "toggleCodeBlock" }
  | { type: "toggleQuote" }
  | { type: "insertHr" }
  | { type: "toggleBulletedList" }
  | { type: "toggleNumberedList" }
  | { type: "toggleTaskList" }
  | { type: "indent" }
  | { type: "outdent" }
  | { type: "undo" }
  | { type: "redo" }
  | { type: "insertLink"; text: string; url: string }
  | { type: "updateLink"; text: string; url: string; pos: number }
  | { type: "removeLink"; pos: number }
  | {
      type: "insertMention"
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
    }
  | {
      type: "updateMention"
      text: string
      bookmarkId: string
      bookmarkKindCode: number
      bookmarkLabel: string
      pos: number
    }
  | { type: "removeMention"; pos: number }
  | { type: "insertInlineMath"; latex: string }
  | { type: "insertBlockMath"; latex: string }
  | { type: "updateInlineMath"; latex: string; pos: number }
  | { type: "updateBlockMath"; latex: string; pos: number }
  | { type: "insertText"; text: string }
  | { type: "setHeading"; level: number }
  | { type: "insertTable"; rows: number; cols: number; withHeaderRow?: boolean }
  | { type: "insertImage"; src: string }
  | { type: "addRowBefore" }
  | { type: "addRowAfter" }
  | { type: "deleteRow" }
  | { type: "addColumnBefore" }
  | { type: "addColumnAfter" }
  | { type: "deleteColumn" }
  | { type: "setTextHighlight"; colorRole: string }
  | { type: "unsetTextHighlight" }
  | { type: "toggleTextHighlight" }
