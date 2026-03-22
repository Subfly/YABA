package dev.subfly.yabacore.webview

/**
 * Active marks and command availability for the note editor toolbar
 * (from [window.YabaEditorBridge.getActiveFormatting] on Android).
 */
data class EditorFormattingState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val subscript: Boolean = false,
    val superscript: Boolean = false,
    val code: Boolean = false,
    val blockquote: Boolean = false,
    val bulletList: Boolean = false,
    val orderedList: Boolean = false,
    val taskList: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val canIndent: Boolean = false,
    val canOutdent: Boolean = false,
)

/** Events emitted by the multiplatform WebView composable from the platform host. */
sealed interface YabaWebHostEvent {
    /** Load state transitions (page progress, bridge ready, crashes). */
    data class LoadState(val state: WebLoadState) : YabaWebHostEvent

    /**
     * Reader metrics for toolbar UI; emitted when values change (reduces Compose-side polling).
     */
    data class ReaderMetrics(
        val canCreateHighlight: Boolean,
        val currentPage: Int,
        val pageCount: Int,
        /** Non-null when the host is the rich-text editor (TipTap); used for formatting toolbar toggles. */
        val editorFormatting: EditorFormattingState? = null,
    ) : YabaWebHostEvent

    data class HtmlConverterSuccess(val result: WebConverterResult) : YabaWebHostEvent

    data class HtmlConverterFailure(val error: Throwable) : YabaWebHostEvent

    data class PdfConverterSuccess(val result: WebPdfConverterResult) : YabaWebHostEvent

    data class PdfConverterFailure(val error: Throwable) : YabaWebHostEvent
}
