package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User-visible sync states.
 */
enum class SyncState {
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
 * Result of a filesystem scan.
 */
data class FileSystemScanResult(
    val folders: List<ScannedFolder>,
    val tags: List<ScannedTag>,
    val bookmarks: List<ScannedBookmark>,
    val corruptedPaths: List<String>,
)

data class ScannedFolder(
    val id: String,
    val isDeleted: Boolean,
    val meta: FolderMetaJson?,
)

data class ScannedTag(
    val id: String,
    val isDeleted: Boolean,
    val meta: TagMetaJson?,
)

data class ScannedBookmark(
    val id: String,
    val isDeleted: Boolean,
    val meta: BookmarkMetaJson?,
    val hasLinkJson: Boolean,
)

/**
 * Result of drift detection between filesystem and SQLite cache.
 */
data class DriftResult(
    val hasDrift: Boolean,
    /** Entities in filesystem but not in cache */
    val missingInCache: List<DriftEntity>,
    /** Entities in cache but not in filesystem */
    val missingInFilesystem: List<DriftEntity>,
    /** Entities marked deleted in filesystem but still in cache */
    val deletedButInCache: List<DriftEntity>,
    /** Entities with different data between filesystem and cache */
    val dataConflicts: List<DriftEntity>,
)

data class DriftEntity(
    val id: String,
    val type: EntityType,
)

enum class EntityType {
    FOLDER,
    TAG,
    BOOKMARK,
}

/**
 * FileSystem State Manager (FSM)
 *
 * Responsibilities:
 * - Scan filesystem for all entities
 * - Detect drift between filesystem and SQLite cache
 * - Detect corruption (invalid JSON, missing required fields)
 * - Expose sync state to UI
 *
 * The FSM is the only authority for sync state.
 */
object FileSystemStateManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val entityFileManager get() = EntityFileManager

    private val _syncState = MutableStateFlow(SyncState.IN_SYNC)

    /**
     * Observes the current sync state.
     */
    fun observeSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    /**
     * Gets the current sync state.
     */
    fun getSyncState(): SyncState = _syncState.value

    /**
     * Sets the sync state (for internal use by sync operations).
     */
    fun setSyncState(state: SyncState) {
        _syncState.value = state
    }

    /**
     * Scans the filesystem for all entities.
     */
    suspend fun scanAll(): FileSystemScanResult {
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

    /**
     * Detects drift between filesystem and SQLite cache.
     */
    suspend fun detectDrift(): DriftResult {
        val scanResult = scanAll()

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

        if (hasDrift) {
            _syncState.value = SyncState.SYNC_NEEDED
        }

        if (scanResult.corruptedPaths.isNotEmpty()) {
            _syncState.value = SyncState.CORRUPTED
        }

        return DriftResult(
            hasDrift = hasDrift,
            missingInCache = missingInCache,
            missingInFilesystem = missingInFilesystem,
            deletedButInCache = deletedButInCache,
            dataConflicts = dataConflicts,
        )
    }

    /**
     * Checks if there are any corrupted entities in the filesystem.
     */
    suspend fun hasCorruption(): Boolean {
        val scanResult = scanAll()
        return scanResult.corruptedPaths.isNotEmpty()
    }
}
