import "katex/dist/katex.min.css"
import StarterKit from "@tiptap/starter-kit"
import Underline from "@tiptap/extension-underline"
import Subscript from "@tiptap/extension-subscript"
import Superscript from "@tiptap/extension-superscript"
import Link from "@tiptap/extension-link"
import Image from "@tiptap/extension-image"
import BaseYoutube from "@tiptap/extension-youtube"
import { Mathematics } from "@tiptap/extension-mathematics"
import { Table } from "@tiptap/extension-table"
import TableRow from "@tiptap/extension-table-row"
import TableCell from "@tiptap/extension-table-cell"
import TableHeader from "@tiptap/extension-table-header"
import TaskList from "@tiptap/extension-task-list"
import TaskItem from "@tiptap/extension-task-item"
import CodeBlockLowlight from "@tiptap/extension-code-block-lowlight"
import { Markdown } from "@tiptap/markdown"
import { all, createLowlight } from "lowlight"
import type { Extensions } from "@tiptap/core"
import { HighlightDecorationsExtension } from "./extensions/highlight-decorations"

const lowlight = createLowlight(all)

const Youtube = BaseYoutube.extend({
  parseMarkdown(token: any, helpers: any) {
    if (token.type !== "code" || token.lang !== "yaba-youtube") return null
    const url = (typeof token.text === "string" ? token.text : token.raw ?? "").trim()
    if (!url) return null
    return helpers.createNode("youtube", { src: url })
  },

  renderMarkdown(node: any) {
    const src = node.attrs?.src ?? ""
    return `\`\`\`yaba-youtube\n${src}\n\`\`\`\n\n`
  },
})

export function createEditorExtensions(): Extensions {
  return [
    StarterKit.configure({
      codeBlock: false,
      link: false,
      underline: false,
    }),
    Underline,
    Subscript,
    Superscript,
    Link.configure({
      openOnClick: false,
      HTMLAttributes: {
        class: "yaba-editor-link",
      },
    }),
    Image.configure({
      inline: false,
      allowBase64: true,
      HTMLAttributes: {
        class: "yaba-editor-img",
        style:
          "max-width: 100%; height: auto; border-radius: 8px; display: block; object-fit: contain; vertical-align: middle;",
      },
    }),
    Youtube.configure({
      width: 640,
      height: 360,
      nocookie: true,
      HTMLAttributes: {
        class: "yaba-editor-youtube",
      },
    }),
    Mathematics.configure({
      katexOptions: { throwOnError: false },
    }),
    Table.configure({
      resizable: false,
      HTMLAttributes: {
        class: "yaba-editor-table",
      },
    }),
    TableRow,
    TableHeader.configure({
      HTMLAttributes: {
        class: "yaba-editor-table-cell-header",
      },
    }),
    TableCell.configure({
      HTMLAttributes: {
        class: "yaba-editor-table-cell",
      },
    }),
    TaskList,
    TaskItem.configure({
      nested: true,
      HTMLAttributes: {
        class: "yaba-editor-task-item",
      },
    }),
    CodeBlockLowlight.configure({
      lowlight,
      HTMLAttributes: {
        class: "yaba-editor-code-block",
      },
    }),
    Markdown.configure({
      markedOptions: {
        gfm: true,
      },
    }),
    HighlightDecorationsExtension,
  ]
}
