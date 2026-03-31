package dev.subfly.yaba.core.database.mappers

import dev.subfly.yaba.core.database.entities.FolderEntity
import dev.subfly.yaba.core.database.entities.TagEntity
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import kotlin.time.Instant

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun FolderEntity.toUiModel(
    bookmarkCount: Int = 0,
    children: List<FolderUiModel> = emptyList(),
    bookmarks: List<BookmarkUiModel> = emptyList(),
): FolderUiModel = FolderUiModel(
    id = id,
    parentId = parentId,
    label = label,
    description = description,
    icon = icon,
    color = color,
    createdAt = createdAt.toInstant(),
    editedAt = editedAt.toInstant(),
    bookmarkCount = bookmarkCount,
    children = children,
    bookmarks = bookmarks,
)

internal fun TagEntity.toUiModel(
    bookmarkCount: Int = 0,
    bookmarks: List<BookmarkUiModel> = emptyList(),
): TagUiModel = TagUiModel(
    id = id,
    label = label,
    icon = icon,
    color = color,
    createdAt = createdAt.toInstant(),
    editedAt = editedAt.toInstant(),
    bookmarkCount = bookmarkCount,
    bookmarks = bookmarks,
)
