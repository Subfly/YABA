package dev.subfly.yaba.core.model.annotation

import kotlinx.serialization.Serializable

/** Serialized into [dev.subfly.yaba.core.database.entities.AnnotationEntity.extrasJson] for [AnnotationType.EPUB]. */
@Serializable
data class EpubAnnotationExtras(
    /** epub.js CFI range for the highlight (inclusive). */
    val cfiRange: String,
)
