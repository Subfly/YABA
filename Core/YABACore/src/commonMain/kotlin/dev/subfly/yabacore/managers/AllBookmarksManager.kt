package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.BookmarkWithRelations
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.LinkmarkFileManager
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.ui.BookmarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant

private data class SavedPreviewAssets(
    val localImageRelativePath: String?,
    val localIconRelativePath: String?,
)

/**
 * Filesystem-first bookmark manager.
 *
 * All operations:
 * 1. Write to filesystem JSON first (authoritative)
 * 2. Generate CRDT events for sync
 * 3. Update SQLite cache for queries
 */
object AllBookmarksManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val entityFileManager get() = EntityFileManager
    private val bookmarkFileManager get() = BookmarkFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System

    // ==================== Query Operations (from SQLite cache) ====================

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
            .map { rows -> rows.map { it.toBookmarkPreviewUiModel() } }
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
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        tagIds: List<String> = emptyList(),
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = "jpeg",
        previewIconBytes: ByteArray? = null,
    ) {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        CoreOperationQueue.queue("CreateBookmark:$id") {
            createBookmarkMetadataInternal(
                id, folderId, kind, label, description, isPrivate, isPinned,
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
        isPrivate: Boolean,
        isPinned: Boolean,
        tagIds: List<String>,
        previewImageBytes: ByteArray?,
        previewImageExtension: String?,
        previewIconBytes: ByteArray?,
    ) {
        val now = clock.now()
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        // Save preview assets first
        val preview = savePreviewAssets(
            bookmarkId = id,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        val bookmarkJson = BookmarkMetaJson(
            id = id,
            folderId = folderId,
            kind = kind.code,
            label = label,
            description = description,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            viewCount = 0,
            isPrivate = isPrivate,
            isPinned = isPinned,
            localImagePath = preview.localImageRelativePath,
            localIconPath = preview.localIconRelativePath,
            tagIds = tagIds,
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeBookmarkMeta(bookmarkJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = id,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            payload = buildBookmarkCreatePayload(bookmarkJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        val entity = bookmarkJson.toEntity()
        bookmarkDao.upsert(entity)

        // 4. Update tag-bookmark relationships
        tagIds.forEach { tagId ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = tagId,
                    bookmarkId = id,
                )
            )
        }
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
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        tagIds: List<String>? = null,
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = null,
        previewIconBytes: ByteArray? = null,
    ) {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        CoreOperationQueue.queue("UpdateBookmark:$bookmarkId") {
            updateBookmarkMetadataInternal(
                bookmarkId, folderId, kind, label, description, isPrivate, isPinned,
                tagIds, previewImageBytes, previewImageExtension, previewIconBytes
            )
        }
    }

    /**
     * Updates bookmark editedAt without changing other fields.
     */
    fun touchBookmarkEditedAt(bookmarkId: String) {
        CoreOperationQueue.queue("TouchBookmarkEditedAt:$bookmarkId") {
            touchBookmarkEditedAtInternal(bookmarkId)
        }
    }

    private suspend fun updateBookmarkMetadataInternal(
        bookmarkId: String,
        folderId: String,
        kind: BookmarkKind,
        label: String,
        description: String?,
        isPrivate: Boolean,
        isPinned: Boolean,
        tagIds: List<String>?,
        previewImageBytes: ByteArray?,
        previewImageExtension: String?,
        previewIconBytes: ByteArray?,
    ) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        // Save preview assets
        val preview = savePreviewAssets(
            bookmarkId = bookmarkId,
            kind = kind,
            imageBytes = previewImageBytes,
            imageExtension = previewImageExtension,
            iconBytes = previewIconBytes,
        )

        // Detect changes
        val changes = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        if (existingJson.folderId != folderId) {
            changes["folderId"] = JsonPrimitive(folderId)
        }
        if (existingJson.label != label) {
            changes["label"] = JsonPrimitive(label)
        }
        if (existingJson.description != description) {
            changes["description"] = CRDTEngine.nullableStringValue(description)
        }
        if (existingJson.isPrivate != isPrivate) {
            changes["isPrivate"] = JsonPrimitive(isPrivate)
        }
        if (existingJson.isPinned != isPinned) {
            changes["isPinned"] = JsonPrimitive(isPinned)
        }
        if (preview.localImageRelativePath != null && existingJson.localImagePath != preview.localImageRelativePath) {
            changes["localImagePath"] =
                CRDTEngine.nullableStringValue(preview.localImageRelativePath)
        }
        if (preview.localIconRelativePath != null && existingJson.localIconPath != preview.localIconRelativePath) {
            changes["localIconPath"] = CRDTEngine.nullableStringValue(preview.localIconRelativePath)
        }

        val newClock = existingClock.increment(deviceId)
        val resolvedTagIds = tagIds ?: existingJson.tagIds

        val updatedJson = existingJson.copy(
            folderId = folderId,
            kind = kind.code,
            label = label,
            description = description,
            editedAt = now.toEpochMilliseconds(),
            isPrivate = isPrivate,
            isPinned = isPinned,
            localImagePath = preview.localImageRelativePath ?: existingJson.localImagePath,
            localIconPath = preview.localIconRelativePath ?: existingJson.localIconPath,
            tagIds = resolvedTagIds,
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Record CRDT UPDATE event (only if there are changes)
        if (changes.isNotEmpty()) {
            crdtEngine.recordUpdate(
                objectId = bookmarkId,
                objectType = ObjectType.BOOKMARK,
                file = FileTarget.META_JSON,
                changes = changes,
                currentClock = existingClock,
            )
        }

        // 3. Update SQLite cache
        bookmarkDao.upsert(updatedJson.toEntity())

        // 4. Update tag-bookmark relationships if tags changed
        if (tagIds != null) {
            val currentTagIds = existingJson.tagIds.toSet()
            val newTagIds = tagIds.toSet()

            // Remove old tags
            (currentTagIds - newTagIds).forEach { tagId ->
                tagBookmarkDao.delete(bookmarkId, tagId)
            }

            // Add new tags
            (newTagIds - currentTagIds).forEach { tagId ->
                tagBookmarkDao.insert(
                    TagBookmarkCrossRef(
                        tagId = tagId,
                        bookmarkId = bookmarkId,
                    )
                )
            }
        }
    }

    private suspend fun touchBookmarkEditedAtInternal(bookmarkId: String) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()
        val newClock = existingClock.increment(deviceId)

        val updatedJson = existingJson.copy(
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        entityFileManager.writeBookmarkMeta(updatedJson)
        crdtEngine.recordUpdate(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            changes = mapOf("editedAt" to JsonPrimitive(now.toEpochMilliseconds())),
            currentClock = existingClock,
        )
        bookmarkDao.upsert(updatedJson.toEntity())
    }

    /**
     * Enqueues move bookmarks operation.
     */
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
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        bookmarkIds.forEach { bookmarkId ->
            val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return@forEach
            val existingClock = VectorClock.fromMap(existingJson.clock)
            val newClock = existingClock.increment(deviceId)

            val updatedJson = existingJson.copy(
                folderId = targetFolderId,
                editedAt = now.toEpochMilliseconds(),
                clock = newClock.toMap(),
            )

            // 1. Write to filesystem
            entityFileManager.writeBookmarkMeta(updatedJson)

            // 2. Record CRDT UPDATE event
            crdtEngine.recordUpdate(
                objectId = bookmarkId,
                objectType = ObjectType.BOOKMARK,
                file = FileTarget.META_JSON,
                changes = mapOf("folderId" to JsonPrimitive(targetFolderId)),
                currentClock = existingClock,
            )

            // 3. Update SQLite cache
            bookmarkDao.upsert(updatedJson.toEntity())
        }
    }

    /**
     * Enqueues bookmark deletions.
     */
    fun deleteBookmarks(bookmarkIds: List<String>) {
        if (bookmarkIds.isEmpty()) return
        CoreOperationQueue.queue("DeleteBookmarks:${bookmarkIds.size}") {
            deleteBookmarksInternal(bookmarkIds)
        }
    }

    /**
     * Internal bookmark deletion - can be called from within queued operations.
     * Made private - other managers should not call this directly.
     */
    private suspend fun deleteBookmarksInternal(bookmarkIds: List<String>) {
        bookmarkIds.forEach { bookmarkId ->
            deleteSingleBookmarkInternal(bookmarkId)
        }
    }

    /**
     * Deletes a single bookmark with full cleanup.
     */
    private suspend fun deleteSingleBookmarkInternal(bookmarkId: String) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId)
        val existingClock =
            existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // 1. Write deletion tombstone to filesystem and delete content recursively
        entityFileManager.deleteBookmark(bookmarkId, existingClock)

        // 2. Record CRDT DELETE event
        crdtEngine.recordDelete(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            currentClock = existingClock,
        )

        // 3. Remove all related SQLite cache rows
        tagBookmarkDao.deleteForBookmark(bookmarkId)
        linkBookmarkDao.deleteById(bookmarkId)
        DatabaseProvider.highlightDao.deleteByBookmarkId(bookmarkId)
        bookmarkDao.deleteByIds(listOf(bookmarkId))
    }

    /**
     * Enqueues adding a tag to a bookmark.
     */
    fun addTagToBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.queue("AddTag:$tagId:$bookmarkId") {
            addTagToBookmarkInternal(tagId, bookmarkId)
        }
    }

    private suspend fun addTagToBookmarkInternal(tagId: String, bookmarkId: String) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)
        val now = clock.now()

        val newTagIds = (existingJson.tagIds + tagId).distinct()

        val updatedJson = existingJson.copy(
            tagIds = newTagIds,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Record CRDT UPDATE event for tagIds change
        crdtEngine.recordUpdate(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            changes = mapOf("tagIds" to JsonArray(newTagIds.map { JsonPrimitive(it) })),
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        tagBookmarkDao.insert(
            TagBookmarkCrossRef(
                tagId = tagId,
                bookmarkId = bookmarkId,
            )
        )
        bookmarkDao.upsert(updatedJson.toEntity())
    }

    /**
     * Enqueues removing a tag from a bookmark.
     */
    fun removeTagFromBookmark(tagId: String, bookmarkId: String) {
        CoreOperationQueue.queue("RemoveTag:$tagId:$bookmarkId") {
            removeTagFromBookmarkInternal(tagId, bookmarkId)
        }
    }

    private suspend fun removeTagFromBookmarkInternal(tagId: String, bookmarkId: String) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)
        val now = clock.now()

        val newTagIds = existingJson.tagIds.filterNot { it == tagId }

        val updatedJson = existingJson.copy(
            tagIds = newTagIds,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Record CRDT UPDATE event for tagIds change
        crdtEngine.recordUpdate(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            changes = mapOf("tagIds" to JsonArray(newTagIds.map { JsonPrimitive(it) })),
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        tagBookmarkDao.delete(bookmarkId, tagId)
        bookmarkDao.upsert(updatedJson.toEntity())
    }

    // ==================== Private Helpers ====================

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel {
        val folderUi = folder.toModel().toUiModel()
        val tagsUi = tags.sortedBy { it.order }.map { it.toModel().toUiModel() }

        val localImageAbsolutePath = bookmark.localImagePath?.let { relativePath ->
            bookmarkFileManager.getAbsolutePath(relativePath)
        }

        val localIconAbsolutePath = bookmark.localIconPath?.let { relativePath ->
            bookmarkFileManager.getAbsolutePath(relativePath)
        }

        return BookmarkPreviewUiModel(
            id = bookmark.id,
            folderId = bookmark.folderId,
            kind = bookmark.kind,
            label = bookmark.label,
            description = bookmark.description,
            createdAt = Instant.fromEpochMilliseconds(bookmark.createdAt),
            editedAt = Instant.fromEpochMilliseconds(bookmark.editedAt),
            viewCount = bookmark.viewCount,
            isPrivate = bookmark.isPrivate,
            isPinned = bookmark.isPinned,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folderUi,
            tags = tagsUi,
        )
    }

    private suspend fun savePreviewAssets(
        bookmarkId: String,
        kind: BookmarkKind,
        imageBytes: ByteArray?,
        imageExtension: String?,
        iconBytes: ByteArray?,
    ): SavedPreviewAssets {
        val imageRelativePath = imageBytes?.let { bytes ->
            when (kind) {
                BookmarkKind.LINK -> {
                    val ext = sanitizeExtension(imageExtension)
                    LinkmarkFileManager.saveLinkImageBytes(bookmarkId, bytes, ext)
                    CoreConstants.FileSystem.Linkmark.linkImagePath(bookmarkId, ext)
                }

                else -> {
                    val ext = sanitizeExtension(imageExtension)
                    val kindDir = kind.name.lowercase()
                    val relativePath = CoreConstants.FileSystem.join(
                        CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, kindDir),
                        "preview_image.$ext",
                    )
                    BookmarkFileManager.writeBytes(relativePath, bytes)
                    relativePath
                }
            }
        }

        val iconRelativePath = iconBytes?.let { bytes ->
            when (kind) {
                BookmarkKind.LINK -> {
                    LinkmarkFileManager.saveDomainIconBytes(bookmarkId, bytes)
                    CoreConstants.FileSystem.Linkmark.domainIconPath(bookmarkId, "png")
                }

                else -> {
                    val ext = "png"
                    val kindDir = kind.name.lowercase()
                    val relativePath = CoreConstants.FileSystem.join(
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

    private fun sanitizeExtension(rawExtension: String?): String =
        rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { "jpeg" }

    private fun buildBookmarkCreatePayload(json: BookmarkMetaJson): Map<String, kotlinx.serialization.json.JsonElement> =
        mapOf(
            "id" to JsonPrimitive(json.id),
            "folderId" to JsonPrimitive(json.folderId),
            "kind" to JsonPrimitive(json.kind),
            "label" to JsonPrimitive(json.label),
            "description" to CRDTEngine.nullableStringValue(json.description),
            "createdAt" to JsonPrimitive(json.createdAt),
            "editedAt" to JsonPrimitive(json.editedAt),
            "viewCount" to JsonPrimitive(json.viewCount),
            "isPrivate" to JsonPrimitive(json.isPrivate),
            "isPinned" to JsonPrimitive(json.isPinned),
            "localImagePath" to CRDTEngine.nullableStringValue(json.localImagePath),
            "localIconPath" to CRDTEngine.nullableStringValue(json.localIconPath),
            "tagIds" to JsonArray(json.tagIds.map { JsonPrimitive(it) }),
        )

    private data class BookmarkQueryParams(
        val folderIds: List<String>,
        val applyFolderFilter: Boolean,
        val tagIds: List<String>,
        val applyTagFilter: Boolean,
    )

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

    // ==================== Mappers ====================

    private fun BookmarkMetaJson.toEntity(): BookmarkEntity = BookmarkEntity(
        id = id,
        folderId = folderId,
        kind = BookmarkKind.fromCode(kind),
        label = label,
        description = description,
        createdAt = createdAt,
        editedAt = editedAt,
        viewCount = viewCount,
        isPrivate = isPrivate,
        isPinned = isPinned,
        localImagePath = localImagePath,
        localIconPath = localIconPath,
    )
}
