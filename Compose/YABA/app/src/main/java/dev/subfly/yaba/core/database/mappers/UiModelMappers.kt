package dev.subfly.yaba.core.database.mappers

import dev.subfly.yaba.core.database.entities.BookmarkEntity
import dev.subfly.yaba.core.database.models.FolderWithBookmarkCount
import dev.subfly.yaba.core.database.models.TagWithBookmarkCount
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import kotlin.time.Instant

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

internal fun BookmarkEntity.toPreviewUiModel(
    folder: FolderUiModel? = null,
    tags: List<TagUiModel> = emptyList(),
    localImagePath: String? = null,
    localIconPath: String? = null,
): BookmarkPreviewUiModel = BookmarkPreviewUiModel(
    id = id,
    folderId = folderId,
    kind = kind,
    label = label,
    description = description,
    createdAt = createdAt.toInstant(),
    editedAt = editedAt.toInstant(),
    viewCount = viewCount,
    isPrivate = isPrivate,
    isPinned = isPinned,
    localImagePath = localImagePath ?: this.localImagePath,
    localIconPath = localIconPath ?: this.localIconPath,
    parentFolder = folder,
    tags = tags,
)

fun FolderWithBookmarkCount.toUiModel(
    children: List<FolderUiModel> = emptyList(),
    bookmarks: List<BookmarkUiModel> = emptyList(),
): FolderUiModel = folder.toUiModel(
    bookmarkCount = bookmarkCount.toInt(),
    children = children,
    bookmarks = bookmarks,
)

fun TagWithBookmarkCount.toUiModel(
    bookmarks: List<BookmarkUiModel> = emptyList(),
): TagUiModel = tag.toUiModel(
    bookmarkCount = bookmarkCount.toInt(),
    bookmarks = bookmarks,
)
