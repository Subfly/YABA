package dev.subfly.yabacore.sync

import kotlin.time.Instant

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
    val bookmarkId: String,
    val relativePath: String,
    val checksum: String,
    val sizeBytes: Long,
    val originDeviceId: String,
    val happenedAt: Instant,
)
