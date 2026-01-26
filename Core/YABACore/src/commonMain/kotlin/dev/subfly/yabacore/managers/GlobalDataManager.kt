package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.VectorClock
import kotlin.time.Clock

/**
 * Global data operations (e.g., wipe all data).
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
     * Performs startup self-healing:
     * - Removes any tombstones for system folders/tags
     *
     * Uses queueAndAwait to serialize with other operations.
     * Call this at app startup.
     */
    suspend fun performStartupSelfHealing() {
        CoreOperationQueue.queueAndAwait("StartupSelfHealing") {
            performStartupSelfHealingInternal()
        }
    }

    private suspend fun performStartupSelfHealingInternal() {
        val deviceId = DeviceIdProvider.get()

        // Self-heal system folder tombstones
        val uncategorizedId = CoreConstants.Folder.Uncategorized.ID
        if (entityFileManager.isFolderDeleted(uncategorizedId)) {
            entityFileManager.removeFolderTombstone(uncategorizedId)
        }
        // Enforce rule: system folders must always be root-level
        val uncategorizedMeta = entityFileManager.readFolderMeta(uncategorizedId)
        if (uncategorizedMeta != null && uncategorizedMeta.parentId != null) {
            val existingClock = VectorClock.fromMap(uncategorizedMeta.clock)
            val newClock = existingClock.increment(deviceId)
            val now = Clock.System.now().toEpochMilliseconds()
            val fixed = uncategorizedMeta.copy(
                parentId = null,
                editedAt = now,
                clock = newClock.toMap(),
            )
            entityFileManager.writeFolderMeta(fixed)
            folderDao.upsert(
                FolderEntity(
                    id = fixed.id,
                    parentId = fixed.parentId,
                    label = fixed.label,
                    description = fixed.description,
                    icon = fixed.icon,
                    color = YabaColor.fromCode(fixed.colorCode),
                    createdAt = fixed.createdAt,
                    editedAt = fixed.editedAt,
                    isHidden = fixed.isHidden,
                )
            )
        }

        // Self-heal system tag tombstones
        val pinnedId = CoreConstants.Tag.Pinned.ID
        val privateId = CoreConstants.Tag.Private.ID
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
