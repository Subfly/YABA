package dev.subfly.yabacore.model.highlight

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Identifies which bookmark/content/version a highlight belongs to.
 */
@Serializable
@Stable
data class HighlightSourceContext(
    val bookmarkId: String,
    val contentKind: HighlightContentKind,
    /** For READABLE: readableVersionId. For others: content-specific ID. */
    val contentId: String,
) {
    companion object {
        fun readable(bookmarkId: String, readableVersionId: String): HighlightSourceContext =
            HighlightSourceContext(
                bookmarkId = bookmarkId,
                contentKind = HighlightContentKind.READABLE,
                contentId = readableVersionId,
            )
    }
}
