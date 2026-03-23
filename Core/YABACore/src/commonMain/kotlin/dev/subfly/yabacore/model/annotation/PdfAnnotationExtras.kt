package dev.subfly.yabacore.model.annotation

import kotlinx.serialization.Serializable

/** Serialized into [dev.subfly.yabacore.database.entities.AnnotationEntity.extrasJson] for [AnnotationType.PDF]. */
@Serializable
data class PdfAnnotationExtras(
    val startSectionKey: String,
    val startOffsetInSection: Int,
    val endSectionKey: String,
    val endOffsetInSection: Int,
)
