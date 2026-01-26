package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import kotlin.time.Instant

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun FolderEntity.toUiModel(
    bookmarkCount: Int = 0,
    children: List<FolderUiModel> = emptyList(),
    bookmarks: List<dev.subfly.yabacore.model.ui.BookmarkUiModel> = emptyList(),
): FolderUiModel = FolderUiModel(
    id = id,
    parentId = parentId,
    label = label,
    description = description,
    icon = icon,
    color = color,
    createdAt = createdAt.toInstant(),
    editedAt = editedAt.toInstant(),
    order = order,
    bookmarkCount = bookmarkCount,
    children = children,
    bookmarks = bookmarks,
)

internal fun TagEntity.toUiModel(
    bookmarkCount: Int = 0,
    bookmarks: List<dev.subfly.yabacore.model.ui.BookmarkUiModel> = emptyList(),
): TagUiModel = TagUiModel(
    id = id,
    label = label,
    icon = icon,
    color = color,
    createdAt = createdAt.toInstant(),
    editedAt = editedAt.toInstant(),
    order = order,
    bookmarkCount = bookmarkCount,
    bookmarks = bookmarks,
)
