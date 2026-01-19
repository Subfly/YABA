package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.BookmarkMetaJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.DropZone
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Filesystem-first tag manager.
 *
 * All operations:
 * 1. Write to filesystem JSON first (authoritative)
 * 2. Generate CRDT events for sync
 * 3. Update SQLite cache for queries
 */
object TagManager {
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System
    private val pinnedTagId = CoreConstants.Tag.Pinned.ID
    private val privateTagId = CoreConstants.Tag.Private.ID

    // ==================== Query Operations (from SQLite cache) ====================

    fun observeTags(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<TagUiModel>> =
        tagDao
            .observeTagsWithBookmarkCounts(sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    suspend fun getTag(tagId: String): TagUiModel? =
        tagDao.getTagWithBookmarkCount(tagId)?.toUiModel()

    fun observeTag(tagId: String): Flow<TagUiModel?> =
        tagDao.observeById(tagId).map { entity ->
            entity?.toModel()?.toUiModel()
        }

    // ==================== Write Operations (Enqueued) ====================

    /**
     * Enqueues tag creation. Returns immediately with the tag model.
     */
    fun createTag(tag: TagUiModel): TagUiModel {
        CoreOperationQueue.queue("CreateTag:${tag.id}") {
            createTagInternal(tag)
        }
        return tag
    }

    private suspend fun createTagInternal(tag: TagUiModel) {
        val now = clock.now()
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        // Force database warmup by querying existing tags
        // This ensures Room has created the database before we write
        val tagsCount = tagDao.getAll().size

        val tagJson = TagMetaJson(
            id = tag.id,
            label = tag.label,
            icon = tag.icon,
            colorCode = tag.color.code,
            order = tagsCount,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeTagMeta(tagJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = tag.id,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            payload = buildTagCreatePayload(tagJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        val entity = tagJson.toEntity()
        tagDao.upsert(entity)

        // 4. Verify write succeeded - if not, retry once
        val verification = tagDao.getById(tag.id)
        if (verification == null) {
            // Retry the upsert
            tagDao.upsert(entity)
        }
    }

    /**
     * Enqueues tag update. Returns immediately with the tag model.
     */
    fun updateTag(tag: TagUiModel): TagUiModel {
        CoreOperationQueue.queue("UpdateTag:${tag.id}") {
            updateTagInternal(tag)
        }
        return tag
    }

    private suspend fun updateTagInternal(tag: TagUiModel) {
        val existingJson = entityFileManager.readTagMeta(tag.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        // Detect changes
        val changes = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        if (existingJson.label != tag.label) {
            changes["label"] = JsonPrimitive(tag.label)
        }
        if (existingJson.icon != tag.icon) {
            changes["icon"] = JsonPrimitive(tag.icon)
        }
        if (existingJson.colorCode != tag.color.code) {
            changes["colorCode"] = JsonPrimitive(tag.color.code)
        }

        if (changes.isEmpty()) {
            return
        }

        val newClock = existingClock.increment(deviceId)

        val updatedJson = existingJson.copy(
            label = tag.label,
            icon = tag.icon,
            colorCode = tag.color.code,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeTagMeta(updatedJson)

        // 2. Record CRDT UPDATE event
        crdtEngine.recordUpdate(
            objectId = tag.id,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        tagDao.upsert(updatedJson.toEntity())
    }

    /**
     * Enqueues tag reorder operation.
     */
    fun reorderTag(dragged: TagUiModel, target: TagUiModel, zone: DropZone) {
        if (zone == DropZone.NONE || zone == DropZone.MIDDLE) {
            return
        }
        CoreOperationQueue.queue("ReorderTag:${dragged.id}") {
            reorderTagInternal(dragged, target, zone)
        }
    }

    private suspend fun reorderTagInternal(dragged: TagUiModel, target: TagUiModel, zone: DropZone) {
        val tags = tagDao.getAll().map { it.toModel() }
        val ordered = reorder(tags, dragged.id, target.id, zone)
        val now = clock.now()

        normalizeTagOrders(ordered, now)
    }

    /**
     * Deletes a tag and removes it from all bookmarks that reference it.
     *
     * This cascades the deletion by:
     * 1. Finding all bookmarks that have this tag in their tagIds
     * 2. Removing the tag from each bookmark's meta.json (filesystem-first)
     * 3. Writing the tag tombstone
     * 4. Recording CRDT events for sync
     * 5. Updating SQLite cache
     */
    fun deleteTag(tag: TagUiModel) {
        // System tags are never deleted as CRDT entities; they are hidden via UPDATE.
        if (CoreConstants.Tag.isSystemTag(tag.id)) {
            CoreOperationQueue.queue("HideSystemTag:${tag.id}") {
                setSystemTagHiddenStateInternal(tagId = tag.id, isHidden = true)
            }
            return
        }
        CoreOperationQueue.queue("DeleteTag:${tag.id}") {
            deleteTagInternal(tag)
        }
    }

    private suspend fun setSystemTagHiddenStateInternal(
        tagId: String,
        isHidden: Boolean,
    ) {
        if (!CoreConstants.Tag.isSystemTag(tagId)) return
        val existingJson = entityFileManager.readTagMeta(tagId) ?: return
        val now = clock.now()

        if (existingJson.isHidden == isHidden) {
            tagDao.upsert(existingJson.toEntity())
            return
        }

        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)

        val changes = mapOf(
            "isHidden" to JsonPrimitive(isHidden),
            "editedAt" to JsonPrimitive(now.toEpochMilliseconds()),
        )

        val updatedJson = existingJson.copy(
            isHidden = isHidden,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1) Write to filesystem
        entityFileManager.writeTagMeta(updatedJson)

        // 2) Record CRDT UPDATE event (never DELETE for system tags)
        crdtEngine.recordUpdate(
            objectId = tagId,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3) Update cache
        tagDao.upsert(updatedJson.toEntity())
    }

    private suspend fun deleteTagInternal(tag: TagUiModel) {
        val existingJson = entityFileManager.readTagMeta(tag.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val now = clock.now()

        // 1. CASCADE: Remove this tag from all bookmarks that reference it
        // Handle this inline to avoid cross-manager dependencies
        removeTagFromAllBookmarksInternal(tag.id)

        // 2. Write deletion tombstone to filesystem
        entityFileManager.deleteTag(tag.id, existingClock)

        // 3. Record CRDT DELETE event
        crdtEngine.recordDelete(
            objectId = tag.id,
            objectType = ObjectType.TAG,
            currentClock = existingClock,
        )

        // 4. Remove from SQLite cache (tag_bookmarks will cascade due to foreign key)
        tagDao.deleteById(tag.id)

        // 5. Normalize remaining tag orders
        val remaining = tagDao.getAll().map { it.toModel() }
        normalizeTagOrders(remaining, now)
    }

    /**
     * Removes a tag from all bookmarks that reference it.
     * Used during tag deletion for cascade cleanup.
     * TagManager handles this inline to avoid cross-manager dependencies.
     */
    private suspend fun removeTagFromAllBookmarksInternal(tagId: String) {
        val allBookmarkIds = entityFileManager.scanAllBookmarks()
        allBookmarkIds.forEach { bookmarkId ->
            if (entityFileManager.isBookmarkDeleted(bookmarkId)) return@forEach
            val bookmarkMeta = entityFileManager.readBookmarkMeta(bookmarkId) ?: return@forEach

            // Check if this bookmark has the tag
            if (bookmarkMeta.tagIds.contains(tagId)) {
                removeTagFromSingleBookmarkInternal(tagId, bookmarkId)
            }
        }
    }

    /**
     * Removes a tag from a single bookmark.
     * TagManager handles this inline for cascade deletion.
     */
    private suspend fun removeTagFromSingleBookmarkInternal(tagId: String, bookmarkId: String) {
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

        // 1. Write to filesystem
        entityFileManager.writeBookmarkMeta(updatedJson)

        // 2. Record CRDT UPDATE event
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

    /**
     * Ensures the Pinned system tag exists.
     * Uses queueAndAwait to serialize with other operations.
     */
    suspend fun ensurePinnedTag(): TagUiModel {
        // Check if already exists in cache first (fast path, no queueing needed)
        tagDao.getTagWithBookmarkCount(pinnedTagId)?.let {
            return it.toUiModel()
        }

        // Need to create - queue the operation
        CoreOperationQueue.queueAndAwait("EnsurePinnedTag") {
            ensurePinnedTagInternal()
        }

        // Return the tag after creation
        return tagDao.getTagWithBookmarkCount(pinnedTagId)?.toUiModel()
            ?: createPinnedTagModel()
    }

    private suspend fun ensurePinnedTagInternal() {
        // Re-check inside queue
        if (tagDao.getById(pinnedTagId) != null) {
            return
        }

        // SELF-HEALING: Remove any tombstone for system tag
        if (entityFileManager.isTagDeleted(pinnedTagId)) {
            entityFileManager.removeTagTombstone(pinnedTagId)
        }

        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val tagJson = TagMetaJson(
            id = pinnedTagId,
            label = CoreConstants.Tag.Pinned.NAME,
            icon = CoreConstants.Tag.Pinned.ICON,
            colorCode = YabaColor.YELLOW.code,
            order = tagsCount,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeTagMeta(tagJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = pinnedTagId,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            payload = buildTagCreatePayload(tagJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        tagDao.upsert(tagJson.toEntity())
    }

    private fun createPinnedTagModel(): TagUiModel {
        val now = clock.now()
        return TagUiModel(
            id = pinnedTagId,
            label = CoreConstants.Tag.Pinned.NAME,
            icon = CoreConstants.Tag.Pinned.ICON,
            color = YabaColor.YELLOW,
            createdAt = now,
            editedAt = now,
            order = 0,
            bookmarkCount = 0,
        )
    }

    /**
     * Ensures the Private system tag exists.
     * Uses queueAndAwait to serialize with other operations.
     */
    suspend fun ensurePrivateTag(): TagUiModel {
        // Check if already exists in cache first (fast path, no queueing needed)
        tagDao.getTagWithBookmarkCount(privateTagId)?.let {
            return it.toUiModel()
        }

        // Need to create - queue the operation
        CoreOperationQueue.queueAndAwait("EnsurePrivateTag") {
            ensurePrivateTagInternal()
        }

        // Return the tag after creation
        return tagDao.getTagWithBookmarkCount(privateTagId)?.toUiModel()
            ?: createPrivateTagModel()
    }

    private suspend fun ensurePrivateTagInternal() {
        // Re-check inside queue
        if (tagDao.getById(privateTagId) != null) {
            return
        }

        // SELF-HEALING: Remove any tombstone for system tag
        if (entityFileManager.isTagDeleted(privateTagId)) {
            entityFileManager.removeTagTombstone(privateTagId)
        }

        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val tagJson = TagMetaJson(
            id = privateTagId,
            label = CoreConstants.Tag.Private.NAME,
            icon = CoreConstants.Tag.Private.ICON,
            colorCode = YabaColor.RED.code,
            order = tagsCount,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeTagMeta(tagJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = privateTagId,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            payload = buildTagCreatePayload(tagJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        tagDao.upsert(tagJson.toEntity())
    }

    private fun createPrivateTagModel(): TagUiModel {
        val now = clock.now()
        return TagUiModel(
            id = privateTagId,
            label = CoreConstants.Tag.Private.NAME,
            icon = CoreConstants.Tag.Private.ICON,
            color = YabaColor.RED,
            createdAt = now,
            editedAt = now,
            order = 0,
            bookmarkCount = 0,
        )
    }

    // ==================== Private Helpers ====================

    private fun reorder(
        tags: List<TagDomainModel>,
        draggedId: String,
        targetId: String,
        zone: DropZone,
    ): List<TagDomainModel> {
        val sorted = tags.sortedBy { it.order }.toMutableList()
        val dragged = sorted.firstOrNull { it.id == draggedId } ?: return tags
        val targetIndex = sorted.indexOfFirst { it.id == targetId }
        if (targetIndex == -1) return tags
        sorted.remove(dragged)
        val insertIndex = when (zone) {
            DropZone.TOP -> targetIndex.coerceAtLeast(0)
            DropZone.BOTTOM -> (targetIndex + 1).coerceAtMost(sorted.size)
            else -> -1
        }
        sorted.add(insertIndex, dragged)
        return sorted.mapIndexed { index, tag -> tag.copy(order = index) }
    }

    private suspend fun normalizeTagOrders(tags: List<TagDomainModel>, timestamp: Instant) {
        tags.sortedBy { it.order }.forEachIndexed { index, tag ->
            if (tag.order != index) {
                val json = entityFileManager.readTagMeta(tag.id) ?: return@forEachIndexed
                val existingClock = VectorClock.fromMap(json.clock)
                val deviceId = DeviceIdProvider.get()
                val newClock = existingClock.increment(deviceId)
                val updatedJson = json.copy(
                    order = index,
                    editedAt = timestamp.toEpochMilliseconds(),
                    clock = newClock.toMap(),
                )
                entityFileManager.writeTagMeta(updatedJson)
                crdtEngine.recordUpdate(
                    objectId = tag.id,
                    objectType = ObjectType.TAG,
                    file = FileTarget.META_JSON,
                    changes = mapOf("order" to JsonPrimitive(index)),
                    currentClock = existingClock,
                )
                tagDao.upsert(updatedJson.toEntity())
            }
        }
    }

    private fun buildTagCreatePayload(json: TagMetaJson): Map<String, JsonElement> =
        mapOf(
            "id" to JsonPrimitive(json.id),
            "label" to JsonPrimitive(json.label),
            "icon" to JsonPrimitive(json.icon),
            "colorCode" to JsonPrimitive(json.colorCode),
            "order" to JsonPrimitive(json.order),
            "createdAt" to JsonPrimitive(json.createdAt),
            "editedAt" to JsonPrimitive(json.editedAt),
        )

    // ==================== Mappers ====================

    private fun TagMetaJson.toEntity(): TagEntity = TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
        isHidden = isHidden,
    )
}
