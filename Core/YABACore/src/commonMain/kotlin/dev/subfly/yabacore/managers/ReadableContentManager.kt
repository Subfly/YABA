package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.utils.parseReadableMarkdownFrontmatter
import dev.subfly.yabacore.model.utils.writeReadableMarkdownWithFrontmatter
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.ReadableAssetUiModel
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.unfurl.ReadableAsset
import dev.subfly.yabacore.unfurl.ReadableUnfurl
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Filesystem-first manager for immutable readable content.
 *
 * Handles saving readable document snapshots and their assets.
 * Content is never overwritten - new saves create new versions.
 *
 * File layout:
 * - /bookmarks/<id>/content/readable/v1.md, v2.md, ...
 * - /bookmarks/<id>/content/assets/<assetId>.<ext>
 */
object ReadableContentManager {
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val readableAssetDao get() = DatabaseProvider.readableAssetDao
    private val highlightDao get() = DatabaseProvider.highlightDao
    private val accessProvider = FileAccessProvider

    /**
     * Saves a new readable content version for a bookmark.
     *
     * This operation is enqueued to ensure proper ordering with other filesystem operations.
     *
     * @param bookmarkId The bookmark ID
     * @param readable The readable content from Unfurler
     */
    fun saveReadableContent(bookmarkId: String, readable: ReadableUnfurl) {
        CoreOperationQueue.queue("SaveReadable:$bookmarkId") {
            saveReadableContentInternal(bookmarkId, readable)
        }
    }

    /**
     * Internal implementation for saving readable content.
     */
    internal suspend fun saveReadableContentInternal(bookmarkId: String, readable: ReadableUnfurl) {
        val nextVersion = getNextContentVersion(bookmarkId)
        val createdAt = Clock.System.now().toEpochMilliseconds()

        val savedAssets = saveAssets(bookmarkId, readable.assets)
        saveMarkdownVersion(bookmarkId, nextVersion, readable.markdown, readable.title, readable.author)
        updateRoomIndex(bookmarkId, nextVersion, createdAt, readable.title, readable.author, savedAssets)
    }

    /**
     * Gets the next content version by scanning existing versions.
     */
    private suspend fun getNextContentVersion(bookmarkId: String): Int {
        val readableDir = CoreConstants.FileSystem.Linkmark.readableDir(bookmarkId)
        val dir = accessProvider.resolveRelativePath(readableDir, ensureParentExists = false)

        if (!dir.exists()) return 1

        val existingVersions = dir.list()
            .filter { it.name.startsWith("v") && it.name.endsWith(".md") }
            .mapNotNull { file ->
                file.name.removePrefix("v").removeSuffix(".md").toIntOrNull()
            }

        return (existingVersions.maxOrNull() ?: 0) + 1
    }

    /**
     * Saves the markdown content to filesystem with optional YAML frontmatter (title, author).
     * Frontmatter allows CacheRebuilder to restore title/author when rebuilding from .md.
     */
    private suspend fun saveMarkdownVersion(
        bookmarkId: String,
        contentVersion: Int,
        markdown: String,
        title: String?,
        author: String?,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(
            bookmarkId,
            contentVersion,
        )

        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
        if (file.exists()) return

        val content = writeReadableMarkdownWithFrontmatter(markdown, title, author)
        file.write(content.encodeToByteArray())
    }

    /**
     * Saves assets to filesystem, returning info about saved assets.
     */
    private suspend fun saveAssets(
        bookmarkId: String,
        assets: List<ReadableAsset>,
    ): List<SavedAssetInfo> {
        return assets.map { asset ->
            val relativePath = CoreConstants.FileSystem.Linkmark.assetPath(
                bookmarkId,
                asset.assetId,
                asset.extension,
            )

            // Check if file already exists (immutable, never overwrite)
            val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
            if (!file.exists()) {
                file.write(asset.bytes)
            }

            SavedAssetInfo(
                assetId = asset.assetId,
                extension = asset.extension,
                relativePath = relativePath,
            )
        }
    }

    /**
     * Updates Room index with the new readable version and assets.
     */
    private suspend fun updateRoomIndex(
        bookmarkId: String,
        contentVersion: Int,
        createdAt: Long,
        title: String?,
        author: String?,
        savedAssets: List<SavedAssetInfo>,
    ) {
        val versionEntity = ReadableVersionEntity(
            id = "${bookmarkId}_v$contentVersion",
            bookmarkId = bookmarkId,
            contentVersion = contentVersion,
            createdAt = createdAt,
            relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(
                bookmarkId,
                contentVersion,
            ),
            title = title,
            author = author,
        )
        readableVersionDao.upsert(versionEntity)

        for (asset in savedAssets) {
            val assetEntity = ReadableAssetEntity(
                id = asset.assetId,
                bookmarkId = bookmarkId,
                role = ReadableAssetRole.INLINE,
                relativePath = asset.relativePath,
            )
            readableAssetDao.upsert(assetEntity)
        }
    }

    /**
     * Reads a specific readable version from filesystem as raw markdown.
     * Strips YAML frontmatter so the UI receives only the body.
     */
    suspend fun readVersion(bookmarkId: String, contentVersion: Int): String? {
        val relativePath =
            CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, contentVersion)
        val raw = accessProvider.readText(relativePath) ?: return null
        return parseReadableMarkdownFrontmatter(raw).body
    }

    fun observeReadableVersions(bookmarkId: String): Flow<List<ReadableVersionUiModel>> {
        return readableVersionDao.observeByBookmarkId(bookmarkId).map { versions ->
            coroutineScope {
                versions.map { versionEntity ->
                    async { buildReadableVersionUiModel(bookmarkId, versionEntity) }
                }.awaitAll()
            }
        }
    }

    private suspend fun buildReadableVersionUiModel(
        bookmarkId: String,
        versionEntity: ReadableVersionEntity,
    ): ReadableVersionUiModel {
        val markdownContent = readVersion(bookmarkId, versionEntity.contentVersion)
        val assets = readableAssetDao.getByBookmarkId(bookmarkId)
        val highlights = highlightDao.getById(bookmarkId)

        val assetsUi = mutableListOf<ReadableAssetUiModel>()
        assets.forEach { entity ->
            assetsUi.add(
                ReadableAssetUiModel(
                    assetId = entity.id,
                    role = entity.role,
                    absolutePath = BookmarkFileManager.getAbsolutePath(entity.relativePath),
                )
            )
        }

        val highlightsUi = mutableListOf<HighlightUiModel>()
        highlights?.let { nonNullHighlights ->
            highlightsUi.add(nonNullHighlights.toUiModel())
        }

        return ReadableVersionUiModel(
            contentVersion = versionEntity.contentVersion,
            createdAt = versionEntity.createdAt,
            title = versionEntity.title,
            author = versionEntity.author,
            markdown = markdownContent,
            assets = assetsUi,
            highlights = highlightsUi,
        )
    }

    private suspend fun HighlightEntity.toUiModel(): HighlightUiModel =
        HighlightUiModel(
            id = id,
            startSectionKey = startSectionKey,
            startOffsetInSection = startOffsetInSection,
            endSectionKey = endSectionKey,
            endOffsetInSection = endOffsetInSection,
            colorRole = colorRole,
            note = note,
            absolutePath = BookmarkFileManager.getAbsolutePath(relativePath),
            createdAt = createdAt,
            editedAt = editedAt,
        )

    private data class SavedAssetInfo(
        val assetId: String,
        val extension: String,
        val relativePath: String,
    )
}
