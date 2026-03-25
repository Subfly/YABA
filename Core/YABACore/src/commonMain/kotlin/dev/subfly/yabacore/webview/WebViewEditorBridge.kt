package dev.subfly.yabacore.webview

/**
 * Rich-text editor WebView: JSON document I/O, formatting commands, plus the same annotation/selection
 * surface as [WebViewReaderBridge] for the readable mirror.
 */
interface WebViewEditorBridge : WebViewReaderBridge {
    override suspend fun getDocumentJson(): String

    suspend fun setEditable(editable: Boolean)

    suspend fun setPlaceholder(placeholder: String)

    /**
     * Calls `window.YabaEditorBridge.unFocus()` so the WebView releases IME focus (Compose keyboard APIs alone are not enough).
     * The web layer remembers the text cursor before unfocusing.
     */
    suspend fun unFocus()

    /** Calls `window.YabaEditorBridge.focus()` — restores focus and, when possible, the stored cursor position. */
    suspend fun focus()

    /** JSON object text for `window.YabaEditorBridge.dispatch` (matches web `EditorCommandPayload`). */
    suspend fun dispatch(payloadJson: String)
}
