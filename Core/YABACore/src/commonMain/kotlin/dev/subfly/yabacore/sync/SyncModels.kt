@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.operations.FilePayload
import dev.subfly.yabacore.database.operations.Operation
import dev.subfly.yabacore.filesystem.model.BookmarkFileAssetKind
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class SyncRequest(
    val deviceId: String,
    val cursors: Map<String, Long> = emptyMap(),
)

data class SyncResponse(
    val deviceId: String,
    val operations: List<Operation>,
)

data class FileSyncDescriptor(
    val bookmarkId: Uuid,
    val relativePath: String,
    val assetKind: BookmarkFileAssetKind,
    val checksum: String,
    val sizeBytes: Long,
    val originDeviceId: String,
    val happenedAt: Instant,
)

fun Operation.toFileSyncDescriptor(): FileSyncDescriptor? {
    val payload = payload as? FilePayload ?: return null
    val bookmarkId = runCatching {
        Uuid.parse(payload.bookmarkId)
    }.getOrNull() ?: return null

    return FileSyncDescriptor(
        bookmarkId = bookmarkId,
        relativePath = payload.relativePath,
        assetKind = BookmarkFileAssetKind.fromCode(payload.assetKindCode),
        checksum = payload.checksum,
        sizeBytes = payload.sizeBytes,
        originDeviceId = originDeviceId,
        happenedAt = happenedAt,
    )
}
