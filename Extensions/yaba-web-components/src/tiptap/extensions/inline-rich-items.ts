import { postToYabaNativeHost } from "@/bridge/yaba-native-host"
import { Node, mergeAttributes } from "@tiptap/core"
import { Plugin } from "@tiptap/pm/state"
import { TextSelection } from "@tiptap/pm/state"
import type { Node as ProseMirrorNode } from "@tiptap/pm/model"
import type { EditorView } from "@tiptap/pm/view"

type InlineRange = { from: number; to: number; node: ProseMirrorNode }

function clampPos(size: number, pos: number): number {
  return Math.max(0, Math.min(size, pos))
}

function findInlineNodeRange(
  doc: ProseMirrorNode,
  pos: number,
  nodeName: string
): InlineRange | null {
  const candidates = [pos, pos - 1, pos + 1].map((it) => clampPos(doc.content.size, it))
  for (const at of candidates) {
    const node = doc.nodeAt(at)
    if (node && node.type.name === nodeName) {
      return { from: at, to: at + node.nodeSize, node }
    }
  }
  return null
}

function placeCaretAroundInlineNode(
  view: EditorView,
  pos: number,
  node: ProseMirrorNode,
  event: MouseEvent
): boolean {
  const target = event.target as HTMLElement | null
  const inlineEl = target?.closest("[data-yaba-inline-link], [data-yaba-inline-mention]") as
    | HTMLElement
    | null
  if (!inlineEl) return false

  const rect = inlineEl.getBoundingClientRect()
  const edgeZonePx = Math.min(12, rect.width * 0.22)
  const clickX = event.clientX

  let caretPos: number | null = null
  if (clickX <= rect.left + edgeZonePx) {
    caretPos = pos
  } else if (clickX >= rect.right - edgeZonePx) {
    caretPos = pos + node.nodeSize
  }

  if (caretPos == null) return false

  const clamped = clampPos(view.state.doc.content.size, caretPos)
  const tr = view.state.tr.setSelection(TextSelection.create(view.state.doc, clamped))
  view.dispatch(tr)
  view.focus()
  event.preventDefault()
  return true
}

const InlineLinkNodeName = "yabaInlineLink"
const InlineMentionNodeName = "yabaInlineMention"

export const YabaInlineLinkNode = Node.create({
  name: InlineLinkNodeName,
  group: "inline",
  inline: true,
  atom: true,
  selectable: false,

  addAttributes() {
    return {
      text: { default: "" },
      url: { default: "" },
    }
  },

  parseHTML() {
    return [{ tag: "span[data-yaba-inline-link]" }]
  },

  renderHTML({ HTMLAttributes }) {
    const text = String(HTMLAttributes.text || "")
    const url = String(HTMLAttributes.url || "")
    return [
      "span",
      mergeAttributes(HTMLAttributes, {
        "data-yaba-inline-link": "true",
        "data-text": text,
        "data-url": url,
        class: "yaba-inline-rich-item yaba-inline-link",
      }),
      text,
    ]
  },

  addCommands() {
    return {
      insertYabaInlineLink:
        (attrs: { text: string; url: string }) =>
        ({ tr, dispatch, state }) => {
          const selectedText = state.doc.textBetween(state.selection.from, state.selection.to, "\n").trim()
          const text = (attrs.text || "").trim() || selectedText || attrs.url
          const url = (attrs.url || "").trim()
          if (!text || !url) return false
          const node = this.type.create({ text, url })
          tr.replaceRangeWith(state.selection.from, state.selection.to, node)
          const caretPos = state.selection.from + node.nodeSize
          tr.setSelection(TextSelection.create(tr.doc, caretPos))
          if (dispatch) dispatch(tr)
          return true
        },
      updateYabaInlineLinkAt:
        (attrs: { text: string; url: string; pos: number }) =>
        ({ tr, dispatch, state }) => {
          const range = findInlineNodeRange(state.doc, attrs.pos, InlineLinkNodeName)
          if (!range) return false
          const oldText = String(range.node.attrs.text || "")
          const oldUrl = String(range.node.attrs.url || "")
          const text = (attrs.text || "").trim() || oldText
          const url = (attrs.url || "").trim() || oldUrl
          if (!text || !url) return false
          const next = this.type.create({ text, url })
          tr.replaceWith(range.from, range.to, next)
          tr.setSelection(TextSelection.create(tr.doc, range.from + next.nodeSize))
          if (dispatch) dispatch(tr)
          return true
        },
      removeYabaInlineLinkAt:
        (attrs: { pos: number }) =>
        ({ tr, dispatch, state }) => {
          const range = findInlineNodeRange(state.doc, attrs.pos, InlineLinkNodeName)
          if (!range) return false
          tr.delete(range.from, range.to)
          tr.setSelection(TextSelection.create(tr.doc, range.from))
          if (dispatch) dispatch(tr)
          return true
        },
    }
  },

  addProseMirrorPlugins() {
    return [
      new Plugin({
        props: {
          handleClickOn(view, pos, node, _nodePos, event) {
            if (node.type.name !== InlineLinkNodeName) return false
            if (event instanceof MouseEvent && placeCaretAroundInlineNode(view, pos, node, event)) {
              return true
            }
            event.preventDefault()
            postToYabaNativeHost({
              type: "inlineLinkTap",
              pos,
              text: String(node.attrs.text || ""),
              url: String(node.attrs.url || ""),
            })
            return true
          },
        },
      }),
    ]
  },
})

