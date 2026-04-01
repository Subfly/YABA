package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.CanvasBookmarkEntity
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.CanvmarkFileManager
import dev.subfly.yaba.core.filesystem.EMPTY_CANVAS_SCENE_JSON
import dev.subfly.yaba.core.model.ui.CanvmarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

object CanvmarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val canvasBookmarkDao get() = DatabaseProvider.canvasBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao

    suspend fun getCanvmarkDetail(bookmarkId: String): CanvmarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        if (bookmarkMetaData.kind != BookmarkKind.CANVAS) return null

        val canvasMeta = canvasBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localIconAbsolutePath = bookmarkMetaData.localIconPath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }

        return CanvmarkUiModel(
            id = bookmarkMetaData.id,
            folderId = bookmarkMetaData.folderId,
            kind = bookmarkMetaData.kind,
            label = bookmarkMetaData.label,
            description = bookmarkMetaData.description,
            createdAt = Instant.fromEpochMilliseconds(bookmarkMetaData.createdAt),
            editedAt = Instant.fromEpochMilliseconds(bookmarkMetaData.editedAt),
            viewCount = bookmarkMetaData.viewCount,
            isPrivate = bookmarkMetaData.isPrivate,
            isPinned = bookmarkMetaData.isPinned,
            sceneRelativePath = canvasMeta.sceneRelativePath,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
        )
    }

    fun observeCanvasDetails(bookmarkId: String): Flow<CanvasBookmarkEntity?> =
        canvasBookmarkDao.observeByBookmarkId(bookmarkId)

    suspend fun readCanvasSceneJson(bookmarkId: String): String? {
        val canvas = canvasBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        return CanvmarkFileManager.readSceneByRelativePath(canvas.sceneRelativePath)
    }

    fun saveCanvasSceneJson(
        bookmarkId: String,
        sceneJson: String,
        touchEditedAt: Boolean = true,
    ) {
        CoreOperationQueue.queue("SaveCanvmarkScene:$bookmarkId") {
            saveCanvasSceneJsonInternal(bookmarkId, sceneJson, touchEditedAt)
        }
    }

    suspend fun persistCanvasSceneJsonAwait(
        bookmarkId: String,
        sceneJson: String,
        touchEditedAt: Boolean = true,
    ): Result<Unit> =
        CoreOperationQueue.queueAndAwait("SaveCanvmarkScene:$bookmarkId") {
            saveCanvasSceneJsonInternal(bookmarkId, sceneJson, touchEditedAt)
        }

    private suspend fun saveCanvasSceneJsonInternal(
        bookmarkId: String,
        sceneJson: String,
        touchEditedAt: Boolean,
    ) {
        val canvas = canvasBookmarkDao.getByBookmarkId(bookmarkId) ?: return
        BookmarkFileManager.writeBytes(canvas.sceneRelativePath, sceneJson.encodeToByteArray())
        if (touchEditedAt) {
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    fun createOrUpdateCanvasDetails(
        bookmarkId: String,
        sceneRelativePath: String = CanvmarkFileManager.sceneRelativePath(bookmarkId),
    ) {
        CoreOperationQueue.queue("CreateOrUpdateCanvasDetails:$bookmarkId") {
            createOrUpdateCanvasDetailsInternal(
                bookmarkId = bookmarkId,
                sceneRelativePath = sceneRelativePath,
            )
        }
    }

    private suspend fun createOrUpdateCanvasDetailsInternal(
        bookmarkId: String,
        sceneRelativePath: String,
    ) {
        CanvmarkFileManager.ensureEmptyScene(bookmarkId)
        val existing = CanvmarkFileManager.readScene(bookmarkId).orEmpty()
        if (existing.isBlank()) {
            CanvmarkFileManager.writeScene(bookmarkId, EMPTY_CANVAS_SCENE_JSON)
        }
        canvasBookmarkDao.upsert(
            CanvasBookmarkEntity(
                bookmarkId = bookmarkId,
                sceneRelativePath = sceneRelativePath,
            ),
        )
    }
}
