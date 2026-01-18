package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.domain.BookmarkMetadataDomainModel
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import kotlin.time.Instant

internal fun FolderEntity.toModel(): FolderDomainModel =
    FolderDomainModel(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = color,
        createdAt = createdAt.toInstant(),
        editedAt = editedAt.toInstant(),
        order = order,
    )

internal fun TagEntity.toModel(): TagDomainModel =
    TagDomainModel(
        id = id,
        label = label,
        icon = icon,
        color = color,
        createdAt = createdAt.toInstant(),
        editedAt = editedAt.toInstant(),
        order = order,
    )

internal fun LinkBookmarkWithRelations.toModel(): LinkBookmarkDomainModel = bookmark.toModel(link)

internal fun BookmarkEntity.toMetadataModel(): BookmarkMetadataDomainModel =
    BookmarkMetadataDomainModel(
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
        localImagePath = localImagePath,
        localIconPath = localIconPath,
    )

internal fun BookmarkEntity.toModel(linkEntity: LinkBookmarkEntity): LinkBookmarkDomainModel =
    LinkBookmarkDomainModel(
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
        localImagePath = localImagePath,
        localIconPath = localIconPath,
        url = linkEntity.url,
        domain = linkEntity.domain,
        linkType = linkEntity.linkType,
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
        createdAt = createdAt.toEpochMillis(),
        editedAt = editedAt.toEpochMillis(),
    )

internal fun TagDomainModel.toEntity(): TagEntity =
    TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = color,
        order = order,
        createdAt = createdAt.toEpochMillis(),
        editedAt = editedAt.toEpochMillis(),
    )

private fun Instant.toEpochMillis(): Long = toEpochMilliseconds()
private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
