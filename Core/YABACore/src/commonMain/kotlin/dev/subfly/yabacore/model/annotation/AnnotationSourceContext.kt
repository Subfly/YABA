package dev.subfly.yabacore.model.annotation

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
    /** For READABLE: readableVersionId. For PDF: same id space as stored on the entity. */
    val contentId: String,
) {
    companion object {
        fun readable(bookmarkId: String, readableVersionId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.READABLE,
                contentId = readableVersionId,
            )

        fun pdf(bookmarkId: String, readableVersionId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.PDF,
                contentId = readableVersionId,
            )

        fun epub(bookmarkId: String, readableVersionId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.EPUB,
                contentId = readableVersionId,
            )
    }
}
