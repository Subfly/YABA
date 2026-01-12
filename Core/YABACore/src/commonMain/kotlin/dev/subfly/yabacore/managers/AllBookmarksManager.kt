@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.LinkBookmarkDomainModel
import dev.subfly.yabacore.database.mappers.toDomain
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.tagLinkOperationDraft
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.LinkmarkUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import io.github.vinceglb.filekit.path
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AllBookmarksManager {
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val opApplier
        get() = OpApplier
    private val bookmarkFileManager
        get() = BookmarkFileManager
    private val clock = Clock.System

    suspend fun moveBookmarksToFolder(
        bookmarks: List<BookmarkUiModel>,
        targetFolder: FolderUiModel,
    ) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts = bookmarks.mapNotNull { bookmark ->
            bookmark.toDomainBookmark()
                ?.copy(folderId = targetFolder.id, editedAt = now)
                ?.toOperationDraft(OperationKind.MOVE)
        }
        if (drafts.isEmpty()) return
        opApplier.applyLocal(drafts)
    }

    suspend fun deleteBookmarks(bookmarks: List<BookmarkUiModel>) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts = bookmarks.mapNotNull { bookmark ->
            bookmark.toDomainBookmark()
                ?.copy(editedAt = now)
                ?.toOperationDraft(OperationKind.DELETE)
        }
        if (drafts.isEmpty()) return
        opApplier.applyLocal(drafts)
        bookmarks.forEach { bookmark ->
            bookmarkFileManager.deleteBookmarkTree(bookmark.id)
        }
    }

    suspend fun addTagToBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val draft = tagLinkOperationDraft(
            tag.id,
            bookmark.id,
            OperationKind.TAG_ADD,
            clock.now()
        )
        opApplier.applyLocal(listOf(draft))
    }

    suspend fun removeTagFromBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val draft = tagLinkOperationDraft(
            tag.id,
            bookmark.id,
            OperationKind.TAG_REMOVE,
            clock.now()
        )
        opApplier.applyLocal(listOf(draft))
    }

    fun observeAllBookmarks(
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
        kinds: List<BookmarkKind>? = null,
    ): Flow<List<BookmarkUiModel>> {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        return bookmarkDao
            .observeAllBookmarks(
                kinds = kindSet ?: listOf(BookmarkKind.LINK),
                applyKindFilter = kindSet != null,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
            )
            .map { rows -> rows.mapNotNull { it.toBookmarkUiModel() } }
    }

    fun observeFolderBookmarks(
        folderId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
        kinds: List<BookmarkKind>? = null,
    ): Flow<List<BookmarkUiModel>> {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        return bookmarkDao
            .observeBookmarksForFolder(
                folderId = folderId.toString(),
                kinds = kindSet ?: listOf(BookmarkKind.LINK),
                applyKindFilter = kindSet != null,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
            )
            .map { rows -> rows.mapNotNull { it.toBookmarkUiModel() } }
    }

    fun observeTagBookmarks(
        tagId: Uuid,
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
        kinds: List<BookmarkKind>? = null,
    ): Flow<List<BookmarkUiModel>> {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        return bookmarkDao
            .observeBookmarksForTag(
                tagId = tagId.toString(),
                kinds = kindSet ?: listOf(BookmarkKind.LINK),
                applyKindFilter = kindSet != null,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
            )
            .map { rows -> rows.mapNotNull { it.toBookmarkUiModel() } }
    }

    fun searchBookmarksFlow(
        query: String,
        filters: BookmarkSearchFilters = BookmarkSearchFilters(),
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<BookmarkUiModel>> {
        val params = filters.toQueryParams()
        return bookmarkDao
            .observeBookmarksSearch(
                query = query,
                kinds = params.kinds,
                applyKindFilter = params.applyKindFilter,
                folderIds = params.folderIds,
                applyFolderFilter = params.applyFolderFilter,
                tagIds = params.tagIds,
                applyTagFilter = params.applyTagFilter,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
            )
            .map { rows -> rows.mapNotNull { it.toBookmarkUiModel() } }
    }

    suspend fun getBookmarkDetail(bookmarkId: Uuid): BookmarkUiModel? =
        bookmarkDao.getBookmarkWithRelationsById(bookmarkId.toString())?.toBookmarkUiModel()

    private suspend fun BookmarkWithRelations.toBookmarkUiModel(): BookmarkUiModel? {
        val folderUi = folder.toModel().toUiModel()
        val tagsUi = tags.sortedBy { it.order }.map { it.toModel().toUiModel() }
        val bookmarkId = Uuid.parse(bookmark.id)

        val localImageAbsolutePath =
            bookmark.localImagePath?.let { relativePath ->
                bookmarkFileManager.resolve(relativePath).path
            } ?: LinkmarkFileManager.getLinkImageFile(bookmarkId)?.path

        val localIconAbsolutePath =
            bookmark.localIconPath?.let { relativePath ->
                bookmarkFileManager.resolve(relativePath).path
            } ?: LinkmarkFileManager.getDomainIconFile(bookmarkId)?.path

        return when (bookmark.kind) {
            BookmarkKind.LINK -> {
                val linkEntity = link ?: return null
                bookmark
                    .toModel(linkEntity)
                    .toUiModel(
                        folder = folderUi,
                        tags = tagsUi,
                        localImagePath = localImageAbsolutePath,
                        localIconPath = localIconAbsolutePath,
                    )
            }

            BookmarkKind.NOTE,
            BookmarkKind.IMAGE,
            BookmarkKind.FILE,
                -> null // TODO: Implement other bookmark kind models
        }
    }

    private fun BookmarkUiModel.toDomainBookmark(): LinkBookmarkDomainModel? =
        when (this) {
            is LinkmarkUiModel -> this.toDomain()
        }

    private data class BookmarkQueryParams(
        val kinds: List<BookmarkKind>,
        val applyKindFilter: Boolean,
        val folderIds: List<String>,
        val applyFolderFilter: Boolean,
        val tagIds: List<String>,
        val applyTagFilter: Boolean,
    )

    private fun BookmarkSearchFilters.toQueryParams(): BookmarkQueryParams {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        val folderSet = folderIds?.takeIf { it.isNotEmpty() }
        val tagSet = tagIds?.takeIf { it.isNotEmpty() }
        return BookmarkQueryParams(
            kinds = kindSet?.toList() ?: listOf(BookmarkKind.LINK),
            applyKindFilter = kindSet != null,
            folderIds = folderSet?.map { it.toString() } ?: listOf(Uuid.NIL.toString()),
            applyFolderFilter = folderSet != null,
            tagIds = tagSet?.map { it.toString() } ?: listOf(Uuid.NIL.toString()),
            applyTagFilter = tagSet != null,
        )
    }
}
