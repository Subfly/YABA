package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.FolderEntity
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * DB-first folder manager.
 *
 * All folder metadata is stored in Room. No filesystem metadata.
 */
object FolderManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
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

    // ==================== Write Operations (DB-first) ====================

    private suspend fun ensureUncategorizedFolderInternal() {
        if (folderDao.getFolderWithBookmarkCount(UNCATEGORIZED_FOLDER_ID) != null) {
            return
        }

        val now = clock.now().toEpochMilliseconds()
        val entity = FolderEntity(
            id = UNCATEGORIZED_FOLDER_ID,
            parentId = null,
            label = CoreConstants.Folder.Uncategorized.NAME,
            description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
            icon = CoreConstants.Folder.Uncategorized.ICON,
            color = YabaColor.BLUE,
            createdAt = now,
            editedAt = now,
            isHidden = false,
        )
        folderDao.upsert(entity)
    }

    fun createFolder(folder: FolderUiModel): FolderUiModel {
        CoreOperationQueue.queue("CreateFolder:${folder.id}") {
            createFolderInternal(folder)
        }
        return folder
    }

    private suspend fun createFolderInternal(folder: FolderUiModel) {
        val now = clock.now().toEpochMilliseconds()
        val safeParentId = folder.parentId?.takeUnless { CoreConstants.Folder.isSystemFolder(it) }

        val entity = FolderEntity(
            id = folder.id,
            parentId = safeParentId,
            label = folder.label,
            description = folder.description,
            icon = folder.icon,
            color = folder.color,
            createdAt = now,
            editedAt = now,
            isHidden = false,
        )
        folderDao.upsert(entity)
    }

    fun updateFolder(folder: FolderUiModel): FolderUiModel {
        CoreOperationQueue.queue("UpdateFolder:${folder.id}") {
            updateFolderInternal(folder)
        }
        return folder
    }

    private suspend fun updateFolderInternal(folder: FolderUiModel) {
        val existing = folderDao.getById(folder.id) ?: return
        val now = clock.now().toEpochMilliseconds()

        folderDao.upsert(
            existing.copy(
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                color = folder.color,
                editedAt = now,
            )
        )
    }

    fun moveFolder(folder: FolderUiModel, targetParent: FolderUiModel?) {
        CoreOperationQueue.queue("MoveFolder:${folder.id}") {
            moveFolderInternal(folder, targetParent)
        }
    }

    private suspend fun moveFolderInternal(folder: FolderUiModel, targetParent: FolderUiModel?) {
        if (CoreConstants.Folder.isSystemFolder(folder.id)) return

        val existing = folderDao.getById(folder.id) ?: return
        val targetParentId =
            targetParent?.id?.takeUnless { CoreConstants.Folder.isSystemFolder(it) }
        val now = clock.now().toEpochMilliseconds()

        folderDao.upsert(
            existing.copy(
                parentId = targetParentId,
                editedAt = now,
            )
        )
    }

    fun deleteFolder(folderId: String) {
        if (CoreConstants.Folder.isSystemFolder(folderId)) {
            CoreOperationQueue.queue("DeleteSystemFolder:$folderId") {
                deleteSystemFolderCascadeInternal(folderId)
            }
            return
        }
        CoreOperationQueue.queue("DeleteFolder:$folderId") { deleteFolderCascadeInternal(folderId) }
    }

    private suspend fun deleteSingleFolderInternal(folderId: String) {
        folderDao.deleteById(folderId)
    }

    private suspend fun deleteFolderCascadeInternal(rootFolderId: String) {
        if (CoreConstants.Folder.isSystemFolder(rootFolderId)) return

        val folderIdsToDelete =
            collectFolderSubtreePostOrder(rootFolderId)
                .filterNot { CoreConstants.Folder.isSystemFolder(it) }
        if (folderIdsToDelete.isEmpty()) return

        val folderIdSet = folderIdsToDelete.toSet()
        val bookmarkIdsToDelete =
            bookmarkDao.getAll()
                .asSequence()
                .filter { it.folderId in folderIdSet }
                .map { it.id }
                .toList()

        bookmarkIdsToDelete.forEach { bookmarkId ->
            AllBookmarksManager.deleteBookmarkById(bookmarkId)
        }

        folderIdsToDelete.forEach { folderId ->
            deleteSingleFolderInternal(folderId)
        }
    }

    private suspend fun deleteSystemFolderCascadeInternal(systemFolderId: String) {
        if (!CoreConstants.Folder.isSystemFolder(systemFolderId)) return

        val folderIdsInSubtree = collectFolderSubtreePostOrder(systemFolderId)
        val descendantFolderIds = folderIdsInSubtree.filterNot { it == systemFolderId }
        val folderIdsForBookmarkDeletion = (descendantFolderIds + systemFolderId).toSet()

        val bookmarkIdsToDelete =
            bookmarkDao.getAll()
                .asSequence()
                .filter { it.folderId in folderIdsForBookmarkDeletion }
                .map { it.id }
                .toList()
        bookmarkIdsToDelete.forEach { bookmarkId ->
            AllBookmarksManager.deleteBookmarkById(bookmarkId)
        }

        descendantFolderIds.forEach { folderId ->
            if (!CoreConstants.Folder.isSystemFolder(folderId)) {
                deleteSingleFolderInternal(folderId)
            }
        }

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
        val existing = folderDao.getById(folderId) ?: return

        val updated = existing.copy(
            parentId = null,
            isHidden = isHidden,
            editedAt = clock.now().toEpochMilliseconds(),
        )
        folderDao.upsert(updated)
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
}
