package dev.subfly.yabacore.model.ui

import androidx.compose.runtime.Immutable
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Instant

@Immutable
data class FolderUiModel(
    val id: String,
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val color: YabaColor,
    val createdAt: Instant,
    val editedAt: Instant,
    val order: Int,
    val bookmarkCount: Int = 0,
    val children: List<FolderUiModel> = emptyList(),
    val bookmarks: List<BookmarkUiModel> = emptyList(),
)
