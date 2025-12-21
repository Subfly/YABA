@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import dev.subfly.yabacore.database.models.TagWithBookmarkCount
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import kotlin.uuid.ExperimentalUuidApi

internal fun FolderDomainModel.toUiModel(
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
    createdAt = createdAt,
    editedAt = editedAt,
    order = order,
    bookmarkCount = bookmarkCount,
    children = children,
    bookmarks = bookmarks,
)

internal fun FolderUiModel.toDomain(): FolderDomainModel =
    FolderDomainModel(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = color,
        createdAt = createdAt,
        editedAt = editedAt,
        order = order,
    )

internal fun TagDomainModel.toUiModel(
    bookmarkCount: Int = 0,
    bookmarks: List<BookmarkUiModel> = emptyList(),
): TagUiModel = TagUiModel(
    id = id,
    label = label,
    icon = icon,
    color = color,
    createdAt = createdAt,
    editedAt = editedAt,
    order = order,
    bookmarkCount = bookmarkCount,
    bookmarks = bookmarks,
)

internal fun TagUiModel.toDomain(): TagDomainModel =
    TagDomainModel(
        id = id,
        label = label,
        icon = icon,
        color = color,
        createdAt = createdAt,
        editedAt = editedAt,
        order = order,
    )

internal fun LinkBookmarkDomainModel.toUiModel(
    folder: FolderUiModel? = null,
    tags: List<TagUiModel> = emptyList(),
): LinkmarkUiModel = LinkmarkUiModel(
    id = id,
    folderId = folderId,
    label = label,
    description = description,
    createdAt = createdAt,
    editedAt = editedAt,
    viewCount = viewCount,
    isPrivate = isPrivate,
    isPinned = isPinned,
    url = url,
    domain = domain,
    linkType = linkType,
    previewImageUrl = previewImageUrl,
    previewIconUrl = previewIconUrl,
    videoUrl = videoUrl,
    parentFolder = folder,
    tags = tags,
)

internal fun LinkmarkUiModel.toDomain(): LinkBookmarkDomainModel =
    LinkBookmarkDomainModel(
        id = id,
        folderId = folderId,
        label = label,
        description = description,
        createdAt = createdAt,
        editedAt = editedAt,
        viewCount = viewCount,
        isPrivate = isPrivate,
        isPinned = isPinned,
        url = url,
        domain = domain,
        linkType = linkType,
        previewImageUrl = previewImageUrl,
        previewIconUrl = previewIconUrl,
        videoUrl = videoUrl,
    )

fun FolderWithBookmarkCount.toUiModel(
    children: List<FolderUiModel> = emptyList(),
    bookmarks: List<BookmarkUiModel> = emptyList(),
): FolderUiModel = folder.toModel().toUiModel(
    bookmarkCount = bookmarkCount.toInt(),
    children = children,
    bookmarks = bookmarks,
)

fun TagWithBookmarkCount.toUiModel(
    bookmarks: List<BookmarkUiModel> = emptyList(),
): TagUiModel = tag.toModel().toUiModel(
    bookmarkCount = bookmarkCount.toInt(),
    bookmarks = bookmarks,
)

