import { useState } from "react"
import { TiptapEditorView } from "@/tiptap/TiptapEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function EditorApp() {
  const [editable] = useState(true)

  return (
    <div
      data-yaba-editor
      style={{ width: "100%", height: "100%", backgroundColor: "transparent" }}
    >
      <TiptapEditorView
        editable={editable}
        variant="editor"
        onEditorReady={initEditorBridge}
        assetsBaseUrl={undefined}
      />
    </div>
  )
}

export { EditorApp }
