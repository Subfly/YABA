package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.ReadableAssetUiModel
import dev.subfly.yabacore.model.ui.ReadableBlockUiModel
import dev.subfly.yabacore.model.ui.ReadableDocumentUiModel
import dev.subfly.yabacore.model.ui.ReadableListItemUiModel
import dev.subfly.yabacore.model.ui.ReadableVersionUiModel
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.unfurl.ReadableAsset
import dev.subfly.yabacore.unfurl.ReadableBlock
import dev.subfly.yabacore.unfurl.ReadableDocumentSnapshot
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
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * Filesystem-first manager for immutable readable content.
 *
 * Handles saving readable document snapshots and their assets.
 * Content is never overwritten - new saves create new versions.
 *
 * File layout:
 * - /bookmarks/<id>/content/readable/v1.json, v2.json, ...
 * - /bookmarks/<id>/content/assets/<assetId>.<ext>
 */
object ReadableContentManager {
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val readableAssetDao get() = DatabaseProvider.readableAssetDao
    private val highlightDao get() = DatabaseProvider.highlightDao
    private val accessProvider = FileAccessProvider
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

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
        // 1. Determine next content version
        val nextVersion = getNextContentVersion(bookmarkId)

        // 2. Update document with correct content version
        val document = readable.document.copy(
            contentVersion = nextVersion,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )

        // 3. Save assets first (immutable, never overwrite)
        val savedAssets = saveAssets(bookmarkId, readable.assets)

        // 4. Save the document JSON (immutable, never overwrite)
        saveDocumentVersion(bookmarkId, document)

