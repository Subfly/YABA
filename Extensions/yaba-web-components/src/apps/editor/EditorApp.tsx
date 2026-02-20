import { useState } from "react"
import { LexicalEditorView } from "@/lexical/LexicalEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function EditorApp() {
  const [editable] = useState(true)

  return (
    <div data-yaba-editor style={{ width: "100%", height: "100%" }}>
      <LexicalEditorView
        editable={editable}
        onEditorReady={initEditorBridge}
        assetsBaseUrl={undefined}
      />
    </div>
  )
}

export { EditorApp }
