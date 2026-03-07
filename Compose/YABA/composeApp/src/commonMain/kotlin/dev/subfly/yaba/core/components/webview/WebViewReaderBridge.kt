package dev.subfly.yaba.core.components.webview

import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.HighlightUiModel

/**
 * Bridge for the reader WebView: get selection snapshot and push highlights.
 * The viewer implementation registers its WebView and runs JS when requested.
 */
interface WebViewReaderBridge {
    /**
     * Returns the current text selection as a draft for highlight creation, or null if
     * no valid selection.
     */
    suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft?

    /**
     * Returns true if there is a valid selection that can be highlighted (not overlapping
     * existing highlights).
     */
    suspend fun getCanCreateHighlight(): Boolean

    /**
     * Pushes saved highlights into the viewer for rendering.
     */
    suspend fun setHighlights(highlights: List<HighlightUiModel>)

    /**
     * Scrolls the viewer so the highlight with the given ID is visible.
     */
    suspend fun scrollToHighlight(highlightId: String)
}
