package dev.subfly.yaba.core.model.annotation

import kotlinx.serialization.Serializable

/**
 * Where the annotation is anchored. Rich-text (TipTap) uses [READABLE] with identity in document JSON;
 * [PDF] keeps section offsets in [dev.subfly.yaba.core.database.entities.AnnotationEntity.extrasJson].
 */
@Serializable
enum class AnnotationType {
    READABLE,
    PDF,
    EPUB,
}
