package dev.subfly.yaba.core.model.annotation

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Identifies which bookmark/content/version an annotation belongs to.
 */
@Serializable
@Stable
data class AnnotationSourceContext(
    val bookmarkId: String,
    val type: AnnotationType,
    /** For READABLE: stable id for the current document snapshot. For PDF/EPUB: same id space as before. */
    val contentId: String,
) {
    companion object {
        const val DEFAULT_READABLE_CONTENT_ID: String = "current"

        fun readable(
            bookmarkId: String,
            contentId: String = DEFAULT_READABLE_CONTENT_ID,
        ): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.READABLE,
                contentId = contentId,
            )

        fun pdf(
            bookmarkId: String,
            contentId: String = DEFAULT_READABLE_CONTENT_ID,
        ): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.PDF,
                contentId = contentId,
            )

        fun epub(
            bookmarkId: String,
            contentId: String = DEFAULT_READABLE_CONTENT_ID,
        ): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.EPUB,
                contentId = contentId,
            )
    }
}
