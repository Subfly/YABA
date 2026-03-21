import { TiptapEditorView } from "@/tiptap/TiptapEditorView"
import { initEditorBridge } from "@/bridge/editor-bridge"

function ViewerApp() {
  return (
    <div
      data-yaba-viewer
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        minHeight: 0,
        minWidth: 0,
      }}
    >
      <div className="yaba-viewer-editor-slot">
        <TiptapEditorView editable={false} onEditorReady={initEditorBridge} />
      </div>
    </div>
  )
}

export { ViewerApp }
