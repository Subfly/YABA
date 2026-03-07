package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
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
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Filesystem-first manager for immutable readable content.
 *
 * Handles saving readable document snapshots and their assets.
 * Content is never overwritten - new saves create new versions.
 *
 * File layout:
 * - /bookmarks/<id>/readable/<versionId>.md
 * - /bookmarks/<id>/assets/<assetId>.<ext>
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
     * @return The created version ID
     */
    internal suspend fun saveReadableContentInternal(bookmarkId: String, readable: ReadableUnfurl): String {
        val versionId = IdGenerator.newId()
        val createdAt = Clock.System.now().toEpochMilliseconds()

        val savedAssets = saveAssets(bookmarkId, readable.assets)
        saveMarkdownVersion(bookmarkId, versionId, readable.markdown, readable.title, readable.author)
        updateRoomIndex(bookmarkId, versionId, createdAt, readable.title, readable.author, savedAssets)
        return versionId
    }

    /**
     * Saves the markdown content to filesystem with optional YAML frontmatter (title, author).
     * Frontmatter stores title/author for display.
     */
    private suspend fun saveMarkdownVersion(
        bookmarkId: String,
        versionId: String,
        markdown: String,
        title: String?,
        author: String?,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionId)

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
        versionId: String,
        createdAt: Long,
        title: String?,
        author: String?,
        savedAssets: List<SavedAssetInfo>,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionId)
        val versionEntity = ReadableVersionEntity(
            id = versionId,
            bookmarkId = bookmarkId,
            createdAt = createdAt,
            relativePath = relativePath,
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
    suspend fun readVersionByPath(relativePath: String): String? {
        val raw = accessProvider.readText(relativePath) ?: return null
        return parseReadableMarkdownFrontmatter(raw).body
    }

    /**
     * Deletes a readable version and its markdown file.
     * Highlights for this version are cascade-deleted via FK.
     */
    fun deleteVersion(versionId: String) {
        CoreOperationQueue.queue("DeleteReadableVersion:$versionId") {
            deleteVersionInternal(versionId)
        }
    }

    internal suspend fun deleteVersionInternal(versionId: String) {
        val entity = readableVersionDao.getById(versionId) ?: return
        accessProvider.delete(entity.relativePath)
        readableVersionDao.deleteById(versionId)
    }

    fun observeReadableVersions(bookmarkId: String): Flow<List<ReadableVersionUiModel>> {
        val versionsFlow = readableVersionDao.observeByBookmarkId(bookmarkId)
        val highlightsFlow = highlightDao.observeByBookmarkId(bookmarkId, readableVersionId = null)
        return combine(versionsFlow, highlightsFlow) { versions, _ ->
            versions
        }.map { versions ->
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
        val markdownContent = readVersionByPath(versionEntity.relativePath)
        val assets = readableAssetDao.getByBookmarkId(bookmarkId)
        val highlights = highlightDao.getByBookmarkId(bookmarkId, readableVersionId = versionEntity.id)

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

        val highlightsUi = highlights.map { it.toHighlightUiModel() }

        return ReadableVersionUiModel(
            versionId = versionEntity.id,
            createdAt = versionEntity.createdAt,
            title = versionEntity.title,
            author = versionEntity.author,
            markdown = markdownContent,
            assets = assetsUi,
            highlights = highlightsUi,
        )
    }

    private fun HighlightEntity.toHighlightUiModel(): HighlightUiModel =
        HighlightUiModel(
            id = id,
            startSectionKey = startSectionKey,
            startOffsetInSection = startOffsetInSection,
            endSectionKey = endSectionKey,
            endOffsetInSection = endOffsetInSection,
            colorRole = colorRole,
            note = note,
            quoteText = quoteText,
            absolutePath = null,
            createdAt = createdAt,
            editedAt = editedAt,
        )

    private data class SavedAssetInfo(
        val assetId: String,
        val extension: String,
        val relativePath: String,
    )
}
