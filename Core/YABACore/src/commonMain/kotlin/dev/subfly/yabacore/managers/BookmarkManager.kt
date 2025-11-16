package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.model.BookmarkKind
import dev.subfly.yabacore.model.BookmarkSearchFilters
import dev.subfly.yabacore.model.LinkBookmark
import dev.subfly.yabacore.model.LinkType
import dev.subfly.yabacore.model.SortOrderType
import dev.subfly.yabacore.model.SortType
import dev.subfly.yabacore.operations.OpApplier
import dev.subfly.yabacore.operations.OperationKind
import dev.subfly.yabacore.operations.toOperationDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class BookmarkManager(
    private val bookmarkDao: BookmarkDao,
    private val opApplier: OpApplier,
) {
    private val clock = Clock.System

    fun observeFolderBookmarks(
        folderId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkBookmark>> =
        bookmarkDao
            .observeLinkBookmarksForFolder(folderId)
            .map { rows ->
                rows.map { it.toModel() }.sortBookmarks(sortType, sortOrder)
            }

    fun observeTagBookmarks(
        tagId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkBookmark>> =
        bookmarkDao
            .observeLinkBookmarksForTag(tagId)
            .map { rows ->
                rows.map { it.toModel() }.sortBookmarks(sortType, sortOrder)
            }

    fun searchBookmarksFlow(
        query: String,
        filters: BookmarkSearchFilters = BookmarkSearchFilters(),
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<LinkBookmark>> =
        bookmarkDao
            .observeLinkBookmarksSearch(query)
            .map { rows ->
                rows
                    .map { it.toModel() }
                    .filterBy(filters)
                    .sortBookmarks(sortType, sortOrder)
            }

    suspend fun createLinkBookmark(
        folderId: Uuid,
        url: String,
        label: String?,
        description: String?,
        linkType: LinkType,
    ): LinkBookmark {
        val now = clock.now()
        val bookmark =
            LinkBookmark(
                id = Uuid.random(),
                folderId = folderId,
                label = label?.takeIf { it.isNotBlank() } ?: url,
                description = description,
                createdAt = now,
                editedAt = now,
                url = url,
                domain = extractDomain(url),
                linkType = linkType,
                previewImageUrl = null,
                previewIconUrl = null,
                videoUrl = null,
                kind = BookmarkKind.LINK,
            )
        opApplier.applyLocal(listOf(bookmark.toOperationDraft(OperationKind.CREATE)))
        return bookmark
    }

    suspend fun updateBookmarkMeta(
        bookmarkId: Uuid,
        label: String?,
        description: String?,
    ) {
        val existing = bookmarkDao.getLinkBookmarkById(bookmarkId)?.toModel() ?: return
        val updated =
            existing.copy(
                label = label ?: existing.label,
                description = description ?: existing.description,
                editedAt = clock.now(),
            )
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
    }

    suspend fun moveBookmarksToFolder(
        bookmarkIds: List<Uuid>,
        targetFolderId: Uuid,
    ) {
        if (bookmarkIds.isEmpty()) return
        val now = clock.now()
        val drafts =
            bookmarkIds.mapNotNull { id ->
                bookmarkDao.getLinkBookmarkById(id)?.toModel()?.copy(
                    folderId = targetFolderId,
                    editedAt = now,
                )?.toOperationDraft(OperationKind.MOVE)
            }
        if (drafts.isNotEmpty()) {
            opApplier.applyLocal(drafts)
        }
    }

    suspend fun deleteBookmarks(ids: List<Uuid>) {
        if (ids.isEmpty()) return
        val now = clock.now()
        val drafts =
            ids.mapNotNull { id ->
                bookmarkDao.getLinkBookmarkById(id)?.toModel()?.copy(editedAt = now)
                    ?.toOperationDraft(OperationKind.DELETE)
            }
        if (drafts.isNotEmpty()) {
            opApplier.applyLocal(drafts)
        }
    }

    private fun List<LinkBookmark>.filterBy(filters: BookmarkSearchFilters): List<LinkBookmark> =
        this
            .filter { bookmark ->
                filters.folderIds?.let { bookmark.folderId in it } ?: true
            }
            .filter { bookmark ->
                filters.kinds?.let { bookmark.kind in it } ?: true
            }

    private fun List<LinkBookmark>.sortBookmarks(
        sortType: SortType,
        sortOrder: SortOrderType,
    ): List<LinkBookmark> {
        val comparator =
            when (sortType) {
                SortType.CREATED_AT -> compareBy<LinkBookmark> { it.createdAt }
                SortType.EDITED_AT -> compareBy { it.editedAt }
                SortType.LABEL -> compareBy { it.label.lowercase() }
                SortType.CUSTOM -> compareBy { it.editedAt }
            }
        val sorted = sortedWith(comparator)
        return if (sortOrder == SortOrderType.DESCENDING) sorted.reversed() else sorted
    }

    private fun extractDomain(url: String): String {
        val withoutProtocol = url.substringAfter("://", url)
        val candidate = withoutProtocol.substringBefore("/")
        return candidate.substringBefore("?").substringBefore("#")
    }
}
