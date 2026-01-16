@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.model.ui.FolderUiModel
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
 * Filesystem-first folder manager.
 *
 * All operations:
 * 1. Write to filesystem JSON first (authoritative)
 * 2. Generate CRDT events for sync
 * 3. Update SQLite cache for queries
 */
object FolderManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System
    private val uncategorizedFolderId = Uuid.parse(CoreConstants.Folder.Uncategorized.ID)

    // ==================== Query Operations (from SQLite cache) ====================

    fun observeFolderTree(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<FolderUiModel>> =
        folderDao.observeAllFoldersWithBookmarkCounts(
            sortType.name,
            sortOrder.name
        ).map { rows -> buildFolderTree(rows) }

    suspend fun getFolder(folderId: Uuid): FolderUiModel? =
        folderDao.getFolderWithBookmarkCount(folderId.toString())?.toUiModel()

    fun observeFolder(folderId: Uuid): Flow<FolderUiModel?> =
        folderDao.observeById(folderId.toString()).map { entity ->
            entity?.toModel()?.toUiModel()
        }

    suspend fun getUncategorizedFolder(): FolderUiModel? =
        folderDao.getFolderWithBookmarkCount(uncategorizedFolderId.toString())?.toUiModel()

    fun createUncategorizedFolderModel(): FolderUiModel {
        val now = clock.now()
        return FolderUiModel(
            id = uncategorizedFolderId,
            parentId = null,
            label = CoreConstants.Folder.Uncategorized.NAME,
            description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
            icon = CoreConstants.Folder.Uncategorized.ICON,
            color = YabaColor.BLUE,
            createdAt = now,
            editedAt = now,
            order = -1,
            bookmarkCount = 0,
        )
    }

    fun observeAllFoldersSorted(
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<FolderUiModel>> =
        folderDao.observeAllFoldersWithBookmarkCounts(sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    suspend fun getMovableFolders(
        currentFolderId: Uuid?,
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): List<FolderUiModel> {
        val excluded = if (currentFolderId == null) {
            emptySet()
        } else {
            setOf(currentFolderId) + collectDescendantIds(currentFolderId)
        }
        val rows = if (excluded.isEmpty()) {
            folderDao.getMovableFolders(sortType.name, sortOrder.name)
        } else {
            folderDao.getMovableFoldersExcluding(
                excluded.map { it.toString() }.toList(),
                sortType.name,
                sortOrder.name
            )
        }
        return rows.map { it.toUiModel() }
    }

    // ==================== Write Operations (Filesystem-First) ====================

    suspend fun ensureUncategorizedFolder(): FolderUiModel {
        // SELF-HEALING: Remove any tombstone for the Uncategorized folder
        // The Uncategorized folder is a system folder that cannot truly be deleted
        if (entityFileManager.isFolderDeleted(uncategorizedFolderId)) {
            entityFileManager.removeFolderTombstone(uncategorizedFolderId)
        }

        // Check if already exists in cache
        folderDao.getFolderWithBookmarkCount(uncategorizedFolderId.toString())?.let {
            return it.toUiModel()
        }

        // Check if exists in filesystem but not in cache (drift recovery)
        val existingJson = entityFileManager.readFolderMeta(uncategorizedFolderId)
        if (existingJson != null) {
            // Folder exists in filesystem but not in cache - restore to cache
            val entity = existingJson.toEntity()
            folderDao.upsert(entity)
            return folderDao.getFolderWithBookmarkCount(uncategorizedFolderId.toString())?.toUiModel()
                ?: existingJson.toUiModel(bookmarkCount = 0)
        }

        // Create new Uncategorized folder
        val now = clock.now()
        val rootSiblings = loadSiblings(parentId = null)
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val folderJson = FolderMetaJson(
            id = uncategorizedFolderId.toString(),
            parentId = null,
            label = CoreConstants.Folder.Uncategorized.NAME,
            description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
            icon = CoreConstants.Folder.Uncategorized.ICON,
            colorCode = YabaColor.BLUE.code,
            order = rootSiblings.size,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Record CRDT events
        recordFolderCreationEvents(uncategorizedFolderId, folderJson)

        // 3. Update SQLite cache
        val entity = folderJson.toEntity()
        folderDao.upsert(entity)

        return folderJson.toUiModel(bookmarkCount = 0)
    }

    suspend fun createFolder(folder: FolderUiModel): FolderUiModel {
        val now = clock.now()
        val folderId = folder.id
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        // Force database warmup by querying siblings
        // This ensures Room has created the database before we write
        val siblings = loadSiblings(folder.parentId)

        val folderJson = FolderMetaJson(
            id = folderId.toString(),
            parentId = folder.parentId?.toString(),
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            colorCode = folder.color.code,
            order = siblings.size,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Record CRDT events
        recordFolderCreationEvents(folderId, folderJson)

        // 3. Update SQLite cache
        val entity = folderJson.toEntity()
        folderDao.upsert(entity)

        // 4. Verify write succeeded - if not, retry once
        val verification = folderDao.getById(folderId.toString())
        if (verification == null) {
            // Retry the upsert
            folderDao.upsert(entity)
        }

        return folderJson.toUiModel(bookmarkCount = 0)
    }

    suspend fun updateFolder(folder: FolderUiModel): FolderUiModel? {
        // Read current state from filesystem
        val existingJson = entityFileManager.readFolderMeta(folder.id) ?: return null
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        // Detect changes
        val changes = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        if (existingJson.label != folder.label) {
            changes["label"] = JsonPrimitive(folder.label)
        }
        if (existingJson.description != folder.description) {
            changes["description"] = CRDTEngine.nullableStringValue(folder.description)
        }
        if (existingJson.icon != folder.icon) {
            changes["icon"] = JsonPrimitive(folder.icon)
        }
        if (existingJson.colorCode != folder.color.code) {
            changes["colorCode"] = JsonPrimitive(folder.color.code)
        }

        if (changes.isEmpty()) {
            return folderDao.getFolderWithBookmarkCount(folder.id.toString())?.toUiModel()
        }

        val newClock = existingClock.increment(deviceId)

        val updatedJson = existingJson.copy(
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            colorCode = folder.color.code,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeFolderMeta(updatedJson)

        // 2. Record CRDT events
        crdtEngine.recordFieldChanges(
            objectId = folder.id,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        folderDao.upsert(updatedJson.toEntity())

        return folderDao.getFolderWithBookmarkCount(folder.id.toString())?.toUiModel()
    }

    suspend fun moveFolder(folder: FolderUiModel, targetParent: FolderUiModel?) {
        val existingJson = entityFileManager.readFolderMeta(folder.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()
        val targetParentId = targetParent?.id

        val targetSiblings = loadSiblings(targetParentId).filterNot { it.id == folder.id }
        val newClock = existingClock.increment(deviceId)

        val movedJson = existingJson.copy(
            parentId = targetParentId?.toString(),
            order = targetSiblings.size,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeFolderMeta(movedJson)

        // 2. Record CRDT events
        val changes = mapOf(
            "parentId" to CRDTEngine.nullableStringValue(targetParentId?.toString()),
            "order" to JsonPrimitive(targetSiblings.size),
        )
        crdtEngine.recordFieldChanges(
            objectId = folder.id,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        folderDao.upsert(movedJson.toEntity())

        // Normalize sibling orders
        normalizeSiblingOrders(targetSiblings.map { it.id } + folder.id, now)

        // Normalize old parent siblings if parent changed
        val oldParentId = existingJson.parentId?.let { Uuid.parse(it) }
        if (oldParentId != targetParentId) {
            val oldSiblings = loadSiblings(oldParentId).filterNot { it.id == folder.id }
            normalizeSiblingOrders(oldSiblings.map { it.id }, now)
        }
    }

    suspend fun reorderFolder(dragged: FolderUiModel, target: FolderUiModel, zone: DropZone) {
        when (zone) {
            DropZone.NONE -> return
            DropZone.MIDDLE -> {
                moveFolder(dragged, target)
                return
            }
            else -> Unit
        }

        val draggedJson = entityFileManager.readFolderMeta(dragged.id) ?: return
        val parentId = draggedJson.parentId?.let { Uuid.parse(it) }
        val siblings = loadSiblings(parentId)
        val ordered = reorderSiblings(siblings, dragged.id, target.id, zone)
        val now = clock.now()

        normalizeSiblingOrdersFromDomain(ordered, now)
    }

    suspend fun deleteFolder(folder: FolderUiModel) {
        // System folders (like Uncategorized) cannot be deleted
        if (CoreConstants.Folder.isSystemFolder(folder.id.toString())) {
            return
        }

        val existingJson = entityFileManager.readFolderMeta(folder.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val deletionClock = existingClock.increment(deviceId)

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteFolder(folder.id, deletionClock)

        // 2. Record deletion event
        crdtEngine.recordFieldChange(
            objectId = folder.id,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            field = "_deleted",
            value = JsonPrimitive(true),
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        folderDao.deleteById(folder.id.toString())

        // Normalize sibling orders
        val parentId = existingJson.parentId?.let { Uuid.parse(it) }
        val siblings = loadSiblings(parentId).filterNot { it.id == folder.id }
        normalizeSiblingOrdersFromDomain(siblings, clock.now())
    }

    // ==================== Private Helpers ====================

    private suspend fun loadSiblings(parentId: Uuid?): List<FolderDomainModel> =
        if (parentId == null) {
            folderDao.getRoot()
        } else {
            folderDao.getChildren(parentId.toString())
        }.map { it.toModel() }

    private fun buildFolderTree(rows: List<FolderWithBookmarkCount>): List<FolderUiModel> {
        val uiRows = rows.map { it.toUiModel() }
        val grouped = uiRows.groupBy { it.parentId }
        fun build(parentId: Uuid?): List<FolderUiModel> =
            grouped[parentId]?.map { entry ->
                entry.copy(children = build(entry.id))
            } ?: emptyList()
        return build(null)
    }

    private suspend fun collectDescendantIds(rootId: Uuid): Set<Uuid> {
        val rows = folderDao.getAllFoldersWithBookmarkCounts(
            SortType.CUSTOM.name,
            SortOrderType.ASCENDING.name
        ).map { it.toUiModel() }
        val grouped = rows.groupBy { it.parentId }
        val visited = mutableSetOf<Uuid>()
        fun traverse(parentId: Uuid) {
            grouped[parentId]?.forEach { child ->
                val childId = child.id
                if (visited.add(childId)) {
                    traverse(childId)
                }
            }
        }
        traverse(rootId)
        return visited
    }

    private fun reorderSiblings(
        siblings: List<FolderDomainModel>,
        draggedId: Uuid,
        targetId: Uuid,
        zone: DropZone,
    ): List<FolderDomainModel> {
        val sorted = siblings.sortedBy { it.order }.toMutableList()
        val dragged = sorted.firstOrNull { it.id == draggedId } ?: return siblings
        val targetIndex = sorted.indexOfFirst { it.id == targetId }
        if (targetIndex == -1) return siblings
        sorted.remove(dragged)
        val insertIndex = when (zone) {
            DropZone.TOP -> targetIndex.coerceAtLeast(0)
            DropZone.BOTTOM -> (targetIndex + 1).coerceAtMost(sorted.size)
            else -> -1
        }
        sorted.add(insertIndex, dragged)
        return sorted.mapIndexed { index, folder -> folder.copy(order = index) }
    }

    private suspend fun normalizeSiblingOrders(
        folderIds: List<Uuid>,
        timestamp: Instant,
    ) {
        val deviceId = DeviceIdProvider.get()
        folderIds.forEachIndexed { index, folderId ->
            val json = entityFileManager.readFolderMeta(folderId) ?: return@forEachIndexed
            if (json.order != index) {
                val existingClock = VectorClock.fromMap(json.clock)
                val newClock = existingClock.increment(deviceId)
                val updatedJson = json.copy(
                    order = index,
                    editedAt = timestamp.toEpochMilliseconds(),
                    clock = newClock.toMap(),
                )
                entityFileManager.writeFolderMeta(updatedJson)
                crdtEngine.recordFieldChange(
                    objectId = folderId,
                    objectType = ObjectType.FOLDER,
                    file = FileTarget.META_JSON,
                    field = "order",
                    value = JsonPrimitive(index),
                    currentClock = existingClock,
                )
                folderDao.upsert(updatedJson.toEntity())
            }
        }
    }

    private suspend fun normalizeSiblingOrdersFromDomain(
        folders: List<FolderDomainModel>,
        timestamp: Instant,
    ) {
        val deviceId = DeviceIdProvider.get()
        folders.sortedBy { it.order }.forEachIndexed { index, folder ->
            if (folder.order != index) {
                val json = entityFileManager.readFolderMeta(folder.id) ?: return@forEachIndexed
                val existingClock = VectorClock.fromMap(json.clock)
                val newClock = existingClock.increment(deviceId)
                val updatedJson = json.copy(
                    order = index,
                    editedAt = timestamp.toEpochMilliseconds(),
                    clock = newClock.toMap(),
                )
                entityFileManager.writeFolderMeta(updatedJson)
                crdtEngine.recordFieldChange(
                    objectId = folder.id,
                    objectType = ObjectType.FOLDER,
                    file = FileTarget.META_JSON,
                    field = "order",
                    value = JsonPrimitive(index),
                    currentClock = existingClock,
                )
                folderDao.upsert(updatedJson.toEntity())
            }
        }
    }

    private suspend fun recordFolderCreationEvents(
        folderId: Uuid,
        json: FolderMetaJson,
    ) {
        val changes = mapOf(
            "id" to JsonPrimitive(json.id),
            "parentId" to CRDTEngine.nullableStringValue(json.parentId),
            "label" to JsonPrimitive(json.label),
            "description" to CRDTEngine.nullableStringValue(json.description),
            "icon" to JsonPrimitive(json.icon),
            "colorCode" to JsonPrimitive(json.colorCode),
            "order" to JsonPrimitive(json.order),
            "createdAt" to JsonPrimitive(json.createdAt),
            "editedAt" to JsonPrimitive(json.editedAt),
        )
        crdtEngine.recordFieldChanges(
            objectId = folderId,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = VectorClock.empty(),
        )
    }

    // ==================== Mappers ====================

    private fun FolderMetaJson.toEntity(): FolderEntity = FolderEntity(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
    )

    private fun FolderMetaJson.toUiModel(bookmarkCount: Int = 0): FolderUiModel = FolderUiModel(
        id = Uuid.parse(id),
        parentId = parentId?.let { Uuid.parse(it) },
        label = label,
        description = description,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        editedAt = Instant.fromEpochMilliseconds(editedAt),
        order = order,
        bookmarkCount = bookmarkCount,
    )
}
