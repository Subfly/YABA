@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.sync.VectorClock
import kotlin.uuid.ExperimentalUuidApi

/**
 * Global data operations (e.g., wipe all data, database initialization).
 *
 * Uses filesystem-first approach for data management.
 */
object GlobalDataManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val eventsDao get() = EventsDatabaseProvider.eventsDao
    private val entityFileManager get() = EntityFileManager

    /**
     * Ensures the database is ready by performing a simple query.
     * This forces Room to create the database file and tables if they don't exist.
     *
     * Call this at app startup before any user-initiated database operations.
     */
    suspend fun ensureDatabaseReady() {
        // Simple queries to force database initialization
        folderDao.getAll()
        tagDao.getAll()
        bookmarkDao.getAll()
    }

    /**
     * Performs startup self-healing:
     * - Removes any tombstones for system folders/tags
     * - Ensures database is properly initialized
     *
     * Call this at app startup.
     */
    suspend fun performStartupSelfHealing() {
        // 1. Ensure database is ready
        ensureDatabaseReady()

        // 2. Self-heal system folder tombstones
        val uncategorizedId = kotlin.uuid.Uuid.parse(
            dev.subfly.yabacore.common.CoreConstants.Folder.Uncategorized.ID
        )
        if (entityFileManager.isFolderDeleted(uncategorizedId)) {
            entityFileManager.removeFolderTombstone(uncategorizedId)
        }

        // 3. Self-heal system tag tombstones
        val pinnedId = kotlin.uuid.Uuid.parse(
            dev.subfly.yabacore.common.CoreConstants.Tag.Pinned.ID
        )
        val privateId = kotlin.uuid.Uuid.parse(
            dev.subfly.yabacore.common.CoreConstants.Tag.Private.ID
        )
        if (entityFileManager.isTagDeleted(pinnedId)) {
            entityFileManager.removeTagTombstone(pinnedId)
        }
        if (entityFileManager.isTagDeleted(privateId)) {
            entityFileManager.removeTagTombstone(privateId)
        }
    }

    /**
     * Wipe all local data and bookmark files, and clear platform notifications via the provided
     * callback.
     *
     * This marks all entities as deleted in the filesystem and clears the SQLite cache.
     */
    suspend fun wipeAll(
        clearNotifications: suspend () -> Unit = {},
    ) {
        // Clear platform notifications first (Darwin can pass its own)
        clearNotifications()

        val deviceId = DeviceIdProvider.get()

        // Mark all folders as deleted
        val folderIds = entityFileManager.scanAllFolders()
        folderIds.forEach { folderId ->
            if (!entityFileManager.isFolderDeleted(folderId)) {
                val existingJson = entityFileManager.readFolderMeta(folderId)
                val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
                val deletionClock = existingClock.increment(deviceId)
                entityFileManager.deleteFolder(folderId, deletionClock)
            }
        }

        // Mark all tags as deleted
        val tagIds = entityFileManager.scanAllTags()
        tagIds.forEach { tagId ->
            if (!entityFileManager.isTagDeleted(tagId)) {
                val existingJson = entityFileManager.readTagMeta(tagId)
                val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
                val deletionClock = existingClock.increment(deviceId)
                entityFileManager.deleteTag(tagId, deletionClock)
            }
        }

        // Mark all bookmarks as deleted
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        bookmarkIds.forEach { bookmarkId ->
            if (!entityFileManager.isBookmarkDeleted(bookmarkId)) {
                val existingJson = entityFileManager.readBookmarkMeta(bookmarkId)
                val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
                val deletionClock = existingClock.increment(deviceId)
                entityFileManager.deleteBookmark(bookmarkId, deletionClock)
            }
        }

        // Clear SQLite cache
        tagBookmarkDao.deleteAll()
        linkBookmarkDao.deleteAll()
        bookmarkDao.deleteAll()
        tagDao.deleteAll()
        folderDao.deleteAll()

        // Clear events
        eventsDao.deleteAllEvents()
    }
}
