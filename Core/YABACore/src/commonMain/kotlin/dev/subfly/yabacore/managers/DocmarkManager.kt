package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.DocBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.DocmarkFileManager
import dev.subfly.yabacore.model.ui.DocmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.DocmarkType
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
        val docmarkType = docMetaData?.type ?: DocmarkType.PDF
        val localDocumentAbsolutePath = resolveDocumentAbsolutePath(bookmarkId, docmarkType)

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
            metadataTitle = docMetaData?.metadataTitle,
            metadataDescription = docMetaData?.metadataDescription,
            metadataAuthor = docMetaData?.metadataAuthor,
            metadataDate = docMetaData?.metadataDate,
            metadataIdentifier = docMetaData?.metadataIdentifier,
            docmarkType = docmarkType,
            localDocumentPath = localDocumentAbsolutePath,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
        )
    }

    fun observeDocDetails(bookmarkId: String): Flow<DocBookmarkEntity?> =
        docBookmarkDao.observeByBookmarkId(bookmarkId)

    suspend fun resolveDocumentAbsolutePath(bookmarkId: String, type: DocmarkType): String? {
        val relativePath = DocmarkFileManager.getDocumentRelativePath(bookmarkId, type)
        BookmarkFileManager.find(relativePath) ?: return null
        return BookmarkFileManager.getAbsolutePath(relativePath)
    }

    fun createOrUpdateDocDetails(
        bookmarkId: String,
        summary: String? = null,
        docmarkType: DocmarkType? = null,
        metadataTitle: String? = null,
        metadataDescription: String? = null,
        metadataAuthor: String? = null,
        metadataDate: String? = null,
        metadataIdentifier: String? = null,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateDocDetails:$bookmarkId") {
            val previous = docBookmarkDao.getByBookmarkId(bookmarkId)
            val entity = DocBookmarkEntity(
                bookmarkId = bookmarkId,
                summary = summary?.takeIf { it.isNotBlank() } ?: previous?.summary,
                type = docmarkType ?: previous?.type ?: DocmarkType.PDF,
                metadataTitle = metadataTitle?.takeIf { it.isNotBlank() } ?: previous?.metadataTitle,
                metadataDescription =
                    metadataDescription?.takeIf { it.isNotBlank() } ?: previous?.metadataDescription,
                metadataAuthor = metadataAuthor?.takeIf { it.isNotBlank() } ?: previous?.metadataAuthor,
                metadataDate = metadataDate?.takeIf { it.isNotBlank() } ?: previous?.metadataDate,
                metadataIdentifier =
                    metadataIdentifier?.takeIf { it.isNotBlank() } ?: previous?.metadataIdentifier,
            )
            docBookmarkDao.upsert(entity)
        }
    }
}
