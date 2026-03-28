package dev.subfly.yabacore.webview

import dev.subfly.yabacore.model.annotation.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.AnnotationUiModel

/**
 * Imperative bridge for reader WebViews (HTML reader shell or PDF): selection, annotations, paging.
 */
interface WebViewReaderBridge {
    suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
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
}
