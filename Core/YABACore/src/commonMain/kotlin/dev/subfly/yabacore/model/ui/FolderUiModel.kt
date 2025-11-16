@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.model.ui

import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class FolderUiModel(
    val id: Uuid,
    val parentId: Uuid?,
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

