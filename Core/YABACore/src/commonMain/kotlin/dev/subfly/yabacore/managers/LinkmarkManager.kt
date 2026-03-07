package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * DB-first link bookmark manager.
 *
 * Link metadata (url, domain, etc.) is stored in Room via linkBookmarkDao.
 */
object LinkmarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val highlightDao get() = DatabaseProvider.highlightDao

    suspend fun getBookmarkUrl(bookmarkId: String): String? =
        linkBookmarkDao.getByBookmarkId(bookmarkId)?.url

    suspend fun getLinkmarkDetail(bookmarkId: String): LinkmarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        val linkMetaData = linkBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localIconAbsolutePath = bookmarkMetaData.localIconPath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }

        return LinkmarkUiModel(
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
            url = linkMetaData.url,
            domain = linkMetaData.domain,
            linkType = linkMetaData.linkType,
            videoUrl = linkMetaData.videoUrl,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
            readableVersions = emptyList(),
        )
    }

    fun observeHighlights(bookmarkId: String): Flow<List<HighlightUiModel>> =
        highlightDao.observeByBookmarkId(bookmarkId)
            .map { highlights -> highlights.map { it.toUiModel() } }

    fun createOrUpdateLinkDetails(
        bookmarkId: String,
        url: String,
        domain: String? = null,
        linkType: LinkType,
        videoUrl: String?,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateLinkDetails:$bookmarkId") {
            val resolvedDomain = domain?.takeIf { it.isNotBlank() } ?: extractDomain(url)
            val entity = LinkBookmarkEntity(
                bookmarkId = bookmarkId,
                url = url,
                domain = resolvedDomain,
                linkType = linkType,
                videoUrl = videoUrl,
            )
            linkBookmarkDao.upsert(entity)
        }
    }

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }

    private fun HighlightEntity.toUiModel(): HighlightUiModel =
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
}
