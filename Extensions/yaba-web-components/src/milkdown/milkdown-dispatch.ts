import type { Editor } from "@milkdown/core"
import type { Ctx } from "@milkdown/ctx"
import { callCommand, replaceRange } from "@milkdown/kit/utils"
import { editorViewCtx } from "@milkdown/kit/core"
import { undoCommand, redoCommand } from "@milkdown/kit/plugin/history"
import {
  toggleStrongCommand,
  toggleEmphasisCommand,
  toggleInlineCodeCommand,
  createCodeBlockCommand,
  wrapInBlockquoteCommand,
  wrapInBulletListCommand,
  wrapInOrderedListCommand,
  sinkListItemCommand,
  liftListItemCommand,
  toggleLinkCommand,
  updateLinkCommand,
  wrapInHeadingCommand,
  insertHrCommand,
} from "@milkdown/kit/preset/commonmark"
import { toggleStrikethroughCommand, insertTableCommand, deleteSelectedCellsCommand } from "@milkdown/kit/preset/gfm"
import {
  addRowBeforeCommand,
  addRowAfterCommand,
  addColBeforeCommand,
  addColAfterCommand,
} from "@milkdown/kit/preset/gfm"
import type { EditorCommandPayload } from "@/bridge/editor-command-payload"
import { resolveImageSrcForEditor } from "./markdown-assets"
import { BOOKMARK_LINK_PREFIX } from "@/markdown-model"
import { ANNOTATION_HREF_PREFIX } from "./yaba-href"

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
}

function run(editor: Editor, fn: (ctx: Ctx) => void): void {
  editor.action((ctx) => {
    fn(ctx)
  })
}

