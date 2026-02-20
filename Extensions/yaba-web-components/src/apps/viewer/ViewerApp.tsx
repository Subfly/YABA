import { TiptapEditorView } from "@/tiptap/TiptapEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function ViewerApp() {
  return (
    <div data-yaba-viewer style={{ width: "100%", height: "100%" }}>
      <TiptapEditorView editable={false} onEditorReady={initEditorBridge} />
    </div>
  )
}

export { ViewerApp }
