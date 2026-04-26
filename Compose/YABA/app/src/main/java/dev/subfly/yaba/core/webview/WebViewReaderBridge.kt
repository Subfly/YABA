package dev.subfly.yaba.core.webview

import dev.subfly.yaba.core.model.annotation.ReadableSelectionDraft
import dev.subfly.yaba.core.model.ui.AnnotationUiModel

/**
 * Imperative bridge for reader WebViews (HTML reader shell or PDF): selection, annotations, paging.
 */
interface WebViewReaderBridge {
    suspend fun getSelectionSnapshot(
        bookmarkId: String,
        contentId: String,
    ): ReadableSelectionDraft?

    suspend fun getCanCreateAnnotation(): Boolean

    suspend fun setAnnotations(annotations: List<AnnotationUiModel>)

    suspend fun scrollToAnnotation(annotationId: String)

    suspend fun getPageCount(): Int = 1

    suspend fun getCurrentPageNumber(): Int = 1

    suspend fun nextPage(): Boolean = false

    suspend fun prevPage(): Boolean = false

    suspend fun getDocumentJson(): String = ""

    suspend fun applyAnnotationToSelection(annotationId: String): Boolean = false

    suspend fun removeAnnotationFromDocument(annotationId: String): Int = 0

    suspend fun navigateToTocItem(id: String, extrasJson: String?) = Unit

    /**
     * Rich-text readable shell only: calls `window.YabaEditorBridge.unFocus()`. No-op for PDF/EPUB.
     */
    suspend fun unFocus() {}

    /**
     * Markdown from `window.YabaEditorBridge.exportMarkdown()` when the rich-text reader is active.
     * PDF/EPUB readers return an empty string.
     */
    suspend fun exportReadableMarkdown(): String = ""

    /**
     * Base64 PDF bytes from `window.YabaEditorBridge.startPdfExportJob` / html2pdf.js for the rich-text reader.
     * PDF/EPUB readers return an empty string.
     */
    suspend fun exportReadablePdfBase64(): String = ""
}