export function runEditorDispatch(
  editor: Editor,
  cmd: EditorCommandPayload,
  lastAssetsBaseUrl: string | undefined,
  page: string | undefined,
): void {
  switch (cmd.type) {
    case "toggleBold":
      run(editor, (ctx) => void callCommand(toggleStrongCommand.key)(ctx))
      return
    case "toggleItalic":
      run(editor, (ctx) => void callCommand(toggleEmphasisCommand.key)(ctx))
      return
    case "toggleUnderline": {
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        if (from === to) return
        const text = view.state.doc.textBetween(from, to, "")
        replaceRange(`<u>${escapeHtml(text)}</u>`, { from, to })(ctx)
      })
      return
    }
    case "toggleSubscript":
    case "toggleSuperscript":
      return
    case "toggleStrikethrough":
      run(editor, (ctx) => void callCommand(toggleStrikethroughCommand.key)(ctx))
      return
    case "toggleCode":
      run(editor, (ctx) => void callCommand(toggleInlineCodeCommand.key)(ctx))
      return
    case "toggleCodeBlock":
      run(editor, (ctx) => void callCommand(createCodeBlockCommand.key)(ctx))
      return
    case "toggleQuote":
      run(editor, (ctx) => void callCommand(wrapInBlockquoteCommand.key)(ctx))
      return
    case "insertHr":
      run(editor, (ctx) => void callCommand(insertHrCommand.key)(ctx))
      return
    case "toggleBulletedList":
      run(editor, (ctx) => void callCommand(wrapInBulletListCommand.key)(ctx))
      return
    case "toggleNumberedList":
      run(editor, (ctx) => void callCommand(wrapInOrderedListCommand.key)(ctx))
      return
    case "toggleTaskList": {
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { $from } = view.state.selection
        const start = $from.start($from.depth)
        view.dispatch(view.state.tr.insertText("- [ ] ", start))
      })
      return
    }
    case "indent":
      run(editor, (ctx) => void callCommand(sinkListItemCommand.key)(ctx))
      return
    case "outdent":
      run(editor, (ctx) => void callCommand(liftListItemCommand.key)(ctx))
      return
    case "undo":
      run(editor, (ctx) => void callCommand(undoCommand.key)(ctx))
      return
    case "redo":
      run(editor, (ctx) => void callCommand(redoCommand.key)(ctx))
      return
    case "insertLink": {
      const md = `[${cmd.text}](${cmd.url})`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "updateLink": {
      run(editor, (ctx) => void callCommand(updateLinkCommand.key, { href: cmd.url, title: cmd.text })(ctx))
      return
    }
    case "removeLink": {
      run(editor, (ctx) => void callCommand(toggleLinkCommand.key, { href: "" })(ctx))
      return
    }
    case "insertMention": {
      const md = `[${cmd.text}](${BOOKMARK_LINK_PREFIX}${cmd.bookmarkId})`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "updateMention": {
      const md = `[${cmd.text}](${BOOKMARK_LINK_PREFIX}${cmd.bookmarkId})`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "removeMention": {
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        const text = view.state.doc.textBetween(from, to, "\n")
        replaceRange(text, { from, to })(ctx)
      })
      return
    }
    case "insertInlineMath": {
      const md = `$${cmd.latex}$`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "insertBlockMath": {
      const md = `$$\n${cmd.latex}\n$$`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "updateInlineMath":
    case "updateBlockMath": {
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        const isBlock = cmd.type === "updateBlockMath"
        const md = isBlock ? `$$\n${cmd.latex}\n$$` : `$${cmd.latex}$`
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "insertText": {
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        view.dispatch(view.state.tr.insertText(cmd.text, from, to))
      })
      return
    }
    case "setHeading": {
      const level = Math.min(6, Math.max(1, Math.floor(cmd.level)))
      run(editor, (ctx) => void callCommand(wrapInHeadingCommand.key, level)(ctx))
      return
    }
    case "insertTable": {
      const rows = Math.max(1, Math.min(20, Math.floor(cmd.rows)))
      const cols = Math.max(1, Math.min(20, Math.floor(cmd.cols)))
      run(editor, (ctx) => void callCommand(insertTableCommand.key, { row: rows, col: cols })(ctx))
      return
    }
    case "insertImage": {
      const src = resolveImageSrcForEditor(cmd.src, lastAssetsBaseUrl)
      const md = `![](${src})`
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "addRowBefore":
      run(editor, (ctx) => void callCommand(addRowBeforeCommand.key)(ctx))
      return
    case "addRowAfter":
      run(editor, (ctx) => void callCommand(addRowAfterCommand.key)(ctx))
      return
    case "deleteRow":
    case "deleteColumn":
      run(editor, (ctx) => void callCommand(deleteSelectedCellsCommand.key)(ctx))
      return
    case "addColumnBefore":
      run(editor, (ctx) => void callCommand(addColBeforeCommand.key)(ctx))
      return
    case "addColumnAfter":
      run(editor, (ctx) => void callCommand(addColAfterCommand.key)(ctx))
      return
    case "setTextHighlight": {
      if (page !== "editor") return
      const role = (cmd.colorRole || "YELLOW").toUpperCase()
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        if (from === to) return
        const text = view.state.doc.textBetween(from, to, "")
        if (role === "NONE") {
          replaceRange(text, { from, to })(ctx)
          return
        }
        const md = `<mark data-color-role="${role}" class="yaba-editor-text-highlight yaba-highlight-${role.toLowerCase()}">${escapeHtml(
          text
        )}</mark>`
        replaceRange(md, { from, to })(ctx)
      })
      return
    }
    case "unsetTextHighlight": {
      if (page !== "editor") return
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        if (from === to) return
        const text = view.state.doc.textBetween(from, to, "")
        replaceRange(text, { from, to })(ctx)
      })
      return
    }
    case "toggleTextHighlight": {
      if (page !== "editor") return
      run(editor, (ctx) => void callCommand(toggleStrongCommand.key)(ctx))
      return
    }
    default:
      return
  }
}

export function applyAnnotationLinkToSelection(editor: Editor, annotationId: string): boolean {
  let ok = false
  editor.action((ctx) => {
    const view = ctx.get(editorViewCtx)
    const { from, to } = view.state.selection
    if (from === to) return
    const text = view.state.doc.textBetween(from, to, "")
    const href = `${ANNOTATION_HREF_PREFIX}${annotationId}`
    const safe = text.replace(/\\/g, "\\\\").replace(/\[/g, "\\[").replace(/\]/g, "\\]")
    const md = `[${safe}](${href})`
    replaceRange(md, { from, to })(ctx)
    ok = true
  })
  return ok
}
