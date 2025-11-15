package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.dao.BookmarkTagDao
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.dao.TombstoneDao
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.TombstoneEntity
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

@OptIn(ExperimentalUuidApi::class)
class BookmarkManager(
        private val bookmarkDao: BookmarkDao,
        private val bookmarkTagDao: BookmarkTagDao,
        private val folderDao: FolderDao,
        private val tombstoneDao: TombstoneDao,
) {

    data class BookmarkUpdate(
            val title: String? = null,
            val description: String? = null,
            val url: String? = null,
            val iconName: String? = null,
            val color: Int? = null,
            val folderId: String? = null,
            val tagIds: List<String>? = null,
    )

    fun getBookmarksFlow(query: String?): Flow<List<BookmarkEntity>> =
            if (query.isNullOrBlank()) bookmarkDao.getAllFlow() else bookmarkDao.searchFlow(query)

    suspend fun addBookmark(
            url: String,
            title: String,
            description: String = "",
            iconName: String = "bookmark-02",
            color: Int = 0,
            folderId: String? = null,
            tagIds: List<String> = emptyList(),
    ): BookmarkEntity {
        val now = Clock.System.now()

        val ensuredFolderId = folderId ?: ensureDefaultFolder()

        val entity =
                BookmarkEntity(
                        id = Uuid.random().toString(),
                        title = title,
                        description = description,
                        url = url,
                        iconName = iconName,
                        color = color,
                        folderId = ensuredFolderId,
                        createdAt = now,
                        editedAt = now,
                        version = 1,
                )

        bookmarkDao.insert(entity)
        if (tagIds.isNotEmpty()) {
            bookmarkTagDao.replaceTagsForBookmark(entity.id, tagIds)
        }
        return entity
    }

    suspend fun updateBookmark(id: String, updates: BookmarkUpdate) {
        val existing = bookmarkDao.getById(id) ?: return
        val now = Clock.System.now()

        val newFolderId = updates.folderId ?: existing.folderId
        val updated =
                existing.copy(
                        title = updates.title ?: existing.title,
                        description = updates.description ?: existing.description,
                        url = updates.url ?: existing.url,
                        iconName = updates.iconName ?: existing.iconName,
                        color = updates.color ?: existing.color,
                        folderId = newFolderId,
                        editedAt = now,
                        version = existing.version + 1,
                )

        bookmarkDao.update(updated)
        updates.tagIds?.let { bookmarkTagDao.replaceTagsForBookmark(id, it) }
    }

    suspend fun moveBookmark(id: String, folderId: String) {
        val existing = bookmarkDao.getById(id) ?: return
        val now = Clock.System.now()
        val updated =
                existing.copy(
                        folderId = folderId,
                        editedAt = now,
                        version = existing.version + 1,
                )
        bookmarkDao.update(updated)
    }

    suspend fun setTagsForBookmark(id: String, tagIds: List<String>) {
        bookmarkTagDao.replaceTagsForBookmark(id, tagIds)
        // Not incrementing bookmark version for tag-only change keeps metadata consistent
        // If you prefer, uncomment below to count tag associations as an edit:
        // val existing = bookmarkDao.getById(id) ?: return
        // bookmarkDao.update(existing.copy(editedAt = Clock.System.now(), version =
        // existing.version + 1))
    }

    suspend fun deleteBookmark(id: String, deviceId: String? = null) {
        val existing = bookmarkDao.getById(id) ?: return
        // Create tombstone first
        val tombstone =
                TombstoneEntity(
                        tombstoneId = Uuid.random().toString(),
                        entityType = "bookmark",
                        entityId = id,
                        timestamp = Clock.System.now(),
                        deviceId = deviceId,
                )
        tombstoneDao.insert(tombstone)

        // Remove associations then the entity
        bookmarkTagDao.deleteAllForBookmark(id)
        bookmarkDao.delete(existing)
    }

    private suspend fun ensureDefaultFolder(): String {
        val existing = folderDao.getById(CoreConstants.UNCATEGORIZED_FOLDER_ID)
        if (existing != null) return existing.id

        val now = Clock.System.now()
        val defaultFolder =
                dev.subfly.yabacore.database.entities.FolderEntity(
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
        folderDao.insert(defaultFolder)
        return defaultFolder.id
    }
}
