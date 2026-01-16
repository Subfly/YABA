@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
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
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.BookmarkSearchFilters
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
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

    // ==================== Write Operations (Filesystem-First) ====================

    suspend fun createBookmarkMetadata(
        id: Uuid = Uuid.random(),
        folderId: Uuid,
        kind: BookmarkKind,
        label: String,
        description: String? = null,
        isPrivate: Boolean = false,
        isPinned: Boolean = false,
        tagIds: List<Uuid> = emptyList(),
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = "jpeg",
        previewIconBytes: ByteArray? = null,
    ): BookmarkPreviewUiModel {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
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
            id = id.toString(),
            folderId = folderId.toString(),
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
            tagIds = tagIds.map { it.toString() },
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeBookmarkMeta(bookmarkJson)

        // 2. Record CRDT events
        recordBookmarkCreationEvents(id, bookmarkJson)

        // 3. Update SQLite cache
        val entity = bookmarkJson.toEntity()
        bookmarkDao.upsert(entity)

        // 4. Update tag-bookmark relationships
        tagIds.forEach { tagId ->
            tagBookmarkDao.insert(
                TagBookmarkCrossRef(
                    tagId = tagId.toString(),
                    bookmarkId = id.toString(),
                )
            )
        }

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
        tagIds: List<Uuid>? = null,
        previewImageBytes: ByteArray? = null,
        previewImageExtension: String? = null,
        previewIconBytes: ByteArray? = null,
    ): BookmarkPreviewUiModel? {
        require(label.isNotBlank()) { "Bookmark label must not be blank." }
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId) ?: return null
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
        if (existingJson.folderId != folderId.toString()) {
            changes["folderId"] = JsonPrimitive(folderId.toString())
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
            changes["localImagePath"] = CRDTEngine.nullableStringValue(preview.localImageRelativePath)
        }
        if (preview.localIconRelativePath != null && existingJson.localIconPath != preview.localIconRelativePath) {
            changes["localIconPath"] = CRDTEngine.nullableStringValue(preview.localIconRelativePath)
        }

        val newClock = existingClock.increment(deviceId)
        val resolvedTagIds = tagIds?.map { it.toString() } ?: existingJson.tagIds

        val updatedJson = existingJson.copy(
            folderId = folderId.toString(),
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

        // 2. Record CRDT events
        if (changes.isNotEmpty()) {
            crdtEngine.recordFieldChanges(
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
            val currentTagIds = existingJson.tagIds.mapNotNull { runCatching { Uuid.parse(it) }.getOrNull() }.toSet()
            val newTagIds = tagIds.toSet()

            // Remove old tags
            (currentTagIds - newTagIds).forEach { tagId ->
                tagBookmarkDao.delete(bookmarkId.toString(), tagId.toString())
            }

            // Add new tags
            (newTagIds - currentTagIds).forEach { tagId ->
                tagBookmarkDao.insert(
                    TagBookmarkCrossRef(
                        tagId = tagId.toString(),
                        bookmarkId = bookmarkId.toString(),
                    )
                )
            }
        }

        return getBookmarkDetail(bookmarkId) as? BookmarkPreviewUiModel
    }

    suspend fun moveBookmarksToFolder(bookmarks: List<BookmarkUiModel>, targetFolder: FolderUiModel) {
        if (bookmarks.isEmpty()) return
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        bookmarks.forEach { bookmark ->
            val existingJson = entityFileManager.readBookmarkMeta(bookmark.id) ?: return@forEach
            val existingClock = VectorClock.fromMap(existingJson.clock)
            val newClock = existingClock.increment(deviceId)

            val updatedJson = existingJson.copy(
                folderId = targetFolder.id.toString(),
                editedAt = now.toEpochMilliseconds(),
                clock = newClock.toMap(),
            )

            // 1. Write to filesystem
            entityFileManager.writeBookmarkMeta(updatedJson)

            // 2. Record CRDT event
            crdtEngine.recordFieldChange(
                objectId = bookmark.id,
                objectType = ObjectType.BOOKMARK,
                file = FileTarget.META_JSON,
                field = "folderId",
                value = JsonPrimitive(targetFolder.id.toString()),
                currentClock = existingClock,
            )

            // 3. Update SQLite cache
            bookmarkDao.upsert(updatedJson.toEntity())
        }
    }

    suspend fun deleteBookmarks(bookmarks: List<BookmarkUiModel>) {
        if (bookmarks.isEmpty()) return
        val deviceId = DeviceIdProvider.get()

        bookmarks.forEach { bookmark ->
            val existingJson = entityFileManager.readBookmarkMeta(bookmark.id)
            val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
            val deletionClock = existingClock.increment(deviceId)

            // 1. Write deletion tombstone to filesystem
            entityFileManager.deleteBookmark(bookmark.id, deletionClock)

            // 2. Record deletion event
            crdtEngine.recordFieldChange(
                objectId = bookmark.id,
                objectType = ObjectType.BOOKMARK,
                file = FileTarget.META_JSON,
                field = "_deleted",
                value = JsonPrimitive(true),
                currentClock = existingClock,
            )

            // 3. Remove from SQLite cache
            bookmarkDao.deleteByIds(listOf(bookmark.id.toString()))
        }
    }

    suspend fun addTagToBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmark.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)
        val now = clock.now()

        val newTagIds = (existingJson.tagIds + tag.id.toString()).distinct()

        val updatedJson = existingJson.copy(
            tagIds = newTagIds,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Update SQLite cache
        tagBookmarkDao.insert(
            TagBookmarkCrossRef(
                tagId = tag.id.toString(),
                bookmarkId = bookmark.id.toString(),
            )
        )
        bookmarkDao.upsert(updatedJson.toEntity())
    }

    suspend fun removeTagFromBookmark(tag: TagUiModel, bookmark: BookmarkUiModel) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmark.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)
        val now = clock.now()

        val newTagIds = existingJson.tagIds.filterNot { it == tag.id.toString() }

        val updatedJson = existingJson.copy(
            tagIds = newTagIds,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Update SQLite cache
        tagBookmarkDao.delete(bookmark.id.toString(), tag.id.toString())
        bookmarkDao.upsert(updatedJson.toEntity())
    }

    // ==================== Private Helpers ====================

    private suspend fun BookmarkWithRelations.toBookmarkPreviewUiModel(): BookmarkPreviewUiModel? {
        val folderUi = folder.toModel().toUiModel()
        val tagsUi = tags.sortedBy { it.order }.map { it.toModel().toUiModel() }

        val localImageAbsolutePath = bookmark.localImagePath?.let { relativePath ->
            bookmarkFileManager.resolve(relativePath).path
        }

        val localIconAbsolutePath = bookmark.localIconPath?.let { relativePath ->
            bookmarkFileManager.resolve(relativePath).path
        }

        return BookmarkPreviewUiModel(
            id = Uuid.parse(bookmark.id),
            folderId = Uuid.parse(bookmark.folderId),
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
        val imageRelativePath = imageBytes?.let { bytes ->
            when (kind) {
                BookmarkKind.LINK -> {
                    val ext = sanitizeExtension(imageExtension, fallback = "jpeg")
                    LinkmarkFileManager.saveLinkImageBytes(bookmarkId, bytes, ext)
                    CoreConstants.FileSystem.Linkmark.linkImagePath(bookmarkId, ext)
                }
                else -> {
                    val ext = sanitizeExtension(imageExtension, fallback = "jpeg")
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

    private fun sanitizeExtension(rawExtension: String?, fallback: String): String =
        rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { fallback }

    private suspend fun recordBookmarkCreationEvents(
        bookmarkId: Uuid,
        json: BookmarkMetaJson,
    ) {
        val changes = mapOf(
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
        )
        crdtEngine.recordFieldChanges(
            objectId = bookmarkId,
            objectType = ObjectType.BOOKMARK,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = VectorClock.empty(),
        )
    }

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
