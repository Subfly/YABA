package dev.subfly.yabacore.model.highlight

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Content-type-aware anchor for a highlight.
 * Sealed to allow future PDF, transcript, image anchors.
 */
sealed interface HighlightAnchor {
    val contentKind: HighlightContentKind
}

/**
 * Anchor for readable markdown/Tiptap content.
 */
@Serializable
@Stable
data class ReadableAnchor(
    val readableVersionId: String,
    val startSectionKey: String,
    val startOffsetInSection: Int,
    val endSectionKey: String,
    val endOffsetInSection: Int,
) : HighlightAnchor {
    override val contentKind: HighlightContentKind get() = HighlightContentKind.READABLE
}

/**
 * Placeholder for future PDF highlights (page/span/quads).
 */
@Serializable
@Stable
data class PdfAnchor(
    val pageIndex: Int = 0,
    val spanId: String = "",
    val quadsJson: String? = null,
) : HighlightAnchor {
    override val contentKind: HighlightContentKind get() = HighlightContentKind.PDF
}

/**
 * Placeholder for future transcript highlights (segment/time offsets).
 */
@Serializable
@Stable
data class TranscriptAnchor(
    val segmentId: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val text: String = "",
) : HighlightAnchor {
    override val contentKind: HighlightContentKind get() = HighlightContentKind.TRANSCRIPT
}

/**
 * Placeholder for future image highlights (geometry + text context).
 */
@Serializable
@Stable
data class ImageAnchor(
    val boundsJson: String? = null,
    val textContext: String? = null,
) : HighlightAnchor {
    override val contentKind: HighlightContentKind get() = HighlightContentKind.IMAGE
}
