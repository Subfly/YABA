@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.sync

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.filesystem.EntityFileManager
import kotlinx.serialization.json.JsonPrimitive
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service for handling entity deletions with death certificates.
 *
 * When an entity is deleted:
 * 1. Write `deleted.json` inside the entity's folder (death certificate)
 * 2. Remove `meta.json`, `link.json` (if applicable)
 * 3. Remove `/content/` directory (bookmarks only)
 * 4. Update SQLite cache to remove entity
 * 5. Store deletion event in events.sqlite
 *
 * For folder deletion, this is cascaded to all child folders and contained bookmarks.
 */
object DeletionService {
    private val folderDao get() = DatabaseProvider.folderDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine

    /**
     * Deletes a folder and all its contents (child folders and bookmarks) recursively.
     *
     * This implements the cascade deletion semantics from the spec:
     * - Recursively find all child folders
     * - Find all bookmarks inside them
     * - Write `deleted.json` for every object
     * - Remove all non-deleted files
     * - No implicit move to Uncategorized
     */
    suspend fun deleteFolderCascade(folderId: Uuid) {
        val deviceId = DeviceIdProvider.get()

        // Collect all folders to delete (this folder + all descendants)
        val foldersToDelete = mutableListOf(folderId)
        collectDescendantFolders(folderId, foldersToDelete)

        // Collect all bookmarks in these folders
        val bookmarksToDelete = mutableListOf<Uuid>()
        foldersToDelete.forEach { folder ->
            val bookmarks = bookmarkDao.getAll()
                .filter { it.folderId == folder.toString() }
                .mapNotNull { runCatching { Uuid.parse(it.id) }.getOrNull() }
            bookmarksToDelete.addAll(bookmarks)
        }

        // Delete all bookmarks first
        bookmarksToDelete.forEach { bookmarkId ->
            deleteBookmark(bookmarkId, deviceId)
        }

        // Delete all folders (children first, then parents)
        foldersToDelete.reversed().forEach { folder ->
            deleteFolder(folder, deviceId)
        }
    }

    /**
     * Deletes a single folder (without cascade).
     * Use [deleteFolderCascade] for recursive deletion.
     *
     * Note: System folders (like Uncategorized) cannot be deleted.
     */
    suspend fun deleteFolder(folderId: Uuid, deviceId: String? = null) {
        // System folders cannot be deleted
        if (CoreConstants.Folder.isSystemFolder(folderId.toString())) {
            return
        }

        val resolvedDeviceId = deviceId ?: DeviceIdProvider.get()
        val existingJson = entityFileManager.readFolderMeta(folderId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val deletionClock = existingClock.increment(resolvedDeviceId)

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteFolder(folderId, deletionClock)

        // 2. Record deletion event
        crdtEngine.recordFieldChange(
            objectId = folderId,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            field = "_deleted",
            value = JsonPrimitive(true),
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        folderDao.deleteById(folderId.toString())
    }

    /**
     * Deletes a single bookmark.
     */
    suspend fun deleteBookmark(bookmarkId: Uuid, deviceId: String? = null) {
        val resolvedDeviceId = deviceId ?: DeviceIdProvider.get()
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val deletionClock = existingClock.increment(resolvedDeviceId)

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteBookmark(bookmarkId, deletionClock)

        // 2. Record deletion event
        crdtEngine.recordFieldChange(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            field = "_deleted",
            value = JsonPrimitive(true),
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        bookmarkDao.deleteByIds(listOf(bookmarkId.toString()))
    }

    /**
     * Deletes a single tag.
     *
     * Note: System tags (like Pinned and Private) cannot be deleted.
     */
    suspend fun deleteTag(tagId: Uuid, deviceId: String? = null) {
        // System tags cannot be deleted
        if (CoreConstants.Tag.isSystemTag(tagId.toString())) {
            return
        }

        val resolvedDeviceId = deviceId ?: DeviceIdProvider.get()
        val existingJson = entityFileManager.readTagMeta(tagId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val deletionClock = existingClock.increment(resolvedDeviceId)

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteTag(tagId, deletionClock)

        // 2. Record deletion event
        crdtEngine.recordFieldChange(
            objectId = tagId,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            field = "_deleted",
            value = JsonPrimitive(true),
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        tagDao.deleteById(tagId.toString())
    }

    /**
     * Checks if an entity is deleted by looking for deleted.json in its folder.
     */
    suspend fun isEntityDeleted(objectType: ObjectType, entityId: Uuid): Boolean {
        return when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(entityId)
            ObjectType.TAG -> entityFileManager.isTagDeleted(entityId)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(entityId)
        }
    }

    /**
     * Recursively collects all descendant folder IDs.
     */
    private suspend fun collectDescendantFolders(parentId: Uuid, result: MutableList<Uuid>) {
        val children = folderDao.getChildren(parentId.toString())
        children.forEach { child ->
            val childId = runCatching { Uuid.parse(child.id) }.getOrNull() ?: return@forEach
            result.add(childId)
            collectDescendantFolders(childId, result)
        }
    }
}
