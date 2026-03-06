package dev.subfly.yabacore.state.sync

import dev.subfly.yabacore.state.base.BaseStateMachine
import kotlinx.coroutines.Job

/**
 * State machine for sync status.
 *
 * With DB as single source of truth, no filesystem metadata to scan.
 * Always reports IN_SYNC. Drift detection and cache rebuild removed.
 */
class SyncStateMachine : BaseStateMachine<SyncUIState, SyncEvent>(
    initialState = SyncUIState()
) {
    private var isInitialized = false
    private var initJob: Job? = null

    override fun onEvent(event: SyncEvent) {
        when (event) {
            SyncEvent.OnInit -> onInit()
            SyncEvent.OnScanFilesystem -> onScanFilesystem()
            SyncEvent.OnFullRefresh -> onFullRefresh()
            SyncEvent.OnIncrementalMerge -> onIncrementalMerge()
            SyncEvent.OnFixDrift -> onFixDrift()
            SyncEvent.OnCompactLog -> onCompactLog()
            SyncEvent.OnDismissError -> onDismissError()
        }
    }

    private fun onInit() {
        if (isInitialized) return
        isInitialized = true
        onScanFilesystem()
    }

    private fun onScanFilesystem() {
        initJob?.cancel()
        initJob = launch {
            updateState {
                it.copy(
                    syncStatus = SyncStatus.IN_SYNC,
                    isScanning = false,
                    driftSummary = null,
                )
            }
        }
    }

    private fun onFullRefresh() {
        launch {
            updateState {
                it.copy(
                    syncStatus = SyncStatus.IN_SYNC,
                    driftSummary = null,
                )
            }
        }
    }

    private fun onIncrementalMerge() {
        launch {
            updateState {
                it.copy(
                    syncStatus = SyncStatus.IN_SYNC,
                    driftSummary = null,
                )
            }
        }
    }

    private fun onFixDrift() {
        // No-op: drift detection removed, DB is source of truth
        launch {
            updateState {
                it.copy(
                    syncStatus = SyncStatus.IN_SYNC,
                    driftSummary = null,
                )
            }
        }
    }

    private fun onCompactLog() {
        // No-op: CRDT event log removed
    }

    private fun onDismissError() {
        updateState { it.copy(errorMessage = null) }
    }

    override fun clear() {
        isInitialized = false
        initJob?.cancel()
        initJob = null
        super.clear()
    }
}
