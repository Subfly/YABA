@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database

import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.EntityType
import dev.subfly.yabacore.filesystem.FileSystemStateManager
import dev.subfly.yabacore.filesystem.SyncState
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Rebuilds the SQLite cache from the filesystem.
 *
 * The correctness test for this system is:
 * 1. Delete `yaba.sqlite`
 * 2. Delete `events.sqlite`
 * 3. Call `CacheRebuilder.rebuildFromFilesystem()`
 * 4. System reconstructs correctly from filesystem alone
 *
 * This is the key mechanism that proves filesystem is the source of truth.
 */
object CacheRebuilder {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val entityFileManager get() = EntityFileManager

    /**
     * Rebuilds the entire SQLite cache from the filesystem.
     *
     * This clears all tables and repopulates from JSON files.
     * Only non-deleted entities are restored.
     */
    suspend fun rebuildFromFilesystem() {
        FileSystemStateManager.setSyncState(SyncState.SYNCING)

        try {
            // 1. Clear all SQLite tables
            clearAllTables()

            // 2. Scan and restore folders
            rebuildFolders()

            // 3. Scan and restore tags
            rebuildTags()

            // 4. Scan and restore bookmarks (and link details)
            rebuildBookmarks()

            // 5. Rebuild tag-bookmark relationships
            rebuildTagBookmarkRelationships()

            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
        } catch (e: Exception) {
            FileSystemStateManager.setSyncState(SyncState.SYNC_FAILED)
            throw e
        }
    }

