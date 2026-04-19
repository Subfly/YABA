import type { EditorState } from "@milkdown/prose/state"
import type { EditorView } from "@milkdown/prose/view"
import { undoDepth, redoDepth } from "prosemirror-history"

/** Toolbar / native sheet snapshot for the note editor (Milkdown). */
export interface EditorFormattingState {
  headingLevel: number
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
    headingLevel: 0,
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

function hasParent(state: EditorState, name: string): boolean {
  const { $from } = state.selection
  for (let d = $from.depth; d > 0; d--) {
    if ($from.node(d).type.name === name) return true
  }
  return false
}

function getHeadingLevel(state: EditorState): number {
  const { $from } = state.selection
  for (let d = $from.depth; d > 0; d--) {
    const n = $from.node(d)
    if (n.type.name === "heading") {
      const lv = Number(n.attrs.level ?? 0)
      return Math.max(0, Math.min(6, lv || 0))
    }
  }
  return 0
}

function hasMark(state: EditorState, markName: string): boolean {
  const { $from, empty, from, to } = state.selection
  if (empty) {
    return $from.marks().some((m) => m.type.name === markName)
  }
  let found = false
  state.doc.nodesBetween(from, to, (node) => {
    if (node.isText && node.marks.some((m) => m.type.name === markName)) {
      found = true
      return false
    }
    return true
  })
  return found
}

function hasUnderline(state: EditorState): boolean {
  return hasMark(state, "underline")
}

function inTable(state: EditorState): boolean {
  return hasParent(state, "table")
}

function listDepth(state: EditorState): { inList: boolean; inTask: boolean } {
  let inTask = false
  for (let d = state.selection.$from.depth; d > 0; d--) {
    const n = state.selection.$from.node(d)
    if (n.type.name === "list_item" && n.attrs.checked != null) {
      inTask = true
      break
    }
  }
  const inList = hasParent(state, "bullet_list") || hasParent(state, "ordered_list")
  return { inList, inTask }
}

function hasMathInline(state: EditorState): boolean {
  let hit = false
  const { from, to } = state.selection
  state.doc.nodesBetween(from, to, (node) => {
    if (node.type.name === "math_inline") hit = true
    return !hit
  })
  return hit
}

function hasMathBlock(state: EditorState): boolean {
  const { $from } = state.selection
  for (let d = $from.depth; d > 0; d--) {
    const n = $from.node(d)
    if (n.type.name === "code_block" && String(n.attrs.language ?? "").toLowerCase() === "latex") {
      return true
    }
  }
  return false
}

function hasTextHighlight(state: EditorState): boolean {
  const { from, to } = state.selection
  let hit = false
  state.doc.nodesBetween(from, to, (node) => {
    if (node.isText) {
      if (node.marks.some((m) => m.type.name === "highlight")) hit = true
    }
    return !hit
  })
  return hit
}

export function getActiveFormattingState(view: EditorView | null): EditorFormattingState {
  if (!view) return getEmptyFormattingState()
  const state = view.state
  const { inList, inTask } = listDepth(state)
  const table = inTable(state)

  return {
    headingLevel: getHeadingLevel(state),
    bold: hasMark(state, "strong"),
    italic: hasMark(state, "emphasis"),
    underline: hasUnderline(state),
    strikethrough: hasMark(state, "strike_through"),
    subscript: false,
    superscript: false,
    code: hasMark(state, "code_inline"),
    codeBlock: hasParent(state, "code_block"),
    blockquote: hasParent(state, "blockquote"),
    bulletList: hasParent(state, "bullet_list"),
    orderedList: hasParent(state, "ordered_list"),
    taskList: inTask,
    inlineMath: hasMathInline(state),
    blockMath: hasMathBlock(state),
    canUndo: undoDepth(state) > 0,
    canRedo: redoDepth(state) > 0,
    canIndent: inList,
    canOutdent: inList,
    inTable: table,
    canAddRowBefore: table,
    canAddRowAfter: table,
    canDeleteRow: table,
    canAddColumnBefore: table,
    canAddColumnAfter: table,
    canDeleteColumn: table,
    textHighlight: hasTextHighlight(state),
  }
}

export function getActiveFormattingJson(view: EditorView | null): string {
  return JSON.stringify(getActiveFormattingState(view))
}
