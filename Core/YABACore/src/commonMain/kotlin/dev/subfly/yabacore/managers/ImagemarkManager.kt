package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.ImageBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.ImagemarkFileManager
import dev.subfly.yabacore.model.ui.ImagemarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlin.time.Instant

/**
 * DB-first image bookmark manager.
 *
 * Image metadata (summary) is stored in Room via imageBookmarkDao.
 * The main image is stored on disk via ImagemarkFileManager and referenced by
 * BookmarkEntity.localImagePath.
 */
object ImagemarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val imageBookmarkDao get() = DatabaseProvider.imageBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao

    suspend fun getImagemarkDetail(bookmarkId: String): ImagemarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        if (bookmarkMetaData.kind != BookmarkKind.IMAGE) return null

        val imageMetaData = imageBookmarkDao.getByBookmarkId(bookmarkId)
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }

        return ImagemarkUiModel(
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
            summary = imageMetaData?.summary,
            localImagePath = localImageAbsolutePath,
            localIconPath = null,
            parentFolder = folder,
            tags = tags,
        )
    }

    suspend fun resolveImageAbsolutePath(bookmarkId: String): String? {
        val relativePath = ImagemarkFileManager.getImageRelativePath(bookmarkId)
            ?: return null
        return BookmarkFileManager.getAbsolutePath(relativePath)
    }

    fun createOrUpdateImageDetails(
        bookmarkId: String,
        summary: String? = null,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateImageDetails:$bookmarkId") {
            val entity = ImageBookmarkEntity(
                bookmarkId = bookmarkId,
                summary = summary?.takeIf { it.isNotBlank() },
            )
            imageBookmarkDao.upsert(entity)
        }
    }
}
