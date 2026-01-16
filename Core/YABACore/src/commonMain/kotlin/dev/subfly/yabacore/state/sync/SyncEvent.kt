package dev.subfly.yabacore.state.sync

/**
 * Events for the SyncStateMachine.
 */
sealed class SyncEvent {
    /** Initialize and start observing sync state */
    data object OnInit : SyncEvent()

    /** Request a filesystem scan to check for drift */
    data object OnScanFilesystem : SyncEvent()

    /** Request a full refresh (clear cache and rebuild from filesystem) */
    data object OnFullRefresh : SyncEvent()

    /** Request an incremental merge (apply pending CRDT events) */
    data object OnIncrementalMerge : SyncEvent()

    /** Fix detected drift */
    data object OnFixDrift : SyncEvent()

    /** Run log compaction if needed */
    data object OnCompactLog : SyncEvent()

    /** Dismiss error message */
    data object OnDismissError : SyncEvent()
}
