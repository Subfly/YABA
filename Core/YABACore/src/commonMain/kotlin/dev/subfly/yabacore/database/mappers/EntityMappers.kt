@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

internal fun FolderEntity.toModel(): FolderDomainModel =
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

internal fun TagEntity.toModel(): TagDomainModel =
    TagDomainModel(
        id = id,
        label = label,
        icon = icon,
        color = color,
        createdAt = createdAt,
        editedAt = editedAt,
        order = order,
    )

internal fun LinkBookmarkWithRelations.toModel(): LinkBookmarkDomainModel = bookmark.toModel(link)

internal fun BookmarkEntity.toModel(linkEntity: LinkBookmarkEntity): LinkBookmarkDomainModel =
    LinkBookmarkDomainModel(
        id = id,
        folderId = folderId,
        kind = kind,
        label = label,
        createdAt = createdAt,
        editedAt = editedAt,
        viewCount = viewCount,
        isPrivate = isPrivate,
        isPinned = isPinned,
        description = linkEntity.description,
        url = linkEntity.url,
        domain = linkEntity.domain,
        linkType = linkEntity.linkType,
        previewImageUrl = linkEntity.previewImageUrl,
        previewIconUrl = linkEntity.previewIconUrl,
        videoUrl = linkEntity.videoUrl,
    )

internal fun FolderDomainModel.toEntity(): FolderEntity =
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

internal fun TagDomainModel.toEntity(): TagEntity =
    TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = color,
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
    )