export const YabaInlineMentionNode = Node.create({
  name: InlineMentionNodeName,
  group: "inline",
  inline: true,
  atom: true,
  selectable: false,

  addAttributes() {
    return {
      text: { default: "" },
      bookmarkId: { default: "" },
      bookmarkKindCode: { default: 0 },
      bookmarkLabel: { default: "" },
    }
  },

  parseHTML() {
    return [{ tag: "span[data-yaba-inline-mention]" }]
  },

  renderHTML({ HTMLAttributes }) {
    const text = String(HTMLAttributes.text || "")
    const bookmarkId = String(HTMLAttributes.bookmarkId || "")
    const bookmarkKindCode = Number(HTMLAttributes.bookmarkKindCode || 0)
    const bookmarkLabel = String(HTMLAttributes.bookmarkLabel || "")
    return [
      "span",
      mergeAttributes(HTMLAttributes, {
        "data-yaba-inline-mention": "true",
        "data-text": text,
        "data-bookmark-id": bookmarkId,
        "data-bookmark-kind-code": bookmarkKindCode,
        "data-bookmark-label": bookmarkLabel,
        class: "yaba-inline-rich-item yaba-inline-mention",
      }),
      text,
    ]
  },

  addCommands() {
    return {
      insertYabaInlineMention:
        (attrs: { text: string; bookmarkId: string; bookmarkKindCode: number; bookmarkLabel: string }) =>
        ({ tr, dispatch, state }) => {
          const selectedText = state.doc.textBetween(state.selection.from, state.selection.to, "\n").trim()
          const text = (attrs.text || "").trim() || selectedText
          const bookmarkId = (attrs.bookmarkId || "").trim()
          const bookmarkKindCode = Number.isFinite(attrs.bookmarkKindCode) ? attrs.bookmarkKindCode : 0
          const bookmarkLabel = (attrs.bookmarkLabel || "").trim()
          if (!text || !bookmarkId) return false
          const node = this.type.create({ text, bookmarkId, bookmarkKindCode, bookmarkLabel })
          tr.replaceRangeWith(state.selection.from, state.selection.to, node)
          const caretPos = state.selection.from + node.nodeSize
          tr.setSelection(TextSelection.create(tr.doc, caretPos))
          if (dispatch) dispatch(tr)
          return true
        },
      updateYabaInlineMentionAt:
        (attrs: {
          text: string
          bookmarkId: string
          bookmarkKindCode: number
          bookmarkLabel: string
          pos: number
        }) =>
        ({ tr, dispatch, state }) => {
          const range = findInlineNodeRange(state.doc, attrs.pos, InlineMentionNodeName)
          if (!range) return false
          const oldText = String(range.node.attrs.text || "")
          const oldBookmarkId = String(range.node.attrs.bookmarkId || "")
          const oldKindCode = Number(range.node.attrs.bookmarkKindCode || 0)
          const oldBookmarkLabel = String(range.node.attrs.bookmarkLabel || "")
          const text = (attrs.text || "").trim() || oldText
          const bookmarkId = (attrs.bookmarkId || "").trim() || oldBookmarkId
          const bookmarkKindCode = Number.isFinite(attrs.bookmarkKindCode)
            ? attrs.bookmarkKindCode
            : oldKindCode
          const bookmarkLabel = (attrs.bookmarkLabel || "").trim() || oldBookmarkLabel
          if (!text || !bookmarkId) return false
          const next = this.type.create({ text, bookmarkId, bookmarkKindCode, bookmarkLabel })
          tr.replaceWith(range.from, range.to, next)
          tr.setSelection(TextSelection.create(tr.doc, range.from + next.nodeSize))
          if (dispatch) dispatch(tr)
          return true
        },
      removeYabaInlineMentionAt:
        (attrs: { pos: number }) =>
        ({ tr, dispatch, state }) => {
          const range = findInlineNodeRange(state.doc, attrs.pos, InlineMentionNodeName)
          if (!range) return false
          tr.delete(range.from, range.to)
          tr.setSelection(TextSelection.create(tr.doc, range.from))
          if (dispatch) dispatch(tr)
          return true
        },
    }
  },

  addProseMirrorPlugins() {
    return [
      new Plugin({
        props: {
          handleClickOn(view, pos, node, _nodePos, event) {
            if (node.type.name !== InlineMentionNodeName) return false
            if (event instanceof MouseEvent && placeCaretAroundInlineNode(view, pos, node, event)) {
              return true
            }
            event.preventDefault()
            postToYabaNativeHost({
              type: "inlineMentionTap",
              pos,
              text: String(node.attrs.text || ""),
              bookmarkId: String(node.attrs.bookmarkId || ""),
              bookmarkKindCode: Number(node.attrs.bookmarkKindCode || 0),
              bookmarkLabel: String(node.attrs.bookmarkLabel || ""),
            })
            return true
          },
        },
      }),
    ]
  },
})

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    yabaInlineLink: {
      insertYabaInlineLink: (attrs: { text: string; url: string }) => ReturnType
      updateYabaInlineLinkAt: (attrs: { text: string; url: string; pos: number }) => ReturnType
      removeYabaInlineLinkAt: (attrs: { pos: number }) => ReturnType
    }
    yabaInlineMention: {
      insertYabaInlineMention: (attrs: {
        text: string
        bookmarkId: string
        bookmarkKindCode: number
        bookmarkLabel: string
      }) => ReturnType
      updateYabaInlineMentionAt: (attrs: {
        text: string
        bookmarkId: string
        bookmarkKindCode: number
        bookmarkLabel: string
        pos: number
      }) => ReturnType
      removeYabaInlineMentionAt: (attrs: { pos: number }) => ReturnType
    }
  }
}
