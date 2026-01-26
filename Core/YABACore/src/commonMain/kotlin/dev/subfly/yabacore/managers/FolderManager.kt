package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.model.ui.FolderUiModel
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock

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
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val clock = Clock.System
    private const val UNCATEGORIZED_FOLDER_ID = CoreConstants.Folder.Uncategorized.ID

    suspend fun getFolder(folderId: String): FolderUiModel? =
        folderDao.getFolderWithBookmarkCount(folderId)?.toUiModel()

    fun observeFolder(folderId: String): Flow<FolderUiModel?> =
        folderDao.observeById(folderId).map { entity ->
            entity?.toUiModel()
        }


    /**
     * Ensures the Uncategorized system folder exists AND is visible.
     *
     * Rules:
     * - System folders are never deleted as CRDT entities
     * - Visibility is toggled via UPDATE events (isHidden true/false)
     */
    suspend fun ensureUncategorizedFolderVisible(): FolderUiModel {
        CoreOperationQueue.queueAndAwait("EnsureUncategorizedFolderVisible") {
            ensureUncategorizedFolderInternal()
            setSystemFolderHiddenStateInternal(
                folderId = UNCATEGORIZED_FOLDER_ID,
                isHidden = false,
            )
        }

        return folderDao.getFolderWithBookmarkCount(UNCATEGORIZED_FOLDER_ID)
            ?.toUiModel()
            ?: createUncategorizedFolderModel()
    }

    fun createUncategorizedFolderModel(): FolderUiModel {
        val now = clock.now()
        return FolderUiModel(
            id = UNCATEGORIZED_FOLDER_ID,
            parentId = null,
            label = CoreConstants.Folder.Uncategorized.NAME,
            description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
            icon = CoreConstants.Folder.Uncategorized.ICON,
            color = YabaColor.BLUE,
            createdAt = now,
            editedAt = now,
            bookmarkCount = 0,
        )
    }

    fun observeAllFoldersSorted(
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<FolderUiModel>> =
        folderDao.observeFolders(sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    suspend fun getMovableFolders(
        currentFolderId: String?,
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): List<FolderUiModel> {
        val excluded = if (currentFolderId == null) {
            emptySet()
        } else {
            setOf(currentFolderId) + collectDescendantIds(currentFolderId)
        }
        val excludedList = excluded.toList()
        val rows = folderDao.getFolders(
            sortType = sortType.name,
            sortOrder = sortOrder.name,
            excludedIds = excludedList,
            excludedIdsCount = excludedList.size
        )
        return rows
            .asSequence()
            .map { it.toUiModel() }
            .filterNot { CoreConstants.Folder.isSystemFolder(it.id) }
            .toList()
    }

    // ==================== Write Operations (Filesystem-First) ====================

    private suspend fun ensureUncategorizedFolderInternal() {
        // SELF-HEALING: Remove any tombstone for the Uncategorized folder
        // The Uncategorized folder is a system folder that cannot truly be deleted
        if (entityFileManager.isFolderDeleted(UNCATEGORIZED_FOLDER_ID)) {
            entityFileManager.removeFolderTombstone(UNCATEGORIZED_FOLDER_ID)
        }

        // Check if already exists in cache (re-check inside queue)
        if (folderDao.getFolderWithBookmarkCount(UNCATEGORIZED_FOLDER_ID) != null) {
            return
        }

        // Check if exists in filesystem but not in cache (drift recovery)
        val existingJson = entityFileManager.readFolderMeta(UNCATEGORIZED_FOLDER_ID)
        if (existingJson != null) {
            // Enforce rule: system folders must always be root-level
            if (existingJson.parentId != null) {
                val existingClock = VectorClock.fromMap(existingJson.clock)
                val deviceId = DeviceIdProvider.get()
                val now = clock.now()
                val newClock = existingClock.increment(deviceId)
                val fixedJson = existingJson.copy(
                    parentId = null,
                    editedAt = now.toEpochMilliseconds(),
                    clock = newClock.toMap(),
                )
                entityFileManager.writeFolderMeta(fixedJson)
                crdtEngine.recordUpdate(
                    objectId = UNCATEGORIZED_FOLDER_ID,
                    objectType = ObjectType.FOLDER,
                    file = FileTarget.META_JSON,
                    changes = mapOf(
                        "parentId" to CRDTEngine.nullableStringValue(null),
                        "editedAt" to JsonPrimitive(now.toEpochMilliseconds()),
                    ),
                    currentClock = existingClock,
                )
                folderDao.upsert(fixedJson.toEntity())
                return
            }
            // Folder exists in filesystem but not in cache - restore to cache
            val entity = existingJson.toEntity()
            folderDao.upsert(entity)
            return
        }

        // Create new Uncategorized folder
        val now = clock.now()
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        val folderJson = FolderMetaJson(
            id = UNCATEGORIZED_FOLDER_ID,
            parentId = null,
            label = CoreConstants.Folder.Uncategorized.NAME,
            description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
            icon = CoreConstants.Folder.Uncategorized.ICON,
            colorCode = YabaColor.BLUE.code,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = UNCATEGORIZED_FOLDER_ID,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            payload = buildFolderCreatePayload(folderJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        val entity = folderJson.toEntity()
        folderDao.upsert(entity)
    }

    /**
     * Enqueues folder creation. Returns immediately with the folder model.
     * Actual persistence happens asynchronously in the queue.
     */
    fun createFolder(folder: FolderUiModel): FolderUiModel {
        CoreOperationQueue.queue("CreateFolder:${folder.id}") {
            createFolderInternal(folder)
        }
        return folder
    }

    private suspend fun createFolderInternal(folder: FolderUiModel) {
        val now = clock.now()
        val folderId = folder.id
        val deviceId = DeviceIdProvider.get()
        val initialClock = VectorClock.of(deviceId, 1)

        // Prevent nesting under system folders (system folders must stay root-level, no children)
        val safeParentId = folder.parentId?.takeUnless { CoreConstants.Folder.isSystemFolder(it) }

        val folderJson = FolderMetaJson(
            id = folderId,
            parentId = safeParentId,
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            colorCode = folder.color.code,
            createdAt = now.toEpochMilliseconds(),
            editedAt = now.toEpochMilliseconds(),
            clock = initialClock.toMap(),
        )

        // 1. Write to filesystem (authoritative)
        entityFileManager.writeFolderMeta(folderJson)

        // 2. Record CRDT CREATE event
        crdtEngine.recordCreate(
            objectId = folderId,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            payload = buildFolderCreatePayload(folderJson),
            currentClock = VectorClock.empty(),
        )

        // 3. Update SQLite cache
        val entity = folderJson.toEntity()
        folderDao.upsert(entity)
    }

    /**
     * Enqueues folder update. Returns immediately with the folder model.
     * Actual persistence happens asynchronously in the queue.
     */
    fun updateFolder(folder: FolderUiModel): FolderUiModel {
        CoreOperationQueue.queue("UpdateFolder:${folder.id}") {
            updateFolderInternal(folder)
        }
        return folder
    }

    private suspend fun updateFolderInternal(folder: FolderUiModel) {
        // Read current state from filesystem
        val existingJson = entityFileManager.readFolderMeta(folder.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()

        // Detect changes
        val changes = mutableMapOf<String, JsonElement>()
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
            return
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

        // 2. Record CRDT UPDATE event
        crdtEngine.recordUpdate(
            objectId = folder.id,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        folderDao.upsert(updatedJson.toEntity())
    }

    /**
     * Enqueues folder move operation.
     */
    fun moveFolder(folder: FolderUiModel, targetParent: FolderUiModel?) {
        CoreOperationQueue.queue("MoveFolder:${folder.id}") {
            moveFolderInternal(folder, targetParent)
        }
    }

    private suspend fun moveFolderInternal(folder: FolderUiModel, targetParent: FolderUiModel?) {
        // System folders are always root-level and cannot be moved.
        if (CoreConstants.Folder.isSystemFolder(folder.id)) return

        val existingJson = entityFileManager.readFolderMeta(folder.id) ?: return
        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val now = clock.now()
        // Prevent nesting into system folders (system folders must stay root-level, no children)
        val targetParentId =
            targetParent?.id?.takeUnless { CoreConstants.Folder.isSystemFolder(it) }

        val newClock = existingClock.increment(deviceId)

        val movedJson = existingJson.copy(
            parentId = targetParentId,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1. Write to filesystem
        entityFileManager.writeFolderMeta(movedJson)

        // 2. Record CRDT UPDATE event
        val changes = mapOf(
            "parentId" to CRDTEngine.nullableStringValue(targetParentId),
        )
        crdtEngine.recordUpdate(
            objectId = folder.id,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3. Update SQLite cache
        folderDao.upsert(movedJson.toEntity())
    }


    /**
     * Enqueues folder deletion by ID with cascade. System folders cannot be deleted.
     *
     * This always cascades to delete:
     * - All descendant folders
     * - All bookmarks contained in the folder and its descendants
     * - All related cache rows (tag_bookmarks, link_bookmarks)
     */
    fun deleteFolder(folderId: String) {
        if (CoreConstants.Folder.isSystemFolder(folderId)) {
            CoreOperationQueue.queue("DeleteSystemFolder:$folderId") {
                deleteSystemFolderCascadeInternal(folderId)
            }
            return
        }
        CoreOperationQueue.queue("DeleteFolder:$folderId") { deleteFolderCascadeInternal(folderId) }
    }

    /**
     * Deletes a single folder (without cascade).
     * Internal helper - use deleteFolderCascadeInternal for full deletion.
     */
    private suspend fun deleteSingleFolderInternal(folderId: String) {
        val existingJson = entityFileManager.readFolderMeta(folderId)
        val existingClock =
            existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // 1. Write deletion tombstone to filesystem
        entityFileManager.deleteFolder(folderId, existingClock)

        // 2. Record CRDT DELETE event
        crdtEngine.recordDelete(
            objectId = folderId,
            objectType = ObjectType.FOLDER,
            currentClock = existingClock,
        )

        // 3. Remove from SQLite cache
        folderDao.deleteById(folderId)
    }

    /**
     * Deletes a single bookmark (inline, for cascade deletion).
     * FolderManager handles this directly to avoid cross-manager dependencies.
     */
    private suspend fun deleteSingleBookmarkInternal(bookmarkId: String) {
        val existingJson = entityFileManager.readBookmarkMeta(bookmarkId)
        val existingClock =
            existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // 1. Write deletion tombstone to filesystem
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
        bookmarkDao.deleteByIds(listOf(bookmarkId))
    }

    /**
     * Deletes a folder and everything under it (cascade deletion):
     * - All descendant folders
     * - All bookmarks contained in any of those folders
     * - All related cache rows (tag_bookmarks, link_bookmarks)
     *
     * Then normalizes sibling orders for the parent folder.
     */
    private suspend fun deleteFolderCascadeInternal(rootFolderId: String) {
        if (CoreConstants.Folder.isSystemFolder(rootFolderId)) return

        // Collect folders to delete in post-order (children before parents)
        val folderIdsToDelete =
            collectFolderSubtreePostOrder(rootFolderId)
                .filterNot { CoreConstants.Folder.isSystemFolder(it) }
        if (folderIdsToDelete.isEmpty()) return

        // 1) Delete bookmarks belonging to any folder in the subtree
        val folderIdSet = folderIdsToDelete.toSet()
        val bookmarkIdsToDelete =
            bookmarkDao.getAll()
                .asSequence()
                .filter { it.folderId in folderIdSet }
                .map { it.id }
                .toList()

        // Delete bookmarks inline (no cross-manager call)
        bookmarkIdsToDelete.forEach { bookmarkId ->
            deleteSingleBookmarkInternal(bookmarkId)
        }

        // 2) Delete folders (bottom-up)
        folderIdsToDelete.forEach { folderId ->
            deleteSingleFolderInternal(folderId)
        }
    }

    /**
     * "Deletes" a system folder by:
     * - Hard-deleting all descendant folders (non-system)
     * - Hard-deleting all bookmarks in the folder and its descendants
     * - Marking the system folder itself as hidden (and recording a DELETE event for sync)
     */
    private suspend fun deleteSystemFolderCascadeInternal(systemFolderId: String) {
        if (!CoreConstants.Folder.isSystemFolder(systemFolderId)) return

        val folderIdsInSubtree = collectFolderSubtreePostOrder(systemFolderId)
        val descendantFolderIds = folderIdsInSubtree.filterNot { it == systemFolderId }
        val folderIdsForBookmarkDeletion = (descendantFolderIds + systemFolderId).toSet()

        // 1) Delete bookmarks in system folder + descendants
        val bookmarkIdsToDelete =
            bookmarkDao.getAll()
                .asSequence()
                .filter { it.folderId in folderIdsForBookmarkDeletion }
                .map { it.id }
                .toList()
        bookmarkIdsToDelete.forEach { bookmarkId ->
            deleteSingleBookmarkInternal(bookmarkId)
        }

        // 2) Delete descendant folders (bottom-up; they are all non-system)
        descendantFolderIds.forEach { folderId ->
            if (!CoreConstants.Folder.isSystemFolder(folderId)) {
                deleteSingleFolderInternal(folderId)
            }
        }

        // 3) Hide the system folder (but keep it in filesystem + DB) via UPDATE (never DELETE)
        setSystemFolderHiddenStateInternal(
            folderId = systemFolderId,
            isHidden = true,
        )
    }

    private suspend fun setSystemFolderHiddenStateInternal(
        folderId: String,
        isHidden: Boolean,
    ) {
        if (!CoreConstants.Folder.isSystemFolder(folderId)) return
        val existingJson = entityFileManager.readFolderMeta(folderId) ?: return
        val now = clock.now()

        // System folder invariant: always root-level
        val shouldFixParent = existingJson.parentId != null
        val alreadyCorrect = existingJson.isHidden == isHidden && !shouldFixParent
        if (alreadyCorrect) {
            folderDao.upsert(existingJson.toEntity())
            return
        }

        val existingClock = VectorClock.fromMap(existingJson.clock)
        val deviceId = DeviceIdProvider.get()
        val newClock = existingClock.increment(deviceId)

        val changes = mutableMapOf<String, JsonElement>()
        if (existingJson.isHidden != isHidden) changes["isHidden"] = JsonPrimitive(isHidden)
        if (shouldFixParent) changes["parentId"] = CRDTEngine.nullableStringValue(null)
        changes["editedAt"] = JsonPrimitive(now.toEpochMilliseconds())

        val updatedJson = existingJson.copy(
            parentId = null,
            isHidden = isHidden,
            editedAt = now.toEpochMilliseconds(),
            clock = newClock.toMap(),
        )

        // 1) Write to filesystem
        entityFileManager.writeFolderMeta(updatedJson)

        // 2) Record CRDT UPDATE event (never DELETE for system folders)
        crdtEngine.recordUpdate(
            objectId = folderId,
            objectType = ObjectType.FOLDER,
            file = FileTarget.META_JSON,
            changes = changes,
            currentClock = existingClock,
        )

        // 3) Update SQLite cache
        folderDao.upsert(updatedJson.toEntity())
    }

    private suspend fun collectFolderSubtreePostOrder(rootId: String): List<String> {
        val folders = folderDao.getAllIncludingHidden()
        val grouped = folders.groupBy { it.parentId }
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun traverse(currentId: String) {
            if (!visited.add(currentId)) return
            grouped[currentId]?.forEach { child ->
                traverse(child.id)
            }
            result.add(currentId)
        }

        traverse(rootId)
        return result
    }

    // ==================== Private Helpers ====================

    private suspend fun collectDescendantIds(rootId: String): Set<String> {
        val rows = folderDao.getFolders(
            SortType.LABEL.name,
            SortOrderType.ASCENDING.name
        ).map { it.toUiModel() }
        val grouped = rows.groupBy { it.parentId }
        val visited = mutableSetOf<String>()
        fun traverse(parentId: String) {
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


    private fun buildFolderCreatePayload(json: FolderMetaJson): Map<String, JsonElement> =
        mapOf(
            "id" to JsonPrimitive(json.id),
            "parentId" to CRDTEngine.nullableStringValue(json.parentId),
            "label" to JsonPrimitive(json.label),
            "description" to CRDTEngine.nullableStringValue(json.description),
            "icon" to JsonPrimitive(json.icon),
            "colorCode" to JsonPrimitive(json.colorCode),
            "createdAt" to JsonPrimitive(json.createdAt),
            "editedAt" to JsonPrimitive(json.editedAt),
        )

    // ==================== Mappers ====================

    private fun FolderMetaJson.toEntity(): FolderEntity = FolderEntity(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        createdAt = createdAt,
        editedAt = editedAt,
        isHidden = isHidden,
    )
}
