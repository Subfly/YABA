@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.model.Folder
import dev.subfly.yabacore.model.LinkBookmark
import dev.subfly.yabacore.model.Tag
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

fun FolderEntity.toModel(): Folder =
    Folder(
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

fun TagEntity.toModel(): Tag =
    Tag(
        id = id,
        label = label,
        icon = icon,
        color = color,
        createdAt = createdAt,
        editedAt = editedAt,
        order = order,
    )

fun LinkBookmarkWithRelations.toModel(): LinkBookmark = bookmark.toModel(link)

fun BookmarkEntity.toModel(linkEntity: LinkBookmarkEntity): LinkBookmark =
    LinkBookmark(
        id = id,
        folderId = folderId,
        kind = kind,
        label = label,
        createdAt = createdAt,
        editedAt = editedAt,
        description = linkEntity.description,
        url = linkEntity.url,
        domain = linkEntity.domain,
        linkType = linkEntity.linkType,
        previewImageUrl = linkEntity.previewImageUrl,
        previewIconUrl = linkEntity.previewIconUrl,
        videoUrl = linkEntity.videoUrl,
    )

fun Folder.toEntity(): FolderEntity =
    FolderEntity(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = color,
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
    )

fun Tag.toEntity(): TagEntity =
    TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = color,
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
    )
