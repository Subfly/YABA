package dev.subfly.yabacore.state.sync

/**
 * UI state for sync status display.
 */
data class SyncUIState(
    /** Current sync state */
    val syncStatus: SyncStatus = SyncStatus.IN_SYNC,
    /** Whether a scan/sync operation is in progress */
    val isScanning: Boolean = false,
    /** Number of corrupted paths found (if any) */
    val corruptedPathCount: Int = 0,
    /** Summary of drift detected */
    val driftSummary: DriftSummary? = null,
    /** Last sync timestamp (epoch millis) */
    val lastSyncedAt: Long? = null,
    /** Error message if sync failed */
    val errorMessage: String? = null,
)

/**
 * User-visible sync statuses.
 */
enum class SyncStatus {
    /** Filesystem and SQLite cache are in sync */
    IN_SYNC,

    /** Changes detected, sync needed */
    SYNC_NEEDED,

    /** Currently syncing */
    SYNCING,

    /** Corrupted data detected in filesystem */
    CORRUPTED,

    /** Last sync attempt failed */
    SYNC_FAILED,
}

/**
 * Summary of drift between filesystem and cache.
 */
data class DriftSummary(
    val missingInCacheCount: Int,
    val missingInFilesystemCount: Int,
    val deletedButInCacheCount: Int,
    val dataConflictCount: Int,
) {
    val totalDrift: Int
        get() = missingInCacheCount + missingInFilesystemCount + deletedButInCacheCount + dataConflictCount

    val hasDrift: Boolean
        get() = totalDrift > 0
}
