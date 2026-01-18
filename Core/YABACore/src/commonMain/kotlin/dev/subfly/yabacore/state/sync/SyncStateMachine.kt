package dev.subfly.yabacore.state.sync

import dev.subfly.yabacore.database.CacheRebuilder
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.state.base.BaseStateMachine
import dev.subfly.yabacore.sync.LogCompaction
import dev.subfly.yabacore.sync.SyncEngine
import dev.subfly.yabacore.sync.SyncMode
import kotlinx.coroutines.Job
import kotlin.time.Clock

/**
 * State machine for managing filesystem sync operations and status.
 *
 * This replaces the old FileSystemStateManager object and provides:
 * - UI-ready state via [stateFlow] or [onState] callbacks
 * - Event-driven sync operations
 * - Drift detection and resolution
 * - Log compaction
 */
class SyncStateMachine : BaseStateMachine<SyncUIState, SyncEvent>(
    initialState = SyncUIState()
) {
    private var isInitialized = false
    private var scanJob: Job? = null

    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val entityFileManager get() = EntityFileManager

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

        // Initial scan on startup
        onScanFilesystem()
    }

    private fun onScanFilesystem() {
        scanJob?.cancel()
        scanJob = launch {
            updateState { it.copy(isScanning = true, errorMessage = null) }

            try {
                val scanResult = scanAll()

                val driftResult = detectDrift(scanResult)

                val syncStatus = when {
                    scanResult.corruptedPaths.isNotEmpty() -> SyncStatus.CORRUPTED
                    driftResult.hasDrift -> SyncStatus.SYNC_NEEDED
                    else -> SyncStatus.IN_SYNC
                }

                val driftSummary = if (driftResult.hasDrift) {
                    DriftSummary(
                        missingInCacheCount = driftResult.missingInCache.size,
                        missingInFilesystemCount = driftResult.missingInFilesystem.size,
                        deletedButInCacheCount = driftResult.deletedButInCache.size,
                        dataConflictCount = driftResult.dataConflicts.size,
                    )
                } else null

                updateState {
                    it.copy(
                        syncStatus = syncStatus,
                        isScanning = false,
                        corruptedPathCount = scanResult.corruptedPaths.size,
                        driftSummary = driftSummary,
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        syncStatus = SyncStatus.SYNC_FAILED,
                        isScanning = false,
                        errorMessage = e.message ?: "Scan failed",
                    )
                }
            }
        }
    }

    private fun onFullRefresh() {
        launch {
            updateState { it.copy(syncStatus = SyncStatus.SYNCING, errorMessage = null) }

            try {
                SyncEngine.sync(SyncMode.FULL_REFRESH)

                // Also compact logs after full refresh
                LogCompaction.compactIfNeeded()

                updateState {
                    it.copy(
                        syncStatus = SyncStatus.IN_SYNC,
                        driftSummary = null,
                        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        syncStatus = SyncStatus.SYNC_FAILED,
                        errorMessage = e.message ?: "Full refresh failed",
                    )
                }
            }
        }
    }

    private fun onIncrementalMerge() {
        launch {
            updateState { it.copy(syncStatus = SyncStatus.SYNCING, errorMessage = null) }

            try {
                SyncEngine.sync(SyncMode.INCREMENTAL_MERGE)

                // Compact logs after merge
                LogCompaction.compactIfNeeded()

                updateState {
                    it.copy(
                        syncStatus = SyncStatus.IN_SYNC,
                        driftSummary = null,
                        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        syncStatus = SyncStatus.SYNC_FAILED,
                        errorMessage = e.message ?: "Incremental merge failed",
                    )
                }
            }
        }
    }

    private fun onFixDrift() {
        launch {
            updateState { it.copy(syncStatus = SyncStatus.SYNCING, errorMessage = null) }

            try {
                CacheRebuilder.fixDrift()

                updateState {
                    it.copy(
                        syncStatus = SyncStatus.IN_SYNC,
                        driftSummary = null,
                        lastSyncedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        syncStatus = SyncStatus.SYNC_FAILED,
                        errorMessage = e.message ?: "Fix drift failed",
                    )
                }
            }
        }
    }

    private fun onCompactLog() {
        launch {
            try {
                val removed = LogCompaction.compact()
                // Optionally could add removed count to state if needed
            } catch (e: Exception) {
                // Log compaction failures are non-critical, don't update error state
            }
        }
    }

    private fun onDismissError() {
        updateState { it.copy(errorMessage = null) }
    }

    // ==================== Filesystem Scanning ====================

    private data class FileSystemScanResult(
        val folders: List<ScannedFolder>,
        val tags: List<ScannedTag>,
        val bookmarks: List<ScannedBookmark>,
        val corruptedPaths: List<String>,
    )

    private data class ScannedFolder(
        val id: String,
        val isDeleted: Boolean,
        val meta: FolderMetaJson?,
    )

    private data class ScannedTag(
        val id: String,
        val isDeleted: Boolean,
        val meta: TagMetaJson?,
    )

    private data class ScannedBookmark(
        val id: String,
        val isDeleted: Boolean,
        val meta: BookmarkMetaJson?,
        val hasLinkJson: Boolean,
    )

    private suspend fun scanAll(): FileSystemScanResult {
        val corruptedPaths = mutableListOf<String>()

        // Scan folders
        val folderIds = entityFileManager.scanAllFolders()
        val folders = folderIds.map { folderId ->
            val isDeleted = entityFileManager.isFolderDeleted(folderId)
            val meta = if (!isDeleted) {
                try {
                    entityFileManager.readFolderMeta(folderId)
                } catch (e: Exception) {
                    corruptedPaths.add("folders/$folderId/meta.json")
                    null
                }
            } else null
            ScannedFolder(id = folderId, isDeleted = isDeleted, meta = meta)
        }

        // Scan tags
        val tagIds = entityFileManager.scanAllTags()
        val tags = tagIds.map { tagId ->
            val isDeleted = entityFileManager.isTagDeleted(tagId)
            val meta = if (!isDeleted) {
                try {
                    entityFileManager.readTagMeta(tagId)
                } catch (e: Exception) {
                    corruptedPaths.add("tags/$tagId/meta.json")
                    null
                }
            } else null
            ScannedTag(id = tagId, isDeleted = isDeleted, meta = meta)
        }

        // Scan bookmarks
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        val bookmarks = bookmarkIds.map { bookmarkId ->
            val isDeleted = entityFileManager.isBookmarkDeleted(bookmarkId)
            val meta = if (!isDeleted) {
                try {
                    entityFileManager.readBookmarkMeta(bookmarkId)
                } catch (e: Exception) {
                    corruptedPaths.add("bookmarks/$bookmarkId/meta.json")
                    null
                }
            } else null
            val hasLinkJson = !isDeleted && entityFileManager.readLinkJson(bookmarkId) != null
            ScannedBookmark(
                id = bookmarkId,
                isDeleted = isDeleted,
                meta = meta,
                hasLinkJson = hasLinkJson
            )
        }

        return FileSystemScanResult(
            folders = folders,
            tags = tags,
            bookmarks = bookmarks,
            corruptedPaths = corruptedPaths,
        )
    }

    // ==================== Drift Detection ====================

    private enum class EntityType { FOLDER, TAG, BOOKMARK }

    private data class DriftEntity(val id: String, val type: EntityType)

    private data class DriftResult(
        val hasDrift: Boolean,
        val missingInCache: List<DriftEntity>,
        val missingInFilesystem: List<DriftEntity>,
        val deletedButInCache: List<DriftEntity>,
        val dataConflicts: List<DriftEntity>,
    )

    private suspend fun detectDrift(scanResult: FileSystemScanResult): DriftResult {
        val missingInCache = mutableListOf<DriftEntity>()
        val missingInFilesystem = mutableListOf<DriftEntity>()
        val deletedButInCache = mutableListOf<DriftEntity>()
        val dataConflicts = mutableListOf<DriftEntity>()

        // Check folders
        val cacheFolderIds = folderDao.getAll().map { it.id }.toSet()
        val fsFolderIds = scanResult.folders.map { it.id }.toSet()

        scanResult.folders.forEach { scanned ->
            if (scanned.isDeleted && scanned.id in cacheFolderIds) {
                deletedButInCache.add(DriftEntity(scanned.id, EntityType.FOLDER))
            } else if (!scanned.isDeleted && scanned.id !in cacheFolderIds && scanned.meta != null) {
                missingInCache.add(DriftEntity(scanned.id, EntityType.FOLDER))
            }
        }

        cacheFolderIds.forEach { cacheId ->
            if (cacheId !in fsFolderIds) {
                missingInFilesystem.add(DriftEntity(cacheId, EntityType.FOLDER))
            }
        }

        // Check tags
        val cacheTagIds = tagDao.getAll().map { it.id }.toSet()
        val fsTagIds = scanResult.tags.map { it.id }.toSet()

        scanResult.tags.forEach { scanned ->
            if (scanned.isDeleted && scanned.id in cacheTagIds) {
                deletedButInCache.add(DriftEntity(scanned.id, EntityType.TAG))
            } else if (!scanned.isDeleted && scanned.id !in cacheTagIds && scanned.meta != null) {
                missingInCache.add(DriftEntity(scanned.id, EntityType.TAG))
            }
        }

        cacheTagIds.forEach { cacheId ->
            if (cacheId !in fsTagIds) {
                missingInFilesystem.add(DriftEntity(cacheId, EntityType.TAG))
            }
        }

        // Check bookmarks
        val cacheBookmarkIds = bookmarkDao.getAll().map { it.id }.toSet()
        val fsBookmarkIds = scanResult.bookmarks.map { it.id }.toSet()

        scanResult.bookmarks.forEach { scanned ->
            if (scanned.isDeleted && scanned.id in cacheBookmarkIds) {
                deletedButInCache.add(DriftEntity(scanned.id, EntityType.BOOKMARK))
            } else if (!scanned.isDeleted && scanned.id !in cacheBookmarkIds && scanned.meta != null) {
                missingInCache.add(DriftEntity(scanned.id, EntityType.BOOKMARK))
            }
        }

        cacheBookmarkIds.forEach { cacheId ->
            if (cacheId !in fsBookmarkIds) {
                missingInFilesystem.add(DriftEntity(cacheId, EntityType.BOOKMARK))
            }
        }

        val hasDrift = missingInCache.isNotEmpty() ||
            missingInFilesystem.isNotEmpty() ||
            deletedButInCache.isNotEmpty() ||
            dataConflicts.isNotEmpty()

        return DriftResult(
            hasDrift = hasDrift,
            missingInCache = missingInCache,
            missingInFilesystem = missingInFilesystem,
            deletedButInCache = deletedButInCache,
            dataConflicts = dataConflicts,
        )
    }

    override fun clear() {
        isInitialized = false
        scanJob?.cancel()
        scanJob = null
        super.clear()
    }
}
