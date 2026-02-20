import { LexicalEditorView } from "@/lexical/LexicalEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function ViewerApp() {
  return (
    <div data-yaba-viewer style={{ width: "100%", height: "100%" }}>
      <LexicalEditorView editable={false} onEditorReady={initEditorBridge} />
    </div>
  )
}

export { ViewerApp }
