package dev.subfly.yabacore.webview

import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.HighlightUiModel

/**
 * Imperative bridge for reader WebViews (HTML reader shell or PDF): selection, highlights, paging.
 */
interface WebViewReaderBridge {
    suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft?

    suspend fun getCanCreateHighlight(): Boolean

    suspend fun setHighlights(highlights: List<HighlightUiModel>)

    suspend fun scrollToHighlight(highlightId: String)

    suspend fun getPageCount(): Int = 1

    suspend fun getCurrentPageNumber(): Int = 1

    suspend fun nextPage(): Boolean = false

    suspend fun prevPage(): Boolean = false

    suspend fun getDocumentJson(): String = ""

    suspend fun applyHighlightToSelection(highlightId: String): Boolean = false

    suspend fun removeHighlightFromDocument(highlightId: String): Int = 0
}
