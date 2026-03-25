import type { Editor } from "@tiptap/core"
import { getActiveFormattingState } from "./editor-formatting"

export const YABA_EDITOR_HOST_EVENT_PREFIX = "yaba-editor-host:"

export interface EditorHostStateEvent {
  type: "editorState"
  formatting: ReturnType<typeof getActiveFormattingState>
  canCreateAnnotation: boolean
}

let lastPublishedEditorStateJson: string | null = null

export function resetPublishedEditorHostState(): void {
  lastPublishedEditorStateJson = null
}

export function publishEditorHostState(
  editor: Editor | null,
  canCreateAnnotation: () => boolean
): void {
  const payload: EditorHostStateEvent = {
    type: "editorState",
    formatting: getActiveFormattingState(editor),
    canCreateAnnotation: canCreateAnnotation(),
  }
  const json = JSON.stringify(payload)
  if (json === lastPublishedEditorStateJson) return
  lastPublishedEditorStateJson = json
  console.info(`${YABA_EDITOR_HOST_EVENT_PREFIX}${json}`)
}
