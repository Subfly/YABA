package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * A highlight annotation (section-anchored).
 */
@Stable
data class HighlightUiModel(
    val id: String,
    val startSectionKey: String,
    val startOffsetInSection: Int,
    val endSectionKey: String,
    val endOffsetInSection: Int,
    val colorRole: YabaColor,
    val note: String?,
    /** Best-effort selected text for UI preview in creation sheet and overview. */
    val quoteText: String?,
    /** Absolute path to the highlight JSON file */
    val absolutePath: String?,
    val createdAt: Long,
    val editedAt: Long,
)
