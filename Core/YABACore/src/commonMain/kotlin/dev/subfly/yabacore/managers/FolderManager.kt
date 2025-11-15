package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.dao.BookmarkTagDao
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.dao.TombstoneDao
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TombstoneEntity
import dev.subfly.yabacore.database.models.FolderWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FolderManager(
    private val folderDao: FolderDao,
    private val bookmarkDao: BookmarkDao,
    private val bookmarkTagDao: BookmarkTagDao,
    private val tombstoneDao: TombstoneDao,
) {

    data class FolderUpdate(
        val label: String? = null,
        val iconName: String? = null,
        val color: Int? = null,
        val parentId: String? = null,
        val order: Int? = null,
    )

    fun getFoldersFlow(): Flow<List<FolderEntity>> = folderDao.getAllFlow()
    fun getFoldersWithBookmarkCountFlow(): Flow<List<FolderWithCount>> = folderDao.getFoldersWithBookmarkCountFlow()

    suspend fun ensureDefaultFolderExists(): String {
        val existing = folderDao.getById(CoreConstants.UNCATEGORIZED_FOLDER_ID)
        if (existing != null) return existing.id
        val now = Clock.System.now()
        val folder = FolderEntity(
            id = CoreConstants.UNCATEGORIZED_FOLDER_ID,
            label = CoreConstants.UNCATEGORIZED_FOLDER_NAME,
            iconName = CoreConstants.UNCATEGORIZED_FOLDER_ICON,
            color = 0,
            parentId = null,
            order = 0,
            createdAt = now,
            editedAt = now,
            version = 1,
        )
        folderDao.insert(folder)
        return folder.id
    }

    suspend fun createFolder(
        label: String,
        iconName: String,
        color: Int,
        parentId: String?,
    ): FolderEntity {
        if (label == CoreConstants.UNCATEGORIZED_FOLDER_NAME) {
            // Prevent duplicating reserved folder name
            ensureDefaultFolderExists()
        }

        val now = Clock.System.now()
        val order = if (parentId == null) {
            (folderDao.getMaxOrderForRoot() ?: -1) + 1
        } else {
            (folderDao.getMaxOrderForParent(parentId) ?: -1) + 1
        }

        val entity = FolderEntity(
            id = Uuid.random().toString(),
            label = label,
            iconName = iconName,
            color = color,
            parentId = parentId,
            order = order,
            createdAt = now,
            editedAt = now,
            version = 1,
        )
        folderDao.insert(entity)
        return entity
    }

    suspend fun updateFolder(id: String, updates: FolderUpdate) {
        val existing = folderDao.getById(id) ?: return
        if (existing.id == CoreConstants.UNCATEGORIZED_FOLDER_ID) {
            // The reserved folder cannot be renamed or moved
            val now = Clock.System.now()
            val updated = existing.copy(
                editedAt = now,
                version = existing.version + 1,
            )
            folderDao.update(updated)
            return
        }

        val now = Clock.System.now()
        val updated = existing.copy(
            label = updates.label ?: existing.label,
            iconName = updates.iconName ?: existing.iconName,
            color = updates.color ?: existing.color,
            parentId = updates.parentId ?: existing.parentId,
            order = updates.order ?: existing.order,
            editedAt = now,
            version = existing.version + 1,
        )
        folderDao.update(updated)
    }

    suspend fun moveFolder(id: String, newParentId: String?, newOrder: Int? = null) {
        val existing = folderDao.getById(id) ?: return
        if (existing.id == CoreConstants.UNCATEGORIZED_FOLDER_ID) return

        val now = Clock.System.now()
        val targetOrder = newOrder ?: if (newParentId == null) {
            (folderDao.getMaxOrderForRoot() ?: -1) + 1
        } else {
            (folderDao.getMaxOrderForParent(newParentId) ?: -1) + 1
        }
        val updated = existing.copy(
            parentId = newParentId,
            order = targetOrder,
            editedAt = now,
            version = existing.version + 1,
        )
        folderDao.update(updated)
    }

    suspend fun deleteFolderRecursive(id: String, deviceId: String? = null) {
        if (id == CoreConstants.UNCATEGORIZED_FOLDER_ID) return
        val stack = ArrayDeque<String>()
        stack.add(id)

        val foldersToDelete = mutableListOf<FolderEntity>()
        val bookmarksToDeleteIds = mutableListOf<String>()

        while (stack.isNotEmpty()) {
            val currentId = stack.removeLast()
            val folder = folderDao.getById(currentId) ?: continue
            foldersToDelete.add(folder)

            // Collect bookmarks in this folder
            val folderBookmarks = bookmarkDao.getForFolderList(currentId)
            folderBookmarks.forEach { bookmarksToDeleteIds.add(it.id) }

            // Push children
            folderDao.getChildrenList(currentId).forEach { child ->
                stack.add(child.id)
            }
        }

        // Tombstones for bookmarks
        val now = Clock.System.now()
        bookmarksToDeleteIds.forEach { bookmarkId ->
            tombstoneDao.insert(
                TombstoneEntity(
                    tombstoneId = Uuid.random().toString(),
                    entityType = "bookmark",
                    entityId = bookmarkId,
                    timestamp = now,
                    deviceId = deviceId,
                )
            )
        }

        // Remove associations and delete bookmarks
        bookmarksToDeleteIds.forEach { bookmarkId ->
            bookmarkTagDao.deleteAllForBookmark(bookmarkId)
        }
        // Efficient delete via DAO deleteAll requires loading entities; we already loaded
        val bookmarkEntities = bookmarksToDeleteIds.mapNotNull { bookmarkDao.getById(it) }
        if (bookmarkEntities.isNotEmpty()) {
            bookmarkDao.deleteAll(bookmarkEntities)
        }

        // Tombstones for folders (bottom-up)
        foldersToDelete.forEach { folder ->
            tombstoneDao.insert(
                TombstoneEntity(
                    tombstoneId = Uuid.random().toString(),
                    entityType = "folder",
                    entityId = folder.id,
                    timestamp = now,
                    deviceId = deviceId,
                )
            )
        }

        // Delete folders bottom-up
        foldersToDelete.asReversed().forEach { folderDao.delete(it) }
    }
}


