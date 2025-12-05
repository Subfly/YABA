@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object LinkmarkManager {
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao
    private val opApplier
        get() = OpApplier
    private val linkmarkFileManager
        get() = LinkmarkFileManager
    private val clock = Clock.System

    fun observeFolderLinkmarks(
        folderId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkmarkUiModel>> =
        bookmarkDao.observeLinkBookmarksForFolder(
            folderId,
            sortType.name,
            sortOrder.name
        ).map { rows -> rows.map { it.toLinkmarkUi() } }

    fun observeTagLinkmarks(
        tagId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkmarkUiModel>> =
        bookmarkDao.observeLinkBookmarksForTag(
            tagId,
            sortType.name,
            sortOrder.name
        ).map { rows ->
            rows.map { it.toLinkmarkUi() }
        }

    fun searchLinkmarksFlow(
        query: String,
        filters: BookmarkSearchFilters = BookmarkSearchFilters(),
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkmarkUiModel>> {
        val params = filters.toQueryParams()
        return bookmarkDao.observeLinkBookmarksSearch(
            query = query,
            kinds = params.kinds,
            applyKindFilter = params.applyKindFilter,
            folderIds = params.folderIds,
            applyFolderFilter = params.applyFolderFilter,
            tagIds = params.tagIds,
            applyTagFilter = params.applyTagFilter,
            sortType = sortType.name,
            sortOrder = sortOrder.name,
        ).map { rows -> rows.map { it.toLinkmarkUi() } }
    }

    suspend fun createLinkmark(linkmark: LinkmarkUiModel): LinkmarkUiModel {
        val now = clock.now()
        val domain = LinkBookmarkDomainModel(
            id = linkmark.id,
            folderId = linkmark.folderId,
            kind = BookmarkKind.LINK,
            label = linkmark.label.takeIf { it.isNotBlank() } ?: linkmark.url,
            createdAt = now,
            editedAt = now,
            viewCount = linkmark.viewCount,
            isPrivate = linkmark.isPrivate,
            isPinned = linkmark.isPinned,
            description = linkmark.description,
            url = linkmark.url,
            domain = extractDomain(linkmark.url),
            linkType = linkmark.linkType,
            previewImageUrl = linkmark.previewImageUrl,
            previewIconUrl = linkmark.previewIconUrl,
            videoUrl = linkmark.videoUrl,
        )
        opApplier.applyLocal(listOf(domain.toOperationDraft(OperationKind.CREATE)))
        return domain.toUiModel()
    }

    suspend fun updateLinkmark(linkmark: LinkmarkUiModel): LinkmarkUiModel? {
        val existing = bookmarkDao.getLinkBookmarkById(linkmark.id)?.toModel() ?: return null
        val now = clock.now()
        val updated = existing.copy(
            folderId = linkmark.folderId,
            label = linkmark.label.takeIf { it.isNotBlank() } ?: existing.label,
            description = linkmark.description,
            url = linkmark.url,
            domain = extractDomain(linkmark.url),
            linkType = linkmark.linkType,
            previewImageUrl = linkmark.previewImageUrl,
            previewIconUrl = linkmark.previewIconUrl,
            videoUrl = linkmark.videoUrl,
            editedAt = now,
            viewCount = linkmark.viewCount,
            isPrivate = linkmark.isPrivate,
            isPinned = linkmark.isPinned,
        )
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
        return updated.toUiModel()
    }

    suspend fun getLinkmarkDetail(bookmarkId: Uuid): LinkmarkUiModel? {
        val linkBookmark = bookmarkDao.getLinkBookmarkById(bookmarkId)?.toModel() ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(linkBookmark.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }
        return linkBookmark.toUiModel(folder = folder, tags = tags)
    }

    private fun LinkBookmarkWithRelations.toLinkmarkUi(): LinkmarkUiModel = toModel().toUiModel()

    suspend fun saveLinkImage(
        bookmarkId: Uuid,
        bytes: ByteArray,
        extension: String = "jpeg",
    ): PlatformFile = linkmarkFileManager.saveLinkImageBytes(bookmarkId, bytes, extension)

    suspend fun importLinkImageFromFile(
        bookmarkId: Uuid,
        source: PlatformFile,
    ): PlatformFile = linkmarkFileManager.importLinkImageFromFile(bookmarkId, source)

    suspend fun getLinkImageFile(bookmarkId: Uuid): PlatformFile? =
        linkmarkFileManager.getLinkImageFile(bookmarkId)

    suspend fun saveDomainIcon(
        bookmarkId: Uuid,
        bytes: ByteArray,
    ): PlatformFile = linkmarkFileManager.saveDomainIconBytes(bookmarkId, bytes)

    suspend fun importDomainIconFromFile(
        bookmarkId: Uuid,
        source: PlatformFile,
    ): PlatformFile = linkmarkFileManager.importDomainIconFromFile(bookmarkId, source)

    suspend fun getDomainIconFile(bookmarkId: Uuid): PlatformFile? =
        linkmarkFileManager.getDomainIconFile(bookmarkId)

    suspend fun clearLinkmarkFiles(bookmarkId: Uuid) {
        linkmarkFileManager.purgeLinkmarkFolder(bookmarkId)
    }

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }

    private data class BookmarkQueryParams(
        val kinds: List<BookmarkKind>,
        val applyKindFilter: Boolean,
        val folderIds: List<Uuid>,
        val applyFolderFilter: Boolean,
        val tagIds: List<Uuid>,
        val applyTagFilter: Boolean,
    )

    private fun BookmarkSearchFilters.toQueryParams(): BookmarkQueryParams {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        val folderSet = folderIds?.takeIf { it.isNotEmpty() }
        val tagSet = tagIds?.takeIf { it.isNotEmpty() }
        return BookmarkQueryParams(
            kinds = kindSet?.toList() ?: listOf(BookmarkKind.LINK),
            applyKindFilter = kindSet != null,
            folderIds = folderSet?.toList() ?: listOf(Uuid.NIL),
            applyFolderFilter = folderSet != null,
            tagIds = tagSet?.toList() ?: listOf(Uuid.NIL),
            applyTagFilter = tagSet != null,
        )
    }
}
