@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.operations

import dev.subfly.yabacore.database.domain.BookmarkMetadataDomainModel
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.filesystem.model.BookmarkFileAssetKind
import dev.subfly.yabacore.model.utils.LinkType
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun FolderDomainModel.toOperationDraft(kind: OperationKind): OperationDraft =
    OperationDraft(
        entityType = OperationEntityType.FOLDER,
        entityId = id.toString(),
        kind = kind,
        happenedAt = editedAt,
        payload = FolderPayload(
            parentId = parentId?.toString(),
            label = label,
            description = description,
            icon = icon,
            colorCode = color.code,
            order = order,
            createdAtEpochMillis = createdAt.toEpochMilliseconds(),
            editedAtEpochMillis = editedAt.toEpochMilliseconds(),
        ),
    )

internal fun TagDomainModel.toOperationDraft(kind: OperationKind): OperationDraft =
    OperationDraft(
        entityType = OperationEntityType.TAG,
        entityId = id.toString(),
        kind = kind,
        happenedAt = editedAt,
        payload = TagPayload(
            label = label,
            icon = icon,
            colorCode = color.code,
            order = order,
            createdAtEpochMillis = createdAt.toEpochMilliseconds(),
            editedAtEpochMillis = editedAt.toEpochMilliseconds(),
        ),
    )

internal fun BookmarkMetadataDomainModel.toOperationDraft(kind: OperationKind): OperationDraft =
    OperationDraft(
        entityType = OperationEntityType.BOOKMARK,
        entityId = id.toString(),
        kind = kind,
        happenedAt = editedAt,
        payload = BookmarkPayload(
            folderId = folderId.toString(),
            label = label,
            description = description,
            kindCode = this.kind.code,
            createdAtEpochMillis = createdAt.toEpochMilliseconds(),
            editedAtEpochMillis = editedAt.toEpochMilliseconds(),
            viewCount = viewCount,
            isPrivate = isPrivate,
            isPinned = isPinned,
            localImagePath = localImagePath,
            localIconPath = localIconPath,
            link = null,
        ),
    )

fun linkBookmarkOperationDraft(
    bookmarkId: Uuid,
    url: String,
    domain: String,
    linkType: LinkType,
    videoUrl: String?,
    kind: OperationKind,
    happenedAt: Instant,
): OperationDraft = OperationDraft(
    entityType = OperationEntityType.LINK_BOOKMARK,
    entityId = bookmarkId.toString(),
    kind = kind,
    happenedAt = happenedAt,
    payload = LinkBookmarkPayload(
        url = url,
        domain = domain,
        linkTypeCode = linkType.code,
        videoUrl = videoUrl,
    ),
)

fun tagLinkOperationDraft(
    tagId: Uuid,
    bookmarkId: Uuid,
    kind: OperationKind,
    happenedAt: Instant,
): OperationDraft = OperationDraft(
    entityType = OperationEntityType.TAG_LINK,
    entityId = "${tagId}|${bookmarkId}",
    kind = kind,
    happenedAt = happenedAt,
    payload = TagLinkPayload(
        tagId = tagId.toString(),
        bookmarkId = bookmarkId.toString(),
    ),
)

data class FileOperationChange(
    val bookmarkId: Uuid,
    val relativePath: String,
    val assetKind: BookmarkFileAssetKind,
    val sizeBytes: Long,
    val checksum: String,
)

fun FileOperationChange.toOperationDraft(
    kind: OperationKind,
    happenedAt: Instant,
): OperationDraft = OperationDraft(
    entityType = OperationEntityType.FILE,
    entityId = "${bookmarkId}|${relativePath}",
    kind = kind,
    happenedAt = happenedAt,
    payload = FilePayload(
        bookmarkId = bookmarkId.toString(),
        relativePath = relativePath,
        assetKindCode = assetKind.code,
        sizeBytes = sizeBytes,
        checksum = checksum,
    ),
)
