import "katex/dist/katex.min.css"
import StarterKit from "@tiptap/starter-kit"
import Underline from "@tiptap/extension-underline"
import Subscript from "@tiptap/extension-subscript"
import Superscript from "@tiptap/extension-superscript"
import Link from "@tiptap/extension-link"
import Image from "@tiptap/extension-image"
import { Mathematics } from "@tiptap/extension-mathematics"
import { Table } from "@tiptap/extension-table"
import TableRow from "@tiptap/extension-table-row"
import TableCell from "@tiptap/extension-table-cell"
import TableHeader from "@tiptap/extension-table-header"
import TaskList from "@tiptap/extension-task-list"
import TaskItem from "@tiptap/extension-task-item"
import { Placeholder, TrailingNode } from "@tiptap/extensions"
import CodeBlockLowlight from "@tiptap/extension-code-block-lowlight"
import { Markdown } from "@tiptap/markdown"
import { all, createLowlight } from "lowlight"
import type { Extensions } from "@tiptap/core"
import { HighlightDecorationsExtension } from "./extensions/highlight-decorations"
import { YabaHighlightMark } from "./extensions/yaba-highlight-mark"
import { CodeBlockEnterBehaviorExtension } from "./extensions/code-block-enter-behavior"

const lowlight = createLowlight(all)

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
    Mathematics.configure({
      katexOptions: { throwOnError: false },
      inlineOptions: {
        onClick: (node, pos) => {
          const latex = encodeURIComponent(node.attrs.latex ?? "")
          window.location.href = `yaba://math-tap?kind=inline&pos=${pos}&latex=${latex}`
        },
      },
      blockOptions: {
        onClick: (node, pos) => {
          const latex = encodeURIComponent(node.attrs.latex ?? "")
          window.location.href = `yaba://math-tap?kind=block&pos=${pos}&latex=${latex}`
        },
      },
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
    Placeholder.configure({
      placeholder: "",
      showOnlyWhenEditable: true,
      showOnlyCurrent: false,
    }),
    TrailingNode.configure({
      node: "paragraph",
      notAfter: ["paragraph"],
    }),
    CodeBlockLowlight.configure({
      lowlight,
      HTMLAttributes: {
        class: "yaba-editor-code-block",
      },
    }),
    CodeBlockEnterBehaviorExtension,
    YabaHighlightMark,
    HighlightDecorationsExtension,
    /** Enables markdown-style input (shortcuts, paste, serialization) alongside the rich-text model. */
    Markdown,
  ]
}
