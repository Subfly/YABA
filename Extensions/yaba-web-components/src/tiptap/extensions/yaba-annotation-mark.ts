import { Mark } from "@tiptap/core"

export const YabaAnnotationMarkName = "yabaAnnotation" as const

/**
 * Persists stable annotation identity in the TipTap/ProseMirror document JSON.
 * Appearance (color) is driven by native via decorations + `setAnnotations([{ id, colorRole }])`.
 */
export const YabaAnnotationMark = Mark.create({
  name: YabaAnnotationMarkName,

  addOptions() {
    return {
      HTMLAttributes: {},
    }
  },

  addAttributes() {
    return {
      id: {
        default: null,
        parseHTML: (element) => (element as HTMLElement).getAttribute("data-yaba-annotation-id"),
        renderHTML: (attributes) => {
          if (!attributes.id) {
            return {}
          }
          return {
            "data-yaba-annotation-id": attributes.id,
          }
        },
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: "span[data-yaba-annotation-id]",
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ["span", { ...HTMLAttributes, class: "yaba-annotation-mark" }, 0]
  },
})
