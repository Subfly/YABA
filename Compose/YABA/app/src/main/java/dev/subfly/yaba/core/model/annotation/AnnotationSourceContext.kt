package dev.subfly.yaba.core.model.annotation

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Identifies which bookmark and annotation surface an annotation belongs to.
 */
@Serializable
@Stable
data class AnnotationSourceContext(
    val bookmarkId: String,
    val type: AnnotationType,
) {
    companion object {
        fun readable(bookmarkId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.READABLE,
            )

        fun pdf(bookmarkId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.PDF,
            )

        fun epub(bookmarkId: String): AnnotationSourceContext =
            AnnotationSourceContext(
                bookmarkId = bookmarkId,
                type = AnnotationType.EPUB,
            )
    }
}
