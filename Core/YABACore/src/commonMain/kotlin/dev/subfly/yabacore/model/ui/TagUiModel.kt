package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Stable
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

@Stable
data class TagUiModel(
    val id: String,
    val label: String,
    val icon: String,
    val color: YabaColor,
    val createdAt: Instant,
    val editedAt: Instant,
    val order: Int,
    val bookmarkCount: Int = 0,
    val bookmarks: List<BookmarkUiModel> = emptyList(),
)
