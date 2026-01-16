@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.sync

import dev.subfly.yabacore.filesystem.model.BookmarkFileAssetKind
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Request to sync with a remote peer.
 *
 * Contains the local device ID and vector clock state for each known device.
 */
data class SyncRequest(
    val deviceId: String,
    /** Map of deviceId to last known clock value for that device */
    val knownClocks: Map<String, Long> = emptyMap(),
)

/**
 * Response from a sync request.
 *
 * Contains CRDT events that the requesting device doesn't have.
 */
data class SyncResponse(
    val deviceId: String,
    val events: List<CRDTEvent>,
)

/**
 * Describes a file that needs to be synced between devices.
 */
data class FileSyncDescriptor(
    val bookmarkId: Uuid,
    val relativePath: String,
    val assetKind: BookmarkFileAssetKind,
    val checksum: String,
    val sizeBytes: Long,
    val originDeviceId: String,
    val happenedAt: Instant,
)