    /**
     * Incremental sync - only fix detected drift.
     */
    suspend fun fixDrift() {
        val drift = FileSystemStateManager.detectDrift()

        if (!drift.hasDrift) {
            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
            return
        }

        FileSystemStateManager.setSyncState(SyncState.SYNCING)

        try {
            // Remove entities that are deleted in filesystem but still in cache
            drift.deletedButInCache.forEach { entity ->
                when (entity.type) {
                    EntityType.FOLDER -> folderDao.deleteById(entity.id.toString())
                    EntityType.TAG -> tagDao.deleteById(entity.id.toString())
                    EntityType.BOOKMARK -> bookmarkDao.deleteByIds(listOf(entity.id.toString()))
                }
            }

            // Remove entities that are in cache but missing in filesystem
            drift.missingInFilesystem.forEach { entity ->
                when (entity.type) {
                    EntityType.FOLDER -> folderDao.deleteById(entity.id.toString())
                    EntityType.TAG -> tagDao.deleteById(entity.id.toString())
                    EntityType.BOOKMARK -> bookmarkDao.deleteByIds(listOf(entity.id.toString()))
                }
            }

            // Add entities that are in filesystem but missing in cache
            drift.missingInCache.forEach { entity ->
                when (entity.type) {
                    EntityType.FOLDER -> {
                        val meta = entityFileManager.readFolderMeta(entity.id)
                        if (meta != null && !entityFileManager.isFolderDeleted(entity.id)) {
                            folderDao.upsert(meta.toFolderEntity())
                        }
                    }

                    EntityType.TAG -> {
                        val meta = entityFileManager.readTagMeta(entity.id)
                        if (meta != null && !entityFileManager.isTagDeleted(entity.id)) {
                            tagDao.upsert(meta.toTagEntity())
                        }
                    }

                    EntityType.BOOKMARK -> {
                        val meta = entityFileManager.readBookmarkMeta(entity.id)
                        if (meta != null && !entityFileManager.isBookmarkDeleted(entity.id)) {
                            bookmarkDao.upsert(meta.toBookmarkEntity())

                            // Also restore link details if present
                            val linkJson = entityFileManager.readLinkJson(entity.id)
                            if (linkJson != null) {
                                linkBookmarkDao.upsert(linkJson.toLinkBookmarkEntity(entity.id))
                            }

                            // Restore tag relationships
                            meta.tagIds.forEach { tagIdStr ->
                                val tagId = runCatching { Uuid.parse(tagIdStr) }.getOrNull()
                                if (tagId != null) {
                                    tagBookmarkDao.insert(
                                        TagBookmarkCrossRef(
                                            tagId = tagIdStr,
                                            bookmarkId = entity.id.toString(),
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
        } catch (e: Exception) {
            FileSystemStateManager.setSyncState(SyncState.SYNC_FAILED)
            throw e
        }
    }

    // ==================== Private Helpers ====================

    private suspend fun clearAllTables() {
        tagBookmarkDao.deleteAll()
        linkBookmarkDao.deleteAll()
        bookmarkDao.deleteAll()
        tagDao.deleteAll()
        folderDao.deleteAll()
    }

    private suspend fun rebuildFolders() {
        val folderIds = entityFileManager.scanAllFolders()
        folderIds.forEach { folderId ->
            if (entityFileManager.isFolderDeleted(folderId)) return@forEach
            val meta = entityFileManager.readFolderMeta(folderId) ?: return@forEach
            folderDao.upsert(meta.toFolderEntity())
        }
    }

    private suspend fun rebuildTags() {
        val tagIds = entityFileManager.scanAllTags()
        tagIds.forEach { tagId ->
            if (entityFileManager.isTagDeleted(tagId)) return@forEach
            val meta = entityFileManager.readTagMeta(tagId) ?: return@forEach
            tagDao.upsert(meta.toTagEntity())
        }
    }

    private suspend fun rebuildBookmarks() {
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        bookmarkIds.forEach { bookmarkId ->
            if (entityFileManager.isBookmarkDeleted(bookmarkId)) return@forEach
            val meta = entityFileManager.readBookmarkMeta(bookmarkId) ?: return@forEach
            bookmarkDao.upsert(meta.toBookmarkEntity())

            // Also restore link details if present
            val linkJson = entityFileManager.readLinkJson(bookmarkId)
            if (linkJson != null) {
                linkBookmarkDao.upsert(linkJson.toLinkBookmarkEntity(bookmarkId))
            }
        }
    }

    private suspend fun rebuildTagBookmarkRelationships() {
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        bookmarkIds.forEach { bookmarkId ->
            if (entityFileManager.isBookmarkDeleted(bookmarkId)) return@forEach
            val meta = entityFileManager.readBookmarkMeta(bookmarkId) ?: return@forEach

            meta.tagIds.forEach { tagIdStr ->
                val tagId = runCatching { Uuid.parse(tagIdStr) }.getOrNull() ?: return@forEach
                // Only add relationship if the tag exists and isn't deleted
                if (!entityFileManager.isTagDeleted(tagId)) {
                    tagBookmarkDao.insert(
                        TagBookmarkCrossRef(
                            tagId = tagIdStr,
                            bookmarkId = bookmarkId.toString(),
                        )
                    )
                }
            }
        }
    }

    // ==================== Mappers ====================

    private fun FolderMetaJson.toFolderEntity(): FolderEntity =
        FolderEntity(
            id = id,
            parentId = parentId,
            label = label,
            description = description,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            order = order,
            createdAt = createdAt,
            editedAt = editedAt,
        )

    private fun TagMetaJson.toTagEntity(): TagEntity =
        TagEntity(
            id = id,
            label = label,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            order = order,
            createdAt = createdAt,
            editedAt = editedAt,
        )

    private fun BookmarkMetaJson.toBookmarkEntity(): BookmarkEntity =
        BookmarkEntity(
            id = id,
            folderId = folderId,
            kind = BookmarkKind.fromCode(kind),
            label = label,
            description = description,
            createdAt = createdAt,
            editedAt = editedAt,
            viewCount = viewCount,
            isPrivate = isPrivate,
            isPinned = isPinned,
            localImagePath = localImagePath,
            localIconPath = localIconPath,
        )

    private fun LinkJson.toLinkBookmarkEntity(bookmarkId: Uuid): LinkBookmarkEntity =
        LinkBookmarkEntity(
            bookmarkId = bookmarkId.toString(),
            url = url,
            domain = domain,
            linkType = LinkType.fromCode(linkTypeCode),
            videoUrl = videoUrl,
        )
}
