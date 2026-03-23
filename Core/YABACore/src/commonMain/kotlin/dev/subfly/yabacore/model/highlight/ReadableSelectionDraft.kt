package dev.subfly.yabacore.model.highlight

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Selection payload from a WebView for highlight creation.
 * - Rich-text reader: [pdfAnchor] is null; quote-only (positions live in TipTap `yabaHighlight` marks after apply).
 * - PDF viewer: [pdfAnchor] holds section offsets stored in DB [HighlightEntity.extrasJson].
 */
@Serializable
@Stable
data class ReadableSelectionDraft(
    val sourceContext: HighlightSourceContext,
    val quote: HighlightQuoteSnapshot,
    val pdfAnchor: PdfHighlightExtras? = null,
) {
    val bookmarkId: String get() = sourceContext.bookmarkId
    val readableVersionId: String get() = sourceContext.contentId
}
