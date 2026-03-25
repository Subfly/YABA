package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.AnnotationEntity
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.model.ui.AnnotationUiModel
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
 * File layout:
 * - /bookmarks/<id>/readable/<versionId>.json (link readable + notemark readable mirror; rich-text document JSON)
 * - /bookmarks/<id>/assets/<assetId>.<ext>
 */
object ReadableContentManager {
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val readableAssetDao get() = DatabaseProvider.readableAssetDao
    private val annotationDao get() = DatabaseProvider.annotationDao
    private val accessProvider = FileAccessProvider

    fun saveReadableContent(bookmarkId: String, readable: ReadableUnfurl) {
        CoreOperationQueue.queue("SaveReadable:$bookmarkId") {
            saveReadableContentInternal(bookmarkId, readable)
        }
    }

    /**
     * PDF annotations still persist an [AnnotationEntity.readableVersionId] FK to [ReadableVersionEntity].
     * Docmarks that never received a readable snapshot therefore have no version row and the UI cannot
     * resolve a version id for annotation selection drafts (see [AnnotationEntity.readableVersionId]).
     * Creates a single minimal document JSON placeholder version when the bookmark has none yet.
     */
    fun ensureDocmarkAnnotationReadableVersionIfNeeded(bookmarkId: String) {
        CoreOperationQueue.queue("EnsureDocmarkReadable:$bookmarkId") {
            val existing = readableVersionDao.getByBookmarkId(bookmarkId)
            if (existing.isNotEmpty()) return@queue
            saveReadableContentInternal(
                bookmarkId = bookmarkId,
                readable = ReadableUnfurl(
                    documentJson = """{"type":"doc","content":[]}""",
                    assets = emptyList(),
                ),
            )
        }
    }

    /**
     * Writes or overwrites readable version JSON at `/readable/<versionId>.json` (notemark mirror,
     * linkmarks after embedding `yabaAnnotation` marks, etc.).
     */
    suspend fun syncNotemarkReadableMirror(
        bookmarkId: String,
        versionId: String,
        documentJson: String,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionId)
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
        file.write(documentJson.encodeToByteArray())

        val existing = readableVersionDao.getById(versionId)
        val createdAt = existing?.createdAt ?: Clock.System.now().toEpochMilliseconds()

        readableVersionDao.upsert(
            ReadableVersionEntity(
                id = versionId,
                bookmarkId = bookmarkId,
                createdAt = createdAt,
                relativePath = relativePath,
            ),
        )
    }

    internal suspend fun saveReadableContentInternal(bookmarkId: String, readable: ReadableUnfurl): String {
        val versionId = IdGenerator.newId()
        val createdAt = Clock.System.now().toEpochMilliseconds()

        val savedAssets = saveAssets(bookmarkId, readable.assets)
        saveJsonVersion(bookmarkId, versionId, readable.documentJson)
        updateRoomIndex(bookmarkId, versionId, createdAt, savedAssets)
        return versionId
    }

    private suspend fun saveJsonVersion(
        bookmarkId: String,
        versionId: String,
        documentJson: String,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionId)
        val file = accessProvider.resolveRelativePath(relativePath, ensureParentExists = true)
        if (file.exists()) return
        file.write(documentJson.encodeToByteArray())
    }

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

    private suspend fun updateRoomIndex(
        bookmarkId: String,
        versionId: String,
        createdAt: Long,
        savedAssets: List<SavedAssetInfo>,
    ) {
        val relativePath = CoreConstants.FileSystem.Linkmark.readableVersionPath(bookmarkId, versionId)
        val versionEntity = ReadableVersionEntity(
            id = versionId,
            bookmarkId = bookmarkId,
            createdAt = createdAt,
            relativePath = relativePath,
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

    suspend fun readVersionByPath(relativePath: String): String? =
        accessProvider.readText(relativePath)

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
        val annotationsFlow = annotationDao.observeByBookmarkId(bookmarkId)
        return combine(versionsFlow, annotationsFlow) { versions, _ ->
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
        val bodyContent = readVersionByPath(versionEntity.relativePath)
        val assets = readableAssetDao.getByBookmarkId(bookmarkId)
        val annotations = annotationDao.getByBookmarkId(bookmarkId, readableVersionId = versionEntity.id)

        val assetsUi = mutableListOf<ReadableAssetUiModel>()
        assets.forEach { entity ->
            assetsUi.add(
                ReadableAssetUiModel(
                    assetId = entity.id,
                    role = entity.role,
                    absolutePath = BookmarkFileManager.getAbsolutePath(entity.relativePath),
                ),
            )
        }

        val annotationsUi = annotations.map { it.toAnnotationUiModel() }

        return ReadableVersionUiModel(
            versionId = versionEntity.id,
            createdAt = versionEntity.createdAt,
            body = bodyContent,
            assets = assetsUi,
            annotations = annotationsUi,
        )
    }

    private fun AnnotationEntity.toAnnotationUiModel(): AnnotationUiModel =
        AnnotationUiModel(
            id = id,
            type = type,
            colorRole = colorRole,
            note = note,
            quoteText = quoteText,
            extrasJson = extrasJson,
            createdAt = createdAt,
            editedAt = editedAt,
        )

    private data class SavedAssetInfo(
        val assetId: String,
        val extension: String,
        val relativePath: String,
    )
}
