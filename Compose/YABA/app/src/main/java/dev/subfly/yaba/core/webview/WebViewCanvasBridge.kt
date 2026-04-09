package dev.subfly.yaba.core.webview

/** Native bridge for the Excalidraw canvmark WebView shell. */
interface WebViewCanvasBridge {
    suspend fun getSceneJson(): String
    suspend fun setSceneJson(sceneJson: String)
    suspend fun setActiveTool(tool: String)
    suspend fun undo()
    suspend fun redo()
    suspend fun deleteSelected()
    suspend fun insertImageFromDataUrl(dataUrl: String)
    suspend fun applySelectionStyle(json: String)
    suspend fun canvasLayer(action: String)
}
