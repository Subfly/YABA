package dev.subfly.yabacore.model.annotation

import kotlinx.serialization.Serializable

/** Serialized into [dev.subfly.yabacore.database.entities.AnnotationEntity.extrasJson] for [AnnotationType.EPUB]. */
@Serializable
data class EpubAnnotationExtras(
    /** epub.js CFI range for the highlight (inclusive). */
    val cfiRange: String,
)
