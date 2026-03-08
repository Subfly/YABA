package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.DocBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.DocmarkFileManager
import dev.subfly.yabacore.model.ui.DocmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

object DocmarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val docBookmarkDao get() = DatabaseProvider.docBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao

    suspend fun getDocmarkDetail(bookmarkId: String): DocmarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        if (bookmarkMetaData.kind != BookmarkKind.FILE) return null

        val docMetaData = docBookmarkDao.getByBookmarkId(bookmarkId)
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localIconAbsolutePath = bookmarkMetaData.localIconPath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localPdfAbsolutePath = resolvePdfAbsolutePath(bookmarkId)

        return DocmarkUiModel(
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
            summary = docMetaData?.summary,
            localPdfPath = localPdfAbsolutePath,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
        )
    }

    fun observeDocDetails(bookmarkId: String): Flow<DocBookmarkEntity?> =
        docBookmarkDao.observeByBookmarkId(bookmarkId)

    suspend fun resolvePdfAbsolutePath(bookmarkId: String): String? {
        val relativePath = DocmarkFileManager.getPdfRelativePath(bookmarkId)
        BookmarkFileManager.find(relativePath) ?: return null
        return BookmarkFileManager.getAbsolutePath(relativePath)
    }

    fun createOrUpdateDocDetails(
        bookmarkId: String,
        summary: String? = null,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateDocDetails:$bookmarkId") {
            val entity = DocBookmarkEntity(
                bookmarkId = bookmarkId,
                summary = summary?.takeIf { it.isNotBlank() },
            )
            docBookmarkDao.upsert(entity)
        }
    }
}
