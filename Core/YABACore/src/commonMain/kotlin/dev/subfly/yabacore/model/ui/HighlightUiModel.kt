package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.YabaColor

/**
 * A highlight annotation.
 */
@Stable
data class HighlightUiModel(
    val id: String,
    val startBlockId: String,
    val startInlinePath: List<Int>,
    val startOffset: Int,
    val endBlockId: String,
    val endInlinePath: List<Int>,
    val endOffset: Int,
    val colorRole: YabaColor,
    val note: String?,
    /** Absolute path to the highlight JSON file */
    val absolutePath: String?,
    val createdAt: Long,
    val editedAt: Long,
)
