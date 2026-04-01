package dev.subfly.yaba.core.webview

/** Events emitted by the multiplatform WebView composable from the platform host. */
sealed interface YabaWebHostEvent {
    /** Load state transitions (page progress, bridge ready, crashes). */
    data class LoadState(val state: WebLoadState) : YabaWebHostEvent

    /**
     * Reader metrics for toolbar UI; emitted when values change (reduces Compose-side polling).
     */
    data class ReaderMetrics(
        val canCreateAnnotation: Boolean,
        val currentPage: Int,
        val pageCount: Int,
        /** Non-null when the host is the rich-text WebView editor; used for formatting toolbar toggles. */
        val editorFormatting: EditorFormattingState? = null,
    ) : YabaWebHostEvent

    data class HtmlConverterSuccess(val result: WebConverterResult) : YabaWebHostEvent

    data class HtmlConverterFailure(val error: Throwable) : YabaWebHostEvent

    data class PdfConverterSuccess(val result: WebPdfConverterResult) : YabaWebHostEvent

    data class PdfConverterFailure(val error: Throwable) : YabaWebHostEvent

    data class EpubConverterSuccess(val result: WebEpubConverterResult) : YabaWebHostEvent

    data class EpubConverterFailure(val error: Throwable) : YabaWebHostEvent

    data class InitialContentLoad(val result: WebShellLoadResult) : YabaWebHostEvent

    data object NoteEditorIdleForAutosave : YabaWebHostEvent

    data object CanvasIdleForAutosave : YabaWebHostEvent

    data class CanvasMetrics(val metrics: CanvasHostMetrics) : YabaWebHostEvent

    data class TableOfContentsChanged(val toc: Toc?) : YabaWebHostEvent
}
