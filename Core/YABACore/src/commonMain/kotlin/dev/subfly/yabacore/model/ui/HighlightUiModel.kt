package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * A highlight annotation. [READABLE]/[NOTE] rely on TipTap document marks; [PDF] uses [extrasJson].
 */
@Stable
data class HighlightUiModel(
    val id: String,
    val type: HighlightType,
    val colorRole: YabaColor,
    val note: String?,
    /** Best-effort selected text for UI preview in creation sheet and overview. */
    val quoteText: String?,
    /** PDF anchor payload (JSON) when [type] is [HighlightType.PDF]. */
    val extrasJson: String?,
    /** Absolute path to the highlight JSON file */
    val absolutePath: String?,
    val createdAt: Long,
    val editedAt: Long,
)
