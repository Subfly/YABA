import "katex/dist/katex.min.css"
import StarterKit from "@tiptap/starter-kit"
import Underline from "@tiptap/extension-underline"
import Subscript from "@tiptap/extension-subscript"
import Superscript from "@tiptap/extension-superscript"
import Link from "@tiptap/extension-link"
import Image from "@tiptap/extension-image"
import Youtube from "@tiptap/extension-youtube"
import { Mathematics } from "@tiptap/extension-mathematics"
import { Table } from "@tiptap/extension-table"
import TableRow from "@tiptap/extension-table-row"
import TableCell from "@tiptap/extension-table-cell"
import TableHeader from "@tiptap/extension-table-header"
import TaskList from "@tiptap/extension-task-list"
import TaskItem from "@tiptap/extension-task-item"
import CodeBlockLowlight from "@tiptap/extension-code-block-lowlight"
import { Markdown } from "@tiptap/markdown"
import { common, createLowlight } from "lowlight"
import type { Extensions } from "@tiptap/core"
import { Excalidraw } from "./extensions/excalidraw"

const lowlight = createLowlight(common)

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
    Excalidraw,
  ]
}
