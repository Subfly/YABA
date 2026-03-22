import { Extension } from "@tiptap/core"
import { exitCode, newlineInCode } from "@tiptap/pm/commands"
import { Plugin } from "@tiptap/pm/state"
import type { EditorView } from "@tiptap/pm/view"

function handleEnterInCodeBlock(view: EditorView): boolean {
  const { state } = view
  const { selection } = state

  if (!selection.empty) return newlineInCode(state, view.dispatch)

  const $from = selection.$from
  const codeBlock = $from.parent
  if (codeBlock.type.name !== "codeBlock") return false

  const textBeforeCursor = codeBlock.textBetween(0, $from.parentOffset, "\n", "\n")
  const textAfterCursor = codeBlock.textBetween(
    $from.parentOffset,
    codeBlock.content.size,
    "\n",
    "\n"
  )

  // Match familiar editor behavior: pressing Enter on a trailing empty line exits code block.
  if (textAfterCursor.length === 0 && textBeforeCursor.endsWith("\n\n")) {
    return exitCode(state, view.dispatch)
  }

  return newlineInCode(state, view.dispatch)
}

export const CodeBlockEnterBehaviorExtension = Extension.create({
  name: "codeBlockEnterBehavior",
  priority: 1000,

  addKeyboardShortcuts() {
    return {
      Enter: () => {
        if (!this.editor.isActive("codeBlock")) return false
        return handleEnterInCodeBlock(this.editor.view)
      },
    }
  },

  addProseMirrorPlugins() {
    return [
      new Plugin({
        props: {
          handleKeyDown(view, event) {
            if (event.key !== "Enter") return false
            if (event.shiftKey || event.ctrlKey || event.metaKey || event.altKey) return false

            const handled = handleEnterInCodeBlock(view)
            if (handled) event.preventDefault()
            return handled
          },
          handleDOMEvents: {
            beforeinput(view, event) {
              const inputEvent = event as InputEvent
              if (
                inputEvent.inputType !== "insertParagraph" &&
                inputEvent.inputType !== "insertLineBreak"
              ) {
                return false
              }

              const handled = handleEnterInCodeBlock(view)
              if (handled) event.preventDefault()
              return handled
            },
          },
        },
      }),
    ]
  },
})
