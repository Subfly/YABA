import { Mark } from "@tiptap/core"

export const YabaHighlightMarkName = "yabaHighlight" as const

/**
 * Persists stable highlight identity in the TipTap/ProseMirror document JSON.
 * Appearance (color) is driven by native via decorations + `setHighlights([{ id, colorRole }])`.
 */
export const YabaHighlightMark = Mark.create({
  name: YabaHighlightMarkName,

  addOptions() {
    return {
      HTMLAttributes: {},
    }
  },

  addAttributes() {
    return {
      id: {
        default: null,
        parseHTML: (element) => (element as HTMLElement).getAttribute("data-yaba-highlight-id"),
        renderHTML: (attributes) => {
          if (!attributes.id) {
            return {}
          }
          return {
            "data-yaba-highlight-id": attributes.id,
          }
        },
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: "span[data-yaba-highlight-id]",
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ["span", { ...HTMLAttributes, class: "yaba-highlight-mark" }, 0]
  },
})
