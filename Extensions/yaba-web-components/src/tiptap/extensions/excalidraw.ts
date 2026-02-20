import { Node, mergeAttributes } from "@tiptap/core"
import { ReactNodeViewRenderer } from "@tiptap/react"
import { ExcalidrawNodeView } from "./ExcalidrawNodeView"

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    excalidraw: {
      insertExcalidraw: () => ReturnType
    }
  }
}

const EMPTY_DATA = JSON.stringify({ appState: {}, elements: [], files: {} })

export const Excalidraw = Node.create({
  name: "excalidraw",

  group: "block",

  atom: true,

  draggable: true,

  addAttributes() {
    return {
      data: {
        default: EMPTY_DATA,
        parseHTML: (el) => el.getAttribute("data-data") ?? el.getAttribute("data-lexical-excalidraw-json") ?? EMPTY_DATA,
        renderHTML: (attrs) => ({ "data-data": attrs.data }),
      },
    }
  },

  parseHTML() {
    return [
      {
        tag: 'span[data-lexical-excalidraw-json]',
        getAttrs: (el) => {
          const data = (el as HTMLElement).getAttribute("data-lexical-excalidraw-json")
          return data ? { data } : false
        },
      },
      {
        tag: 'div[data-yaba-excalidraw]',
        getAttrs: (el) => {
          const data = (el as HTMLElement).getAttribute("data-data")
          return data ? { data } : false
        },
      },
    ]
  },

  renderHTML({ HTMLAttributes }) {
    return ["div", mergeAttributes({ "data-yaba-excalidraw": "" }, HTMLAttributes), 0]
  },

  parseMarkdown({ token, helpers }) {
    if (token.type !== "code" || token.lang !== "yaba-excalidraw") return null
    const text = typeof token.text === "string" ? token.text : (token as { raw?: string }).raw ?? ""
    try {
      JSON.parse(text)
      return helpers.createNode("excalidraw", { data: text })
    } catch {
      return null
    }
  },

  renderMarkdown({ node }) {
    const data = node.attrs?.data ?? EMPTY_DATA
    return `\`\`\`yaba-excalidraw\n${data}\n\`\`\`\n\n`
  },

  addNodeView() {
    return ReactNodeViewRenderer(ExcalidrawNodeView)
  },

  addCommands() {
    return {
      insertExcalidraw:
        () =>
        ({ commands }) =>
          commands.insertContent({ type: this.name, attrs: { data: EMPTY_DATA } }),
    }
  },
})
