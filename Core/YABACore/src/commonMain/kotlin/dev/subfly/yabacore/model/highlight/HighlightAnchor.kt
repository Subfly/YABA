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
 * Anchor for readable HTML / rich-text content.
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
