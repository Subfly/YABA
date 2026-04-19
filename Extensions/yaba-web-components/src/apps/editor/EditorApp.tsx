import { useState } from "react"
import { CrepeEditorView } from "@/milkdown/CrepeEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function EditorApp() {
  const [editable] = useState(true)

  return (
    <div
      data-yaba-editor
      style={{ width: "100%", height: "100%", backgroundColor: "transparent" }}
    >
      <CrepeEditorView
        editable={editable}
        variant="editor"
        onCrepeReady={initEditorBridge}
        assetsBaseUrl={undefined}
      />
    </div>
  )
}

export { EditorApp }
