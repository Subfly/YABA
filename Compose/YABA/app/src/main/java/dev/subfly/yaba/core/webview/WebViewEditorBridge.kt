package dev.subfly.yaba.core.webview

/**
 * Rich-text editor WebView: JSON document I/O, formatting commands, plus the same annotation/selection
 * surface as [WebViewReaderBridge] for the readable mirror.
 */
interface WebViewEditorBridge : WebViewReaderBridge {
    override suspend fun getDocumentJson(): String

    suspend fun getSelectedText(): String

    suspend fun setEditable(editable: Boolean)

    suspend fun setPlaceholder(placeholder: String)

    /**
     * Calls `window.YabaEditorBridge.unFocus()` so the WebView releases IME focus (Compose keyboard APIs alone are not enough).
     * The web layer remembers the text cursor before unfocusing.
     */
    override suspend fun unFocus()

    /** Calls `window.YabaEditorBridge.focus()` — restores focus and, when possible, the stored cursor position. */
    suspend fun focus()

    /** JSON object text for `window.YabaEditorBridge.dispatch` (matches web `EditorCommandPayload`). */
    suspend fun dispatch(payloadJson: String)

    /** Markdown from `window.YabaEditorBridge.exportMarkdown()` (image links as `./assets/<file>`). */
    suspend fun exportNoteMarkdown(): String

    /** Base64-encoded PDF bytes (no `data:` prefix) from async `html2pdf.js` export (`startPdfExportJob` + host message). */
    suspend fun exportNotePdfBase64(): String
}