        // 5. Update Room index
        updateRoomIndex(bookmarkId, document, savedAssets)
    }

    /**
     * Gets the next content version by scanning existing versions.
     */
    private suspend fun getNextContentVersion(bookmarkId: String): Int {
        val readableDir = CoreConstants.FileSystem.Linkmark.readableDir(bookmarkId)
        val dir = accessProvider.resolveRelativePath(readableDir, ensureParentExists = false)

        if (!dir.exists()) return 1

        val existingVersions = dir.list()
            .filter { it.name.startsWith("v") && it.name.endsWith(".json") }
            .mapNotNull { file ->
                file.name.removePrefix("v").removeSuffix(".json").toIntOrNull()
            }

        return (existingVersions.maxOrNull() ?: 0) + 1
    }

    /**
     * Saves the document JSON to filesystem.
     */
    private suspend fun saveDocumentVersion(bookmarkId: String, document: ReadableDocumentSnapshot) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(
            bookmarkId,
            document.contentVersion,
        )

        // Check if file already exists (immutable, never overwrite)
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
        if (file.exists()) return

        val jsonContent = json.encodeToString(document)
        file.write(jsonContent.encodeToByteArray())
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
        document: ReadableDocumentSnapshot,
        savedAssets: List<SavedAssetInfo>,
    ) {
        // Insert readable version
        val versionEntity = ReadableVersionEntity(
            id = "${bookmarkId}_v${document.contentVersion}",
            bookmarkId = bookmarkId,
            contentVersion = document.contentVersion,
            createdAt = document.createdAt,
            relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(
                bookmarkId,
                document.contentVersion,
            ),
            title = document.title,
            author = document.author,
        )
        readableVersionDao.upsert(versionEntity)

        // Insert assets with roles from document
        val assetRoles = extractAssetRoles(document)
        for (asset in savedAssets) {
            val role = assetRoles[asset.assetId] ?: ReadableAssetRole.INLINE
            val assetEntity = ReadableAssetEntity(
                id = asset.assetId,
                bookmarkId = bookmarkId,
                contentVersion = document.contentVersion,
                role = role,
                relativePath = asset.relativePath,
            )
            readableAssetDao.upsert(assetEntity)
        }
    }

    /**
     * Extracts asset roles from document blocks.
     */
    private fun extractAssetRoles(document: ReadableDocumentSnapshot): Map<String, ReadableAssetRole> {
        val roles = mutableMapOf<String, ReadableAssetRole>()

        fun processBlocks(blocks: List<ReadableBlock>) {
            for (block in blocks) {
                when (block) {
                    is ReadableBlock.Image -> {
                        roles[block.assetId] = block.role
                    }
                    is ReadableBlock.Quote -> {
                        processBlocks(block.children)
                    }
                    is ReadableBlock.ListBlock -> {
                        for (item in block.items) {
                            processBlocks(item.blocks)
                        }
                    }
                    else -> {}
                }
            }
        }

        processBlocks(document.blocks)
        return roles
    }

    /**
     * Reads a specific readable version from filesystem.
     */
    suspend fun readVersion(bookmarkId: String, contentVersion: Int): ReadableDocumentSnapshot? {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, contentVersion)
        val content = accessProvider.readText(relativePath) ?: return null
        return runCatching { json.decodeFromString<ReadableDocumentSnapshot>(content) }.getOrNull()
    }

    /**
     * Gets the latest content version number for a bookmark.
     */
    suspend fun getLatestVersion(bookmarkId: String): Int? {
        val readableDir = CoreConstants.FileSystem.Linkmark.readableDir(bookmarkId)
        val dir = accessProvider.resolveRelativePath(readableDir, ensureParentExists = false)

        if (!dir.exists()) return null

        return dir.list()
            .filter { it.name.startsWith("v") && it.name.endsWith(".json") }
            .mapNotNull { file ->
                file.name.removePrefix("v").removeSuffix(".json").toIntOrNull()
            }
            .maxOrNull()
    }

    fun observeReadableVersions(bookmarkId: String): Flow<List<ReadableVersionUiModel>> {
        return readableVersionDao.observeByBookmarkId(bookmarkId).map { versions ->
            coroutineScope {
                versions.map { versionEntity ->
                    async { buildReadableVersionUiModel(bookmarkId, versionEntity) }
                }.awaitAll().filterNotNull()
            }
        }
    }

    private suspend fun buildReadableVersionUiModel(
        bookmarkId: String,
        versionEntity: ReadableVersionEntity,
    ): ReadableVersionUiModel? {
        val document = readVersion(bookmarkId, versionEntity.contentVersion)
        val assets = readableAssetDao.getByBookmarkIdAndVersion(bookmarkId, versionEntity.contentVersion)
        val highlights = highlightDao.getByBookmarkIdAndVersion(bookmarkId, versionEntity.contentVersion)
        val assetPathMap = mutableMapOf<String, String?>()
        for (asset in assets) {
            assetPathMap[asset.id] = BookmarkFileManager.getAbsolutePath(asset.relativePath)
        }
        val assetsUi = mutableListOf<ReadableAssetUiModel>()
        for (assetEntity in assets) {
            assetsUi.add(
                ReadableAssetUiModel(
                    assetId = assetEntity.id,
                    role = assetEntity.role,
                    absolutePath = BookmarkFileManager.getAbsolutePath(assetEntity.relativePath),
                )
            )
        }
        val highlightsUi = mutableListOf<HighlightUiModel>()
        for (highlight in highlights) {
            highlightsUi.add(highlight.toUiModel())
        }

        return ReadableVersionUiModel(
            contentVersion = versionEntity.contentVersion,
            createdAt = versionEntity.createdAt,
            title = versionEntity.title,
            author = versionEntity.author,
            document = document?.let { buildReadableDocumentUiModel(it, assetPathMap) },
            assets = assetsUi,
            highlights = highlightsUi,
        )
    }

    private fun buildReadableDocumentUiModel(
        document: ReadableDocumentSnapshot,
        assetPathMap: Map<String, String?>,
    ): ReadableDocumentUiModel {
        return ReadableDocumentUiModel(
            title = document.title,
            author = document.author,
            blocks = document.blocks.map { block -> buildReadableBlockUiModel(block, assetPathMap) },
        )
    }

    private fun buildReadableBlockUiModel(
        block: ReadableBlock,
        assetPathMap: Map<String, String?>,
    ): ReadableBlockUiModel {
        return when (block) {
            is ReadableBlock.Paragraph -> ReadableBlockUiModel.Paragraph(
                id = block.id,
                inlines = block.inlines,
            )
            is ReadableBlock.Heading -> ReadableBlockUiModel.Heading(
                id = block.id,
                level = block.level,
                inlines = block.inlines,
            )
            is ReadableBlock.Image -> ReadableBlockUiModel.Image(
                id = block.id,
                assetId = block.assetId,
                assetPath = assetPathMap[block.assetId],
                role = block.role,
                caption = block.caption,
            )
            is ReadableBlock.Code -> ReadableBlockUiModel.Code(
                id = block.id,
                language = block.language,
                text = block.text,
            )
            is ReadableBlock.ListBlock -> ReadableBlockUiModel.ListBlock(
                id = block.id,
                ordered = block.ordered,
                items = block.items.map { item ->
                    ReadableListItemUiModel(
                        blocks = item.blocks.map { child -> buildReadableBlockUiModel(child, assetPathMap) }
                    )
                },
            )
            is ReadableBlock.Quote -> ReadableBlockUiModel.Quote(
                id = block.id,
                children = block.children.map { child -> buildReadableBlockUiModel(child, assetPathMap) },
            )
            is ReadableBlock.Divider -> ReadableBlockUiModel.Divider(id = block.id)
        }
    }

    private suspend fun HighlightEntity.toUiModel(): HighlightUiModel =
        HighlightUiModel(
            id = id,
            startBlockId = startBlockId,
            startInlinePath = startInlinePath.split(",").mapNotNull { it.toIntOrNull() },
            startOffset = startOffset,
            endBlockId = endBlockId,
            endInlinePath = endInlinePath.split(",").mapNotNull { it.toIntOrNull() },
            endOffset = endOffset,
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
