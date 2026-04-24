package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.BookmarkEntity
import dev.subfly.yaba.core.database.entities.ImageBookmarkEntity
import dev.subfly.yaba.core.database.entities.TagBookmarkCrossRef
import dev.subfly.yaba.core.database.mappers.toPreviewUiModel
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.database.models.BookmarkWithRelations
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.ImagemarkFileManager
import dev.subfly.yaba.core.filesystem.LinkmarkFileManager
import dev.subfly.yaba.core.images.ImageCompression
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.ui.BookmarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.BookmarkSearchFilters
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.notifications.NotificationManager
import dev.subfly.yaba.core.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

private data class SavedPreviewAssets(
    val localImageRelativePath: String?,
    val localIconRelativePath: String?,
    val imagemarkOriginalRelativePath: String? = null,
)

private data class BookmarkQueryParams(
    val folderIds: List<String>,
    val applyFolderFilter: Boolean,
    val tagIds: List<String>,
    val applyTagFilter: Boolean,
)

/**
 * DB-first bookmark manager.
 *
 * Metadata is stored in Room (bookmarkDao, linkBookmarkDao, tagBookmarkDao).
 * Asset files (preview images, readable content) are stored on disk under bookmarks/<id>/.
 */
object AllBookmarksManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val imageBookmarkDao get() = DatabaseProvider.imageBookmarkDao
    private val docBookmarkDao get() = DatabaseProvider.docBookmarkDao
    private val noteBookmarkDao get() = DatabaseProvider.noteBookmarkDao
    private val canvasBookmarkDao get() = DatabaseProvider.canvasBookmarkDao
    private val annotationDao get() = DatabaseProvider.annotationDao
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val bookmarkFileManager get() = BookmarkFileManager
    private val clock = Clock.System

    // ==================== Query Operations (from SQLite cache) ====================

    fun observeAllBookmarks(
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
        kinds: List<BookmarkKind>? = null,
    ): Flow<List<BookmarkUiModel>> {
        val kindSet = kinds?.takeIf { it.isNotEmpty() }
        return bookmarkDao
            .observeAll(
                kinds = kindSet ?: BookmarkKind.entries,
                applyKindFilter = kindSet != null,
                sortType = sortType.name,
                sortOrder = sortOrder.name,
                query = null,
                folderIds = emptyList(),
                applyFolderFilter = false,
                tagIds = emptyList(),
                applyTagFilter = false,
            )
            .map { rows -> rows.map { it.toBookmarkPreviewUiModel() } }
    }

    /**
     * Observes a single bookmark as [BookmarkUiModel], or null if it does not exist.
     */
    fun observeBookmarkById(bookmarkId: String): Flow<BookmarkUiModel?> {
        return bookmarkDao.observeByIdWithRelations(bookmarkId).map { row ->
            row?.toBookmarkPreviewUiModel()
        }
    }

    fun searchBookmarksFlow(
        query: String,
        filters: BookmarkSearchFilters = BookmarkSearchFilters(),
        sortType: SortType = SortType.EDITED_AT,
        sortOrder: SortOrderType = SortOrderType.DESCENDING,
    ): Flow<List<BookmarkUiModel>> {
        val params = filters.toQueryParams()
        return bookmarkDao
            .observeAll(
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
            .map { rows -> rows.map { it.toBookmarkPreviewUiModel() } }
    }

    // ==================== Write Operations (Enqueued) ====================

    /**
     * Enqueues bookmark creation.
     */
    fun createBookmarkMetadata(
        id: String = IdGenerator.newId(),
        folderId: String,
        kind: BookmarkKind,
        label: String,
        description: String? = null,
        isPinned: Boolean = false,
        tagIds: List<String> = emptyList(),
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = "jpeg",
        previewIconBytes: ByteArray? = null,
    ) {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        CoreOperationQueue.queue("CreateBookmark:$id") {
            createBookmarkMetadataInternal(
                id, folderId, kind, label, description, isPinned,
                tagIds, previewImageBytes, previewImageExtension, previewIconBytes
            )
        }
    }

    private suspend fun createBookmarkMetadataInternal(
        id: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        description: String?,
        isPinned: Boolean,
        tagIds: List<String>,
        previewImageBytes: ByteArray?,
        previewImageExtension: String?,
        previewIconBytes: ByteArray?,
    ) {
        val now = clock.now().toEpochMilliseconds()

        // 1. Save preview assets to filesystem
        val preview = savePreviewAssets(
            bookmarkId = id,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        // 2. Persist to DB
        val entity = BookmarkEntity(
            id = id,
            folderId = folderId,
            kind = kind,
            label = label,
            description = description,
            createdAt = now,
            editedAt = now,
            viewCount = 0,
            isPinned = isPinned,
            localImagePath = preview.localImageRelativePath,
            localIconPath = preview.localIconRelativePath,
        )
        bookmarkDao.upsert(entity)

        if (kind == BookmarkKind.IMAGE && preview.imagemarkOriginalRelativePath != null) {
            val existing = imageBookmarkDao.getByBookmarkId(id)
            imageBookmarkDao.upsert(
                ImageBookmarkEntity(
                    bookmarkId = id,
                    summary = existing?.summary,
                    originalImageRelativePath = preview.imagemarkOriginalRelativePath,
                ),
            )
        }

        tagIds.forEach { tagId ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = tagId,
                    bookmarkId = id,
                )
            )
        }
        syncPinnedSystemTagForBookmark(id, isPinned)
    }

    /**
     * Enqueues bookmark update.
     */
    fun updateBookmarkMetadata(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        description: String? = null,
        isPinned: Boolean = false,
        tagIds: List<String>? = null,
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = null,
        previewIconBytes: ByteArray? = null,
    ) {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        CoreOperationQueue.queue("UpdateBookmark:$bookmarkId") {
            updateBookmarkMetadataInternal(
                bookmarkId, folderId, kind, label, description, isPinned,
                tagIds, previewImageBytes, previewImageExtension, previewIconBytes
            )
        }
    }

    /**
     * Updates bookmark editedAt without changing other fields.
     * Useful for making the bookmark marked as edited after internal
     * additions such as highlighting or new version fetching.
     */
    fun touchBookmarkEditedAt(bookmarkId: String) {
        CoreOperationQueue.queue("TouchBookmarkEditedAt:$bookmarkId") {
            touchBookmarkEditedAtInternal(bookmarkId)
        }
    }

    /**
     * Increments [BookmarkEntity.viewCount] when the user opens a bookmark detail screen.
     */
    fun recordBookmarkView(bookmarkId: String) {
        CoreOperationQueue.queue("RecordBookmarkView:$bookmarkId") {
            bookmarkDao.incrementViewCount(bookmarkId)
        }
    }

    private suspend fun updateBookmarkMetadataInternal(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        description: String?,
        isPinned: Boolean,
        tagIds: List<String>?,
        previewImageBytes: ByteArray?,
        previewImageExtension: String?,
        previewIconBytes: ByteArray?,
    ) {
        val existing = bookmarkDao.getById(bookmarkId) ?: return
        val now = clock.now().toEpochMilliseconds()

        val preview = savePreviewAssets(
            bookmarkId = bookmarkId,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        val updated = existing.copy(
            folderId = folderId,
            kind = kind,
            label = label,
            description = description,
            editedAt = now,
            isPinned = isPinned,
            localImagePath = preview.localImageRelativePath ?: existing.localImagePath,
            localIconPath = preview.localIconRelativePath ?: existing.localIconPath,
        )
        bookmarkDao.upsert(updated)

        if (kind == BookmarkKind.IMAGE && preview.imagemarkOriginalRelativePath != null) {
            val imgExisting = imageBookmarkDao.getByBookmarkId(bookmarkId)
            imageBookmarkDao.upsert(
                ImageBookmarkEntity(
                    bookmarkId = bookmarkId,
                    summary = imgExisting?.summary,
                    originalImageRelativePath = preview.imagemarkOriginalRelativePath,
                ),
            )
        }

        if (tagIds != null) {
            val currentTagIds = tagBookmarkDao.getTagIdsForBookmark(bookmarkId).toSet()
            val newTagIds = tagIds.toSet()

            (currentTagIds - newTagIds).forEach { tagId ->
                tagBookmarkDao.delete(bookmarkId, tagId)
            }

            (newTagIds - currentTagIds).forEach { tagId ->
                tagBookmarkDao.insert(
                    TagBookmarkCrossRef(
                        tagId = tagId,
                        bookmarkId = bookmarkId,
                    )
                )
            }
        }
        syncPinnedSystemTagForBookmark(bookmarkId, isPinned)
    }

    /**
     * Toggles [BookmarkEntity.isPinned] and keeps the Pinned system tag in sync with that flag.
     */
    fun toggleBookmarkPinned(bookmarkId: String) {
        CoreOperationQueue.queue("TogglePin:$bookmarkId") {
            val existing = bookmarkDao.getById(bookmarkId) ?: return@queue
            val newPinned = !existing.isPinned
            val now = clock.now().toEpochMilliseconds()
            bookmarkDao.upsert(
                existing.copy(
                    isPinned = newPinned,
                    editedAt = now,
                ),
            )
            syncPinnedSystemTagForBookmark(bookmarkId, newPinned)
        }
    }

    private suspend fun syncPinnedSystemTagForBookmark(bookmarkId: String, isPinned: Boolean) {
        val pinnedTagId = CoreConstants.Tag.Pinned.ID
        val existing = bookmarkDao.getById(bookmarkId) ?: return
        val now = clock.now().toEpochMilliseconds()
        if (isPinned) {
            TagManager.ensurePinnedTag()
            val hasTag = tagBookmarkDao.getTagIdsForBookmark(bookmarkId).contains(pinnedTagId)
            if (!hasTag) {
                tagBookmarkDao.insert(
                    TagBookmarkCrossRef(
                        tagId = pinnedTagId,
                        bookmarkId = bookmarkId,
                    ),
                )
                bookmarkDao.upsert(existing.copy(editedAt = now))
            }
        } else {
            val hadTag = tagBookmarkDao.getTagIdsForBookmark(bookmarkId).contains(pinnedTagId)
            if (hadTag) {
                tagBookmarkDao.delete(bookmarkId, pinnedTagId)
                bookmarkDao.upsert(existing.copy(editedAt = now))
            }
        }
    }

    private suspend fun touchBookmarkEditedAtInternal(bookmarkId: String) {
        val existing = bookmarkDao.getById(bookmarkId) ?: return
        val now = clock.now().toEpochMilliseconds()

        bookmarkDao.upsert(existing.copy(editedAt = now))
    }

    fun moveBookmarksToFolder(
        bookmarkIds: List<String>,
        targetFolderId: String,
    ) {
        if (bookmarkIds.isEmpty()) return
        CoreOperationQueue.queue("MoveBookmarks:${bookmarkIds.size}") {
            moveBookmarksToFolderInternal(bookmarkIds, targetFolderId)
        }
    }

    private suspend fun moveBookmarksToFolderInternal(
        bookmarkIds: List<String>,
        targetFolderId: String,
    ) {
        val now = clock.now().toEpochMilliseconds()

        bookmarkIds.forEach { bookmarkId ->
            val existing = bookmarkDao.getById(bookmarkId) ?: return@forEach
            bookmarkDao.upsert(existing.copy(folderId = targetFolderId, editedAt = now))
        }
    }

    fun deleteBookmarks(bookmarkIds: List<String>) {
        if (bookmarkIds.isEmpty()) return
        CoreOperationQueue.queue("DeleteBookmarks:${bookmarkIds.size}") {
            deleteBookmarksInternal(bookmarkIds)
        }
    }

    /**
     * Deletes a single bookmark by ID (content + DB).
     * Used by FolderManager for cascade deletion.
     */
    suspend fun deleteBookmarkById(bookmarkId: String) {
        deleteSingleBookmarkInternal(bookmarkId)
    }

    private suspend fun deleteBookmarksInternal(bookmarkIds: List<String>) {
        bookmarkIds.forEach { bookmarkId ->
            deleteSingleBookmarkInternal(bookmarkId)
        }
    }

    private suspend fun deleteSingleBookmarkInternal(bookmarkId: String) {
        // 1. Delete bookmark folder (assets) from filesystem
        bookmarkFileManager.deleteBookmarkFolder(bookmarkId)

        // 2. Cancel any pending reminder notification
        NotificationManager.cancelReminder(bookmarkId)

        // 3. Remove all related SQLite cache rows
        tagBookmarkDao.deleteForBookmark(bookmarkId)
        linkBookmarkDao.deleteById(bookmarkId)
        imageBookmarkDao.deleteById(bookmarkId)
        docBookmarkDao.deleteById(bookmarkId)
        noteBookmarkDao.deleteById(bookmarkId)
        canvasBookmarkDao.deleteById(bookmarkId)
        annotationDao.deleteByBookmarkId(bookmarkId)
        readableVersionDao.deleteByBookmarkId(bookmarkId)
        bookmarkDao.deleteByIds(listOf(bookmarkId))
    }

    fun addTagToBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.queue("AddTag:$tagId:$bookmarkId") {
            addTagToBookmarkInternal(tagId, bookmarkId)
        }
    }

    private suspend fun addTagToBookmarkInternal(tagId: String, bookmarkId: String) {
        val existing = bookmarkDao.getById(bookmarkId) ?: return
        val now = clock.now().toEpochMilliseconds()

        tagBookmarkDao.insert(
            TagBookmarkCrossRef(
                tagId = tagId,
                bookmarkId = bookmarkId,
            )
        )
        bookmarkDao.upsert(existing.copy(editedAt = now))
    }

    fun removeTagFromBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.queue("RemoveTag:$tagId:$bookmarkId") {
            removeTagFromBookmarkInternal(tagId, bookmarkId)
        }
    }

    private suspend fun removeTagFromBookmarkInternal(tagId: String, bookmarkId: String) {
        val existing = bookmarkDao.getById(bookmarkId) ?: return
        val now = clock.now().toEpochMilliseconds()

        tagBookmarkDao.delete(bookmarkId, tagId)
        bookmarkDao.upsert(existing.copy(editedAt = now))
    }

    // ==================== Private Helpers ====================

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
        val folderUi = folder.toUiModel()
        val tagsUi = tags.map { it.toUiModel() }

        val localImageAbsolutePath = bookmark.localImagePath?.let { relativePath ->
            bookmarkFileManager.getAbsolutePath(relativePath)
        }

        val localIconAbsolutePath = bookmark.localIconPath?.let { relativePath ->
            bookmarkFileManager.getAbsolutePath(relativePath)
        }

        return bookmark.toPreviewUiModel(
            folder = folderUi,
            tags = tagsUi,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
        )
    }

    private suspend fun imageCompressionPercent(): Int =
        SettingsStores.userPreferences.get().imageCompressionPercent.coerceIn(0, 50)

    private suspend fun savePreviewAssets(
        bookmarkId: String,
        kind: BookmarkKind,
        imageBytes: ByteArray?,
        imageExtension: String?,
        iconBytes: ByteArray?,
    ): SavedPreviewAssets {
        val compressionP = imageCompressionPercent()

        if (imageBytes != null && kind == BookmarkKind.IMAGE) {
            val ext = sanitizeExtension(imageExtension)
            ImagemarkFileManager.saveOriginalImageBytes(bookmarkId, imageBytes, ext)
            val originalRel = CoreConstants.FileSystem.Imagemark.imageOriginalPath(bookmarkId, ext)
            val out = ImageCompression.compressForStorage(imageBytes, ext, compressionP)
            val toWrite = out?.bytes ?: imageBytes
            val outExt = out?.extension ?: ext
            ImagemarkFileManager.saveImageBytes(bookmarkId, toWrite, outExt)
            val previewRel = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, outExt)
            return SavedPreviewAssets(
                localImageRelativePath = previewRel,
                localIconRelativePath = null,
                imagemarkOriginalRelativePath = originalRel,
            )
        }

        val imageRelativePath = imageBytes?.let { bytes ->
            when (kind) {
                BookmarkKind.LINK -> {
                    val ext = sanitizeExtension(imageExtension)
                    val out =
                        ImageCompression.compressForStorage(bytes, ext, compressionP)
                    val toWrite = out?.bytes ?: bytes
                    val outExt = out?.extension ?: ext
                    LinkmarkFileManager.saveLinkImageBytes(bookmarkId, toWrite, outExt)
                    CoreConstants.FileSystem.Linkmark.linkImagePath(bookmarkId, outExt)
                }

                else -> {
                    val ext = sanitizeExtension(imageExtension)
                    val out =
                        ImageCompression.compressForStorage(bytes, ext, compressionP)
                    val toWrite = out?.bytes ?: bytes
                    val outExt = out?.extension ?: ext
                    val kindDir = kind.name.lowercase()
                    val relativePath = CoreConstants.FileSystem.join(
                        CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, kindDir),
                        "preview_image.$outExt",
                    )
                    BookmarkFileManager.writeBytes(relativePath, toWrite)
                    relativePath
                }
            }
        }

        val iconRelativePath = iconBytes?.let { bytes ->
            when (kind) {
                BookmarkKind.LINK -> {
                    val out =
                        ImageCompression.compressForStorage(bytes, "png", compressionP)
                    val toWrite = out?.bytes ?: bytes
                    LinkmarkFileManager.saveDomainIconBytes(bookmarkId, toWrite)
                    CoreConstants.FileSystem.Linkmark.domainIconPath(
                        bookmarkId,
                        "png",
                    )
                }

                BookmarkKind.IMAGE -> null

                else -> {
                    val out =
                        ImageCompression.compressForStorage(bytes, "png", compressionP)
                    val toWrite = out?.bytes ?: bytes
                    val outExt = out?.extension ?: "png"
                    val kindDir = kind.name.lowercase()
                    val relativePath = CoreConstants.FileSystem.join(
                        CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, kindDir),
                        "preview_icon.$outExt",
                    )
                    BookmarkFileManager.writeBytes(relativePath, toWrite)
                    relativePath
                }
            }
        }

        return SavedPreviewAssets(
            localImageRelativePath = imageRelativePath,
            localIconRelativePath = iconRelativePath,
            imagemarkOriginalRelativePath = null,
        )
    }

    private fun sanitizeExtension(rawExtension: String?): String =
        rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { "jpeg" }

    private fun BookmarkSearchFilters.toQueryParams(): BookmarkQueryParams {
        val folders = folderIds?.takeIf { it.isNotEmpty() }.orEmpty().toList()
        val tags = tagIds?.takeIf { it.isNotEmpty() }.orEmpty().toList()
        return BookmarkQueryParams(
            folderIds = folders,
            applyFolderFilter = folders.isNotEmpty(),
            tagIds = tags,
            applyTagFilter = tags.isNotEmpty(),
        )
    }

}
