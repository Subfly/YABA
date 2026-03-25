package dev.subfly.yabacore.model.annotation

import kotlinx.serialization.Serializable

/**
 * Where the annotation is anchored. Rich-text (TipTap) uses [READABLE] with identity in document JSON;
 * [PDF] keeps section offsets in [dev.subfly.yabacore.database.entities.AnnotationEntity.extrasJson].
 */
@Serializable
enum class AnnotationType {
    READABLE,
    PDF,
    EPUB,
}
