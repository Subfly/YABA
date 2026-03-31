package dev.subfly.yaba.core.model.annotation

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Selection payload from a WebView for annotation creation.
 * - Rich-text reader: [pdfAnchor] is null; quote-only (positions live in TipTap `yabaAnnotation` marks after apply).
 * - PDF viewer: [pdfAnchor] holds section offsets stored in DB [AnnotationEntity.extrasJson].
 * - EPUB viewer: [epubAnchor] holds CFI range in [AnnotationEntity.extrasJson].
 */
@Serializable
@Stable
data class ReadableSelectionDraft(
    val sourceContext: AnnotationSourceContext,
    val quote: AnnotationQuoteSnapshot,
    val pdfAnchor: PdfAnnotationExtras? = null,
    val epubAnchor: EpubAnnotationExtras? = null,
) {
    val bookmarkId: String get() = sourceContext.bookmarkId
    val readableVersionId: String get() = sourceContext.contentId
}
