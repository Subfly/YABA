package dev.subfly.yabacore.database

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.utils.parseReadableMarkdownFrontmatter
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.EntityType
import dev.subfly.yabacore.filesystem.FileSystemStateManager
import dev.subfly.yabacore.filesystem.SyncState
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.HighlightJson
import dev.subfly.yabacore.filesystem.json.LinkJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.model.utils.YabaColor
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import kotlinx.serialization.json.Json

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
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val readableAssetDao get() = DatabaseProvider.readableAssetDao
    private val highlightDao get() = DatabaseProvider.highlightDao
    private val entityFileManager get() = EntityFileManager

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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

            // 6. Rebuild readable content indexes
            rebuildReadableContent()

            // 7. Rebuild highlight indexes
            rebuildHighlights()

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
                    EntityType.FOLDER -> folderDao.deleteById(entity.id)
                    EntityType.TAG -> tagDao.deleteById(entity.id)
                    EntityType.BOOKMARK -> bookmarkDao.deleteByIds(listOf(entity.id))
                }
            }

            // Remove entities that are in cache but missing in filesystem
            drift.missingInFilesystem.forEach { entity ->
                when (entity.type) {
                    EntityType.FOLDER -> folderDao.deleteById(entity.id)
                    EntityType.TAG -> tagDao.deleteById(entity.id)
                    EntityType.BOOKMARK -> bookmarkDao.deleteByIds(listOf(entity.id))
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
                                tagBookmarkDao.insert(
                                    TagBookmarkCrossRef(
                                        tagId = tagIdStr,
                                        bookmarkId = entity.id,
                                    )
                                )
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
        highlightDao.deleteAll()
        readableAssetDao.deleteAll()
        readableVersionDao.deleteAll()
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
                // Only add relationship if the tag exists and isn't deleted
                if (!entityFileManager.isTagDeleted(tagIdStr)) {
                    tagBookmarkDao.insert(
                        TagBookmarkCrossRef(
                            tagId = tagIdStr,
                            bookmarkId = bookmarkId,
                        )
                    )
                }
            }
        }
    }

    /**
     * Rebuilds the readable content version and asset indexes.
     */
    private suspend fun rebuildReadableContent() {
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        bookmarkIds.forEach { bookmarkId ->
            if (entityFileManager.isBookmarkDeleted(bookmarkId)) return@forEach

            // Scan readable versions
            val readableDir = CoreConstants.FileSystem.Linkmark.readableDir(bookmarkId)
            val readableDirFile = FileAccessProvider.resolveRelativePath(readableDir, ensureParentExists = false)

            if (readableDirFile.exists()) {
                readableDirFile.list().forEach { versionFile ->
                    val fileName = versionFile.name
                    if (fileName.startsWith("v") && fileName.endsWith(".md")) {
                        val versionNum = fileName.removePrefix("v").removeSuffix(".md").toIntOrNull()
                        if (versionNum != null) {
                            val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionNum)
                            val content = FileAccessProvider.readText(relativePath)
                            if (content != null) {
                                runCatching {
                                    val parsed = parseReadableMarkdownFrontmatter(content)
                                    val entity = ReadableVersionEntity(
                                        id = "${bookmarkId}_v$versionNum",
                                        bookmarkId = bookmarkId,
                                        contentVersion = versionNum,
                                        createdAt = 0L,
                                        relativePath = relativePath,
                                        title = parsed.title,
                                        author = parsed.author,
                                    )
                                    readableVersionDao.upsert(entity)
                                }
                            }
                        }
                    }
                }
                // Index all assets in assets/ for this bookmark with role INLINE (no document source when rebuilding from .md)
                indexAssetsForVersion(bookmarkId, emptyMap())
            }
        }
    }

    /**
     * Indexes assets for a specific content version.
     */
    private suspend fun indexAssetsForVersion(
        bookmarkId: String,
        assetRoles: Map<String, ReadableAssetRole>,
    ) {
        val assetsDir = CoreConstants.FileSystem.Linkmark.assetsDir(bookmarkId)
        val assetsDirFile = FileAccessProvider.resolveRelativePath(assetsDir, ensureParentExists = false)

        if (assetsDirFile.exists()) {
            assetsDirFile.list().forEach { assetFile ->
                val assetId = assetFile.name.substringBeforeLast(".")
                val extension = assetFile.extension

                if (assetId.isNotEmpty() && extension.isNotEmpty()) {
                    val role = assetRoles[assetId] ?: ReadableAssetRole.INLINE
                    val relativePath = CoreConstants.FileSystem.Linkmark.assetPath(bookmarkId, assetId, extension)

                    val entity = ReadableAssetEntity(
                        id = assetId,
                        bookmarkId = bookmarkId,
                        role = role,
                        relativePath = relativePath,
                    )
                    readableAssetDao.upsert(entity)
                }
            }
        }
    }

    /**
     * Rebuilds the highlight annotation indexes.
     */
    private suspend fun rebuildHighlights() {
        val bookmarkIds = entityFileManager.scanAllBookmarks()
        bookmarkIds.forEach { bookmarkId ->
            if (entityFileManager.isBookmarkDeleted(bookmarkId)) return@forEach

            // Use entityFileManager to read all highlights for this bookmark
            val highlights = entityFileManager.readAllHighlights(bookmarkId)
            highlights.forEach { highlight ->
                val entity = highlight.toHighlightEntity()
                highlightDao.upsert(entity)
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
            createdAt = createdAt,
            editedAt = editedAt,
            isHidden = isHidden,
        )

    private fun TagMetaJson.toTagEntity(): TagEntity =
        TagEntity(
            id = id,
            label = label,
            icon = icon,
            color = YabaColor.fromCode(colorCode),
            createdAt = createdAt,
            editedAt = editedAt,
            isHidden = isHidden,
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

    private fun LinkJson.toLinkBookmarkEntity(bookmarkId: String): LinkBookmarkEntity =
        LinkBookmarkEntity(
            bookmarkId = bookmarkId,
            url = url,
            domain = domain,
            linkType = LinkType.fromCode(linkTypeCode),
            videoUrl = videoUrl,
        )

    private fun HighlightJson.toHighlightEntity(): HighlightEntity {
        val relativePath = CoreConstants.FileSystem.Linkmark.highlightPath(bookmarkId, id)
        return HighlightEntity(
            id = id,
            bookmarkId = bookmarkId,
            contentVersion = contentVersion,
            startOffset = startOffset,
            endOffset = endOffset,
            colorRole = colorRole,
            note = note,
            relativePath = relativePath,
            createdAt = createdAt,
            editedAt = editedAt,
        )
    }
}
