@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.operations

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class OperationEntityType {
    FOLDER,
    TAG,
    BOOKMARK,
    LINK_BOOKMARK,
    TAG_LINK,
    FILE,
    ALL,
}

enum class OperationKind {
    CREATE,
    UPDATE,
    DELETE,
    MOVE,
    REORDER,
    TAG_ADD,
    TAG_REMOVE,
    BULK_MOVE,
    BULK_DELETE,
}

data class Operation(
    val opId: Uuid,
    val originDeviceId: String,
    val originSeq: Long,
    val entityType: OperationEntityType,
    val entityId: String,
    val kind: OperationKind,
    val happenedAt: Instant,
    val payload: OperationPayload,
)

data class OperationDraft(
    val entityType: OperationEntityType,
    val entityId: String,
    val kind: OperationKind,
    val happenedAt: Instant,
    val payload: OperationPayload,
)

@Serializable
sealed interface OperationPayload

@Serializable
@SerialName("folder")
data class FolderPayload(
    val parentId: String?,
    val label: String,
    val description: String?,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long,
) : OperationPayload

@Serializable
@SerialName("tag")
data class TagPayload(
    val label: String,
    val icon: String,
    val colorCode: Int,
    val order: Int,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long,
) : OperationPayload

@Serializable
@SerialName("bookmark")
data class BookmarkPayload(
    val folderId: String,
    val label: String,
    val description: String? = null,
    val kindCode: Int,
    val createdAtEpochMillis: Long,
    val editedAtEpochMillis: Long,
    val viewCount: Long = 0,
    val isPrivate: Boolean = false,
    val isPinned: Boolean = false,
    /** Relative path within the app-managed bookmark filesystem. */
    val localImagePath: String? = null,
    /** Relative path within the app-managed bookmark filesystem. */
    val localIconPath: String? = null,
    val link: LinkBookmarkPayload? = null,
) : OperationPayload

@Serializable
@SerialName("link_bookmark")
data class LinkBookmarkPayload(
    val url: String,
    val domain: String,
    val linkTypeCode: Int,
    val videoUrl: String?,
) : OperationPayload

@Serializable
@SerialName("tag_link")
data class TagLinkPayload(
    val tagId: String,
    val bookmarkId: String,
) : OperationPayload

@Serializable
@SerialName("file_asset")
data class FilePayload(
    val bookmarkId: String,
    val relativePath: String,
    val assetKindCode: Int,
    val sizeBytes: Long,
    val checksum: String,
) : OperationPayload

@Serializable
@SerialName("delete_all")
object DeleteAllPayload : OperationPayload
