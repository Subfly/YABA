package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * A persisted annotation. [READABLE] relies on TipTap document marks; [PDF] uses [extrasJson].
 */
@Stable
data class AnnotationUiModel(
    val id: String,
    val type: AnnotationType,
    val colorRole: YabaColor,
    val note: String?,
    /** Best-effort selected text for UI preview in creation sheet and overview. */
    val quoteText: String?,
    /** PDF anchor payload (JSON) when [type] is [AnnotationType.PDF]. */
    val extrasJson: String?,
    val createdAt: Long,
    val editedAt: Long,
)
