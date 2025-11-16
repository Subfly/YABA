@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.operations

import dev.subfly.yabacore.model.Folder
import dev.subfly.yabacore.model.LinkBookmark
import dev.subfly.yabacore.model.Tag
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

fun Folder.toOperationDraft(kind: OperationKind): OperationDraft =
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

fun Tag.toOperationDraft(kind: OperationKind): OperationDraft =
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

fun LinkBookmark.toOperationDraft(kind: OperationKind): OperationDraft =
    OperationDraft(
        entityType = OperationEntityType.BOOKMARK,
        entityId = id.toString(),
        kind = kind,
        happenedAt = editedAt,
        payload = BookmarkPayload(
            folderId = folderId.toString(),
            label = label,
            kindCode = this.kind.code,
            createdAtEpochMillis = createdAt.toEpochMilliseconds(),
            editedAtEpochMillis = editedAt.toEpochMilliseconds(),
            link =
                LinkBookmarkPayload(
                    description = description,
                    url = url,
                    domain = domain,
                    linkTypeCode = linkType.code,
                    previewImageUrl = previewImageUrl,
                    previewIconUrl = previewIconUrl,
                    videoUrl = videoUrl,
                ),
        ),
    )
