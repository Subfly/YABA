@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.mappers

import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun FolderEntity.toModel(): FolderDomainModel =
    FolderDomainModel(
        id = id.toUuid(),
        parentId = parentId.toUuidOrNull(),
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
        id = id.toUuid(),
        label = label,
        icon = icon,
        color = color,
        createdAt = createdAt.toInstant(),
        editedAt = editedAt.toInstant(),
        order = order,
    )

internal fun LinkBookmarkWithRelations.toModel(): LinkBookmarkDomainModel = bookmark.toModel(link)

internal fun BookmarkEntity.toModel(linkEntity: LinkBookmarkEntity): LinkBookmarkDomainModel =
    LinkBookmarkDomainModel(
        id = id.toUuid(),
        folderId = folderId.toUuid(),
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
        id = id.asString(),
        parentId = parentId?.asString(),
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
        id = id.asString(),
        label = label,
        icon = icon,
        color = color,
        order = order,
        createdAt = createdAt.toEpochMillis(),
        editedAt = editedAt.toEpochMillis(),
    )

private fun String.toUuid(): Uuid = Uuid.parse(this)
private fun String?.toUuidOrNull(): Uuid? = this?.let { runCatching { Uuid.parse(it) }.getOrNull() }
private fun Uuid.asString(): String = toString()
private fun Instant.toEpochMillis(): Long = toEpochMilliseconds()
private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
