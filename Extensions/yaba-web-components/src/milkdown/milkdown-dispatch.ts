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
import {
  toggleStrikethroughCommand,
  insertTableCommand,
  deleteSelectedCellsCommand,
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

/** Run a Milkdown kit command by key (see ./docs/api/utils.md — `callCommand`). */
function runCallCommand(editor: Editor, runner: (ctx: Ctx) => boolean | void): void {
  run(editor, (ctx) => {
    void runner(ctx)
  })
}

function replaceRangeMarkdown(ctx: Ctx, markdown: string): void {
  const view = ctx.get(editorViewCtx)
  const { from, to } = view.state.selection
  replaceRange(markdown, { from, to })(ctx)
}

function textBetweenSelection(ctx: Ctx, blockSeparator: string): string {
  const view = ctx.get(editorViewCtx)
  const { from, to } = view.state.selection
  return view.state.doc.textBetween(from, to, blockSeparator)
}

export function runEditorDispatch(
  editor: Editor,
  cmd: EditorCommandPayload,
  lastAssetsBaseUrl: string | undefined,
  page: string | undefined,
): void {
  switch (cmd.type) {
    case "toggleBold":
      runCallCommand(editor, (ctx) => callCommand(toggleStrongCommand.key)(ctx))
      return
    case "toggleItalic":
      runCallCommand(editor, (ctx) => callCommand(toggleEmphasisCommand.key)(ctx))
      return
    case "toggleUnderline":
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        if (from === to) return
        const text = view.state.doc.textBetween(from, to, "")
        replaceRange(`<u>${escapeHtml(text)}</u>`, { from, to })(ctx)
      })
      return
    case "toggleSubscript":
    case "toggleSuperscript":
      return
    case "toggleStrikethrough":
      runCallCommand(editor, (ctx) => callCommand(toggleStrikethroughCommand.key)(ctx))
      return
    case "toggleCode":
      runCallCommand(editor, (ctx) => callCommand(toggleInlineCodeCommand.key)(ctx))
      return
    case "toggleCodeBlock":
      runCallCommand(editor, (ctx) => callCommand(createCodeBlockCommand.key)(ctx))
      return
    case "toggleQuote":
      runCallCommand(editor, (ctx) => callCommand(wrapInBlockquoteCommand.key)(ctx))
      return
    case "insertHr":
      runCallCommand(editor, (ctx) => callCommand(insertHrCommand.key)(ctx))
      return
    case "toggleBulletedList":
      runCallCommand(editor, (ctx) => callCommand(wrapInBulletListCommand.key)(ctx))
      return
    case "toggleNumberedList":
      runCallCommand(editor, (ctx) => callCommand(wrapInOrderedListCommand.key)(ctx))
      return
    case "toggleTaskList":
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { $from } = view.state.selection
        const start = $from.start($from.depth)
        view.dispatch(view.state.tr.insertText("- [ ] ", start))
      })
      return
    case "indent":
      runCallCommand(editor, (ctx) => callCommand(sinkListItemCommand.key)(ctx))
      return
    case "outdent":
      runCallCommand(editor, (ctx) => callCommand(liftListItemCommand.key)(ctx))
      return
    case "undo":
      runCallCommand(editor, (ctx) => callCommand(undoCommand.key)(ctx))
      return
    case "redo":
      runCallCommand(editor, (ctx) => callCommand(redoCommand.key)(ctx))
      return
    case "insertLink":
      run(editor, (ctx) => replaceRangeMarkdown(ctx, `[${cmd.text}](${cmd.url})`))
      return
    case "updateLink":
      runCallCommand(editor, (ctx) =>
        callCommand(updateLinkCommand.key, { href: cmd.url, title: cmd.text })(ctx),
      )
      return
    case "removeLink":
      runCallCommand(editor, (ctx) => callCommand(toggleLinkCommand.key, { href: "" })(ctx))
      return
    case "insertMention":
      run(editor, (ctx) =>
        replaceRangeMarkdown(ctx, `[${cmd.text}](${BOOKMARK_LINK_PREFIX}${cmd.bookmarkId})`),
      )
      return
    case "updateMention":
      run(editor, (ctx) =>
        replaceRangeMarkdown(ctx, `[${cmd.text}](${BOOKMARK_LINK_PREFIX}${cmd.bookmarkId})`),
      )
      return
    case "removeMention":
      run(editor, (ctx) => replaceRangeMarkdown(ctx, textBetweenSelection(ctx, "\n")))
      return
    case "insertInlineMath":
      run(editor, (ctx) => replaceRangeMarkdown(ctx, `$${cmd.latex}$`))
      return
    case "insertBlockMath":
      run(editor, (ctx) => replaceRangeMarkdown(ctx, `$$\n${cmd.latex}\n$$`))
      return
    case "updateInlineMath":
    case "updateBlockMath":
      run(editor, (ctx) => {
        const isBlock = cmd.type === "updateBlockMath"
        replaceRangeMarkdown(ctx, isBlock ? `$$\n${cmd.latex}\n$$` : `$${cmd.latex}$`)
      })
      return
    case "insertText":
      run(editor, (ctx) => {
        const view = ctx.get(editorViewCtx)
        const { from, to } = view.state.selection
        view.dispatch(view.state.tr.insertText(cmd.text, from, to))
      })
      return
    case "setHeading": {
      const level = Math.min(6, Math.max(1, Math.floor(cmd.level)))
      runCallCommand(editor, (ctx) => callCommand(wrapInHeadingCommand.key, level)(ctx))
      return
    }
    case "insertTable": {
      const rows = Math.max(1, Math.min(20, Math.floor(cmd.rows)))
      const cols = Math.max(1, Math.min(20, Math.floor(cmd.cols)))
      runCallCommand(editor, (ctx) =>
        callCommand(insertTableCommand.key, { row: rows, col: cols })(ctx),
      )
      return
    }
    case "insertImage": {
      const src = resolveImageSrcForEditor(cmd.src, lastAssetsBaseUrl)
      run(editor, (ctx) => replaceRangeMarkdown(ctx, `![](${src})`))
      return
    }
    case "addRowBefore":
      runCallCommand(editor, (ctx) => callCommand(addRowBeforeCommand.key)(ctx))
      return
    case "addRowAfter":
      runCallCommand(editor, (ctx) => callCommand(addRowAfterCommand.key)(ctx))
      return
    case "deleteRow":
    case "deleteColumn":
      runCallCommand(editor, (ctx) => callCommand(deleteSelectedCellsCommand.key)(ctx))
      return
    case "addColumnBefore":
      runCallCommand(editor, (ctx) => callCommand(addColBeforeCommand.key)(ctx))
      return
    case "addColumnAfter":
      runCallCommand(editor, (ctx) => callCommand(addColAfterCommand.key)(ctx))
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
          text,
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
      runCallCommand(editor, (ctx) => callCommand(toggleStrongCommand.key)(ctx))
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
