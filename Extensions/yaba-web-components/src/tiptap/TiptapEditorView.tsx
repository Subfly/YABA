import { useEffect } from "react"
import { EditorProvider, useCurrentEditor } from "@tiptap/react"
import { createEditorExtensions } from "./editor-extensions"
import "./tiptap-styles.css"

interface TiptapEditorViewProps {
  editable: boolean
  initialMarkdown?: string
  onEditorReady?: (editor: import("@tiptap/core").Editor) => void
  assetsBaseUrl?: string
}

function EditorReadyNotifier({
  onEditorReady,
}: {
  onEditorReady?: (editor: import("@tiptap/core").Editor) => void
}) {
  const { editor } = useCurrentEditor()

  useEffect(() => {
    if (editor && onEditorReady) {
      onEditorReady(editor)
    }
  }, [editor, onEditorReady])

  return null
}

export function TiptapEditorView({
  editable,
  initialMarkdown,
  onEditorReady,
  assetsBaseUrl,
}: TiptapEditorViewProps) {
  let content = initialMarkdown ?? ""
  if (assetsBaseUrl && content.includes("../assets/")) {
    const base = assetsBaseUrl.replace(/\/?$/, "/")
    content = content.replace(/\]\(\.\.\/assets\//g, `](${base}assets/`)
  }

  const extensions = createEditorExtensions()

  return (
    <div className="yaba-editor-container" data-yaba-editor-root>
      <EditorProvider
        extensions={extensions}
        editable={editable}
        content={content}
        contentType="markdown"
        editorProps={{
          attributes: {
            class: "yaba-content-editable",
            style: "caret-color: var(--yaba-cursor, var(--yaba-primary)); outline: none;",
          },
          handleDOMEvents: {
            error: (_, err) => {
              console.error("TipTap error:", err)
            },
          },
        }}
        slotBefore={<EditorReadyNotifier onEditorReady={onEditorReady} />}
      />
    </div>
  )
}
