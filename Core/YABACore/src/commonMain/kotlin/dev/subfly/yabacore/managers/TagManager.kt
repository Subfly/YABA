@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.DropZone
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System
    private val pinnedTagId = Uuid.parse(CoreConstants.Tag.Pinned.ID)
    private val privateTagId = Uuid.parse(CoreConstants.Tag.Private.ID)

    // ==================== Query Operations (from SQLite cache) ====================

    fun observeTags(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<TagUiModel>> =
        tagDao
            .observeTagsWithBookmarkCounts(sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    suspend fun getTag(tagId: Uuid): TagUiModel? =
        tagDao.getTagWithBookmarkCount(tagId.toString())?.toUiModel()

    fun observeTag(tagId: Uuid): Flow<TagUiModel?> =
        tagDao.observeById(tagId.toString()).map { entity ->
            entity?.toModel()?.toUiModel()
        }

    // ==================== Write Operations (Filesystem-First) ====================

    suspend fun createTag(tag: TagUiModel): TagUiModel {
        val now = clock.now()
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        // Force database warmup by querying existing tags
        // This ensures Room has created the database before we write
        val tagsCount = tagDao.getAll().size

        val tagJson = TagMetaJson(
            id = tag.id.toString(),
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

        // 2. Record CRDT events
        recordTagCreationEvents(tag.id, tagJson)

        // 3. Update SQLite cache
        val entity = tagJson.toEntity()
        tagDao.upsert(entity)

        // 4. Verify write succeeded - if not, retry once
        val verification = tagDao.getById(tag.id.toString())
        if (verification == null) {
            // Retry the upsert
            tagDao.upsert(entity)
        }

        return tagJson.toUiModel()
    }

    suspend fun updateTag(tag: TagUiModel): TagUiModel? {
        val existingJson = entityFileManager.readTagMeta(tag.id) ?: return null
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
            return tagDao.getTagWithBookmarkCount(tag.id.toString())?.toUiModel()
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

        // 2. Record CRDT events
        crdtEngine.recordFieldChanges(
            objectId = tag.id,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        tagDao.upsert(updatedJson.toEntity())

        return tagDao.getTagWithBookmarkCount(tag.id.toString())?.toUiModel()
    }

    suspend fun reorderTag(dragged: TagUiModel, target: TagUiModel, zone: DropZone) {
        if (zone == DropZone.NONE || zone == DropZone.MIDDLE) {
            return
        }

        val tags = tagDao.getAll().map { it.toModel() }
        val ordered = reorder(tags, dragged.id, target.id, zone)
        val now = clock.now()

        normalizeTagOrders(ordered, now)
    }

    suspend fun deleteTag(tag: TagUiModel) {
        val existingJson = entityFileManager.readTagMeta(tag.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val deletionClock = existingClock.increment(deviceId)
        val now = clock.now()

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteTag(tag.id, deletionClock)

        // 2. Record deletion event
        crdtEngine.recordFieldChange(
            objectId = tag.id,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            field = "_deleted",
            value = JsonPrimitive(true),
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        tagDao.deleteById(tag.id.toString())

        // Normalize remaining tag orders
        val remaining = tagDao.getAll().map { it.toModel() }
        normalizeTagOrders(remaining, now)
    }

    suspend fun addTagToBookmark(tag: TagUiModel, bookmarkId: Uuid) {
        // Update SQLite cache for tag-bookmark relationship
        tagBookmarkDao.insert(
            TagBookmarkCrossRef(
                tagId = tag.id.toString(),
                bookmarkId = bookmarkId.toString(),
            )
        )

        // Note: The bookmark's tagIds list in meta.json should be updated
        // by the bookmark manager when tags change
    }

    suspend fun removeTagFromBookmark(tag: TagUiModel, bookmarkId: Uuid) {
        // Update SQLite cache for tag-bookmark relationship
        tagBookmarkDao.delete(
            bookmarkId = bookmarkId.toString(),
            tagId = tag.id.toString(),
        )

        // Note: The bookmark's tagIds list in meta.json should be updated
        // by the bookmark manager when tags change
    }

    suspend fun ensurePinnedTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(pinnedTagId.toString())?.let {
            return it.toUiModel()
        }

        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val tagJson = TagMetaJson(
            id = pinnedTagId.toString(),
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

        // 2. Record CRDT events
        recordTagCreationEvents(pinnedTagId, tagJson)

        // 3. Update SQLite cache
        tagDao.upsert(tagJson.toEntity())

        return tagJson.toUiModel(bookmarkCount = 0)
    }

    suspend fun ensurePrivateTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(privateTagId.toString())?.let {
            return it.toUiModel()
        }

        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val tagJson = TagMetaJson(
            id = privateTagId.toString(),
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

        // 2. Record CRDT events
        recordTagCreationEvents(privateTagId, tagJson)

        // 3. Update SQLite cache
        tagDao.upsert(tagJson.toEntity())

        return tagJson.toUiModel(bookmarkCount = 0)
    }

    // ==================== Private Helpers ====================

    private fun reorder(
        tags: List<TagDomainModel>,
        draggedId: Uuid,
        targetId: Uuid,
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
        val deviceId = DeviceIdProvider.get()
        tags.sortedBy { it.order }.forEachIndexed { index, tag ->
            if (tag.order != index) {
                val json = entityFileManager.readTagMeta(tag.id) ?: return@forEachIndexed
                val existingClock = VectorClock.fromMap(json.clock)
                val newClock = existingClock.increment(deviceId)
                val updatedJson = json.copy(
                    order = index,
                    editedAt = timestamp.toEpochMilliseconds(),
                    clock = newClock.toMap(),
                )
                entityFileManager.writeTagMeta(updatedJson)
                crdtEngine.recordFieldChange(
                    objectId = tag.id,
                    objectType = ObjectType.TAG,
                    file = FileTarget.META_JSON,
                    field = "order",
                    value = JsonPrimitive(index),
                    currentClock = existingClock,
                )
                tagDao.upsert(updatedJson.toEntity())
            }
        }
    }

    private suspend fun recordTagCreationEvents(
        tagId: Uuid,
        json: TagMetaJson,
    ) {
        val changes = mapOf(
            "id" to JsonPrimitive(json.id),
            "label" to JsonPrimitive(json.label),
            "icon" to JsonPrimitive(json.icon),
            "colorCode" to JsonPrimitive(json.colorCode),
            "order" to JsonPrimitive(json.order),
            "createdAt" to JsonPrimitive(json.createdAt),
            "editedAt" to JsonPrimitive(json.editedAt),
        )
        crdtEngine.recordFieldChanges(
            objectId = tagId,
            objectType = ObjectType.TAG,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = VectorClock.empty(),
        )
    }

    // ==================== Mappers ====================

    private fun TagMetaJson.toEntity(): TagEntity = TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
    )

    private fun TagMetaJson.toUiModel(bookmarkCount: Int = 0): TagUiModel = TagUiModel(
        id = Uuid.parse(id),
        label = label,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        editedAt = Instant.fromEpochMilliseconds(editedAt),
        order = order,
        bookmarkCount = bookmarkCount,
    )
}
