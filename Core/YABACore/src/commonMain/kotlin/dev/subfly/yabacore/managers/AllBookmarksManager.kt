@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.domain.BookmarkMetadataDomainModel
import dev.subfly.yabacore.database.mappers.toMetadataModel
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
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.FolderUiModel
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

    /**
     * Creates the base bookmark metadata row (used by list/grid) and persists preview assets
     * (image/icon) into `BookmarkEntity.localImagePath/localIconPath` as relative paths.
     *
     * Subtype details (e.g., link url/domain) are saved separately by subtype managers.
     */
    suspend fun createBookmarkMetadata(
        id: Uuid = Uuid.random(),
        folderId: Uuid,
        kind: BookmarkKind,
        label: String,
        description: String? = null,
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = "jpeg",
        previewIconBytes: ByteArray? = null,
    ): BookmarkPreviewUiModel {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        val now = clock.now()

        val preview = savePreviewAssets(
            bookmarkId = id,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        val domain = BookmarkMetadataDomainModel(
            id = id,
            folderId = folderId,
            kind = kind,
            label = label,
            description = description,
            createdAt = now,
            editedAt = now,
            isPrivate = isPrivate,
            isPinned = isPinned,
            localImagePath = preview.localImageRelativePath,
            localIconPath = preview.localIconRelativePath,
        )

        opApplier.applyLocal(listOf(domain.toOperationDraft(OperationKind.CREATE)))
        return getBookmarkDetail(id) as? BookmarkPreviewUiModel
            ?: BookmarkPreviewUiModel(
                id = id,
                folderId = folderId,
                kind = kind,
                label = label,
                description = description,
                createdAt = now,
                editedAt = now,
                isPrivate = isPrivate,
                isPinned = isPinned,
                parentFolder = null,
                tags = emptyList(),
            )
    }

    suspend fun updateBookmarkMetadata(
        bookmarkId: Uuid,
        folderId: Uuid,
        kind: BookmarkKind,
        label: String,
        description: String? = null,
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = null,
        previewIconBytes: ByteArray? = null,
    ): BookmarkPreviewUiModel? {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        val existing = bookmarkDao.getById(bookmarkId.toString()) ?: return null
        val now = clock.now()

        val existingDomain = existing.toMetadataModel()

        val preview = savePreviewAssets(
            bookmarkId = bookmarkId,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        val updated =
            existingDomain.copy(
                folderId = folderId,
                kind = kind,
                label = label,
                description = description,
                editedAt = now,
                isPrivate = isPrivate,
                isPinned = isPinned,
                localImagePath = preview.localImageRelativePath ?: existingDomain.localImagePath,
                localIconPath = preview.localIconRelativePath ?: existingDomain.localIconPath,
            )

        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
        return getBookmarkDetail(bookmarkId) as? BookmarkPreviewUiModel
    }

    suspend fun moveBookmarksToFolder(
        bookmarks: List<BookmarkUiModel>,
        targetFolder: FolderUiModel,
    ) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts =
            bookmarks.mapNotNull { bookmark ->
                bookmarkDao.getById(bookmark.id.toString())
                    ?.toMetadataModel()
                    ?.copy(folderId = targetFolder.id, editedAt = now)
                    ?.toOperationDraft(OperationKind.MOVE)
            }
        if (drafts.isEmpty()) return
        opApplier.applyLocal(drafts)
    }

    suspend fun deleteBookmarks(bookmarks: List<BookmarkUiModel>) {
        if (bookmarks.isEmpty()) return
        val now = clock.now()
        val drafts =
            bookmarks.mapNotNull { bookmark ->
                bookmarkDao.getById(bookmark.id.toString())
                    ?.toMetadataModel()
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
            .map { rows -> rows.mapNotNull { it.toBookmarkPreviewUiModel() } }
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
            .map { rows -> rows.mapNotNull { it.toBookmarkPreviewUiModel() } }
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
            .map { rows -> rows.mapNotNull { it.toBookmarkPreviewUiModel() } }
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
                kinds = emptyList(),
                applyKindFilter = false,
                folderIds = params.folderIds,
                applyFolderFilter = params.applyFolderFilter,
                tagIds = params.tagIds,
                applyTagFilter = params.applyTagFilter,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
            )
            .map { rows -> rows.mapNotNull { it.toBookmarkPreviewUiModel() } }
    }

    suspend fun getBookmarkDetail(bookmarkId: Uuid): BookmarkUiModel? =
        bookmarkDao.getBookmarkWithRelationsById(bookmarkId.toString())?.toBookmarkPreviewUiModel()

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel? {
        val folderUi = folder.toModel().toUiModel()
        val tagsUi = tags.sortedBy { it.order }.map { it.toModel().toUiModel() }

        val localImageAbsolutePath =
            bookmark.localImagePath?.let { relativePath ->
                bookmarkFileManager.resolve(relativePath).path
            }

        val localIconAbsolutePath =
            bookmark.localIconPath?.let { relativePath ->
                bookmarkFileManager.resolve(relativePath).path
            }

        return bookmark
            .toMetadataModel()
            .toUiModel(
                folder = folderUi,
                tags = tagsUi,
                localImagePath = localImageAbsolutePath,
                localIconPath = localIconAbsolutePath,
            )
    }

    private data class SavedPreviewAssets(
        val localImageRelativePath: String?,
        val localIconRelativePath: String?,
    )

    private suspend fun savePreviewAssets(
        bookmarkId: Uuid,
        kind: BookmarkKind,
        imageBytes: ByteArray?,
        imageExtension: String?,
        iconBytes: ByteArray?,
    ): SavedPreviewAssets {
        val imageRelativePath =
            imageBytes?.let { bytes ->
                when (kind) {
                    BookmarkKind.LINK -> {
                        val ext = sanitizeExtension(imageExtension, fallback = "jpeg")
                        LinkmarkFileManager.saveLinkImageBytes(bookmarkId, bytes, ext)
                        CoreConstants.FileSystem.Linkmark.linkImagePath(bookmarkId, ext)
                    }

                    else -> {
                        // Future kinds will get their own file manager + asset kinds.
                        // Keep routing logic centralized here.
                        val ext = sanitizeExtension(imageExtension, fallback = "jpeg")
                        val kindDir = kind.name.lowercase()
                        val relativePath =
                            CoreConstants.FileSystem.join(
                                CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, kindDir),
                                "preview_image.$ext",
                            )
                        BookmarkFileManager.writeBytes(relativePath, bytes)
                        relativePath
                    }
                }
            }

        val iconRelativePath =
            iconBytes?.let { bytes ->
                when (kind) {
                    BookmarkKind.LINK -> {
                        LinkmarkFileManager.saveDomainIconBytes(bookmarkId, bytes)
                        CoreConstants.FileSystem.Linkmark.domainIconPath(bookmarkId, "png")
                    }

                    else -> {
                        val ext = "png"
                        val kindDir = kind.name.lowercase()
                        val relativePath =
                            CoreConstants.FileSystem.join(
                                CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, kindDir),
                                "preview_icon.$ext",
                            )
                        BookmarkFileManager.writeBytes(relativePath, bytes)
                        relativePath
                    }
                }
            }

        return SavedPreviewAssets(
            localImageRelativePath = imageRelativePath,
            localIconRelativePath = iconRelativePath,
        )
    }

    private fun sanitizeExtension(rawExtension: String?, fallback: String): String =
        rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { fallback }

    private data class BookmarkQueryParams(
        val folderIds: List<String>,
        val applyFolderFilter: Boolean,
        val tagIds: List<String>,
        val applyTagFilter: Boolean,
    )

    private fun BookmarkSearchFilters.toQueryParams(): BookmarkQueryParams {
        val folderSet = folderIds?.takeIf { it.isNotEmpty() }
        val tagSet = tagIds?.takeIf { it.isNotEmpty() }
        return BookmarkQueryParams(
            folderIds = folderSet?.map { it.toString() } ?: listOf(Uuid.NIL.toString()),
            applyFolderFilter = folderSet != null,
            tagIds = tagSet?.map { it.toString() } ?: listOf(Uuid.NIL.toString()),
            applyTagFilter = tagSet != null,
        )
    }
}
