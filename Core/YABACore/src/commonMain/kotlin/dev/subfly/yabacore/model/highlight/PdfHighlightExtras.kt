package dev.subfly.yabacore.model.highlight

import kotlinx.serialization.Serializable

/** Serialized into [HighlightEntity.extrasJson] for [HighlightType.PDF]. */
@Serializable
data class PdfHighlightExtras(
    val startSectionKey: String,
    val startOffsetInSection: Int,
    val endSectionKey: String,
    val endOffsetInSection: Int,
)
