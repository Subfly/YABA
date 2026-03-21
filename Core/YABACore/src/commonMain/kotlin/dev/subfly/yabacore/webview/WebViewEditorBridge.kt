package dev.subfly.yabacore.webview

/**
 * Rich-text editor WebView: JSON document I/O, formatting commands, plus the same highlight/selection
 * surface as [WebViewReaderBridge] for the readable mirror.
 */
interface WebViewEditorBridge : WebViewReaderBridge {
    suspend fun getDocumentJson(): String

    suspend fun setEditable(editable: Boolean)

    /** JSON object text for `window.YabaEditorBridge.dispatch` (matches web `EditorCommandPayload`). */
    suspend fun dispatch(payloadJson: String)
}
