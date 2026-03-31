import type { Editor } from "@tiptap/core"
import { getActiveFormattingState } from "./editor-formatting"
import { postToYabaNativeHost } from "./yaba-native-host"

let lastPublishedEditorStateJson: string | null = null

export function resetPublishedEditorHostState(): void {
  lastPublishedEditorStateJson = null
}

export function publishEditorHostState(
  editor: Editor | null,
  canCreateAnnotation: () => boolean,
): void {
  const formatting = getActiveFormattingState(editor)
  const can = canCreateAnnotation()
  const payload = {
    type: "readerMetrics" as const,
    canCreateAnnotation: can,
    currentPage: 1,
    pageCount: 1,
    formatting,
  }
  const json = JSON.stringify(payload)
  if (json === lastPublishedEditorStateJson) return
  lastPublishedEditorStateJson = json
  postToYabaNativeHost(payload)
}
