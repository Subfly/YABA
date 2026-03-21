package dev.subfly.yabacore.model.highlight

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Selection payload from the web viewer for readable content.
 * Produced by the rich-text reader WebView bridge when the user selects text and taps the FAB.
 */
@Serializable
@Stable
data class ReadableSelectionDraft(
    val sourceContext: HighlightSourceContext,
    val anchor: ReadableAnchor,
    val quote: HighlightQuoteSnapshot,
) {
    val bookmarkId: String get() = sourceContext.bookmarkId
    val readableVersionId: String get() = anchor.readableVersionId
}
