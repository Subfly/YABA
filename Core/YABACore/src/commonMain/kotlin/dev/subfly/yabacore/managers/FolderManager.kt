package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.domain.FolderDomainModel
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationDraft
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.DropZone
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class FolderManager(
    private val folderDao: FolderDao,
    private val opApplier: OpApplier,
) {
    private val clock = Clock.System
    private val uncategorizedFolderId = Uuid.parse(CoreConstants.Folder.Uncategorized.ID)

    fun observeFolders(
        parentId: Uuid?,
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<FolderUiModel>> =
        folderDao.observeFoldersWithBookmarkCounts(
            parentId,
            sortType.name,
            sortOrder.name
        ).map { rows -> rows.map { it.toUiModel() } }

    fun observeFolderTree(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<FolderUiModel>> =
        folderDao.observeAllFoldersWithBookmarkCounts(
            sortType.name,
            sortOrder.name
        ).map { rows -> buildFolderTree(rows) }

    suspend fun getFolder(folderId: Uuid): FolderUiModel? =
        folderDao.getFolderWithBookmarkCount(folderId)?.toUiModel()

    suspend fun ensureUncategorizedFolder(): FolderUiModel {
        folderDao.getFolderWithBookmarkCount(uncategorizedFolderId)?.let {
            return it.toUiModel()
        }
        val now = clock.now()
        val rootSiblings = loadSiblings(parentId = null)
        val folder =
            FolderDomainModel(
                id = uncategorizedFolderId,
                parentId = null,
                label = CoreConstants.Folder.Uncategorized.NAME,
                description = CoreConstants.Folder.Uncategorized.DESCRIPTION,
                icon = CoreConstants.Folder.Uncategorized.ICON,
                color = YabaColor.NONE,
                createdAt = now,
                editedAt = now,
                order = rootSiblings.size,
            )
        opApplier.applyLocal(listOf(folder.toOperationDraft(OperationKind.CREATE)))
        return folder.toUiModel(bookmarkCount = 0)
    }

    suspend fun createFolder(folder: FolderUiModel): FolderUiModel {
        val now = clock.now()
        val siblings = loadSiblings(folder.parentId)
        val folderId = folder.id
        val newFolder =
            FolderDomainModel(
                id = folderId,
                parentId = folder.parentId,
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                color = folder.color,
                createdAt = now,
                editedAt = now,
                order = siblings.size,
            )
        opApplier.applyLocal(listOf(newFolder.toOperationDraft(OperationKind.CREATE)))
        return newFolder.toUiModel(bookmarkCount = 0)
    }

    suspend fun updateFolder(folder: FolderUiModel): FolderUiModel? {
        val existing = folderDao.getById(folder.id)?.toModel() ?: return null
        val now = clock.now()
        val updated =
            existing.copy(
                label = folder.label,
                description = folder.description,
                icon = folder.icon,
                color = folder.color,
                editedAt = now,
            )
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
        return folderDao.getFolderWithBookmarkCount(folder.id)?.toUiModel()
    }

    suspend fun moveFolder(
        folder: FolderUiModel,
        targetParent: FolderUiModel?,
    ) {
        val current = folderDao.getById(folder.id)?.toModel() ?: return
        val targetParentId = targetParent?.id
        val now = clock.now()
        val targetSiblings = loadSiblings(targetParentId).filterNot { it.id == folder.id }
        val moved =
            current.copy(
                parentId = targetParentId,
                order = targetSiblings.size,
                editedAt = now,
            )
        val drafts = mutableListOf(moved.toOperationDraft(OperationKind.MOVE))
        drafts += normalizeFolderOrders(targetParentId, targetSiblings + moved, now)
        val oldParentId = current.parentId
        if (oldParentId != targetParentId) {
            val oldSiblings = loadSiblings(oldParentId).filterNot { it.id == folder.id }
            drafts += normalizeFolderOrders(oldParentId, oldSiblings, now)
        }
        opApplier.applyLocal(drafts)
    }

    suspend fun reorderFolder(
        dragged: FolderUiModel,
        target: FolderUiModel,
        zone: DropZone,
    ) {
        when (zone) {
            DropZone.NONE -> return
            DropZone.MIDDLE -> {
                moveFolder(dragged, target)
                return
            }

            else -> Unit
        }
        val draggedFolder = folderDao.getById(dragged.id)?.toModel() ?: return
        val parentId = draggedFolder.parentId
        val siblings = loadSiblings(parentId)
        val ordered = reorderSiblings(siblings, dragged.id, target.id, zone)
        val now = clock.now()
        val drafts = normalizeFolderOrders(parentId, ordered, now)
        opApplier.applyLocal(drafts)
    }

    suspend fun deleteFolder(folder: FolderUiModel) {
        val target = folderDao.getById(folder.id)?.toModel() ?: return
        val now = clock.now()
        val drafts =
            mutableListOf(target.copy(editedAt = now).toOperationDraft(OperationKind.DELETE))
        val siblings = loadSiblings(target.parentId).filterNot { it.id == folder.id }
        drafts += normalizeFolderOrders(target.parentId, siblings, now)
        opApplier.applyLocal(drafts)
    }

    suspend fun getMovableFolders(
        currentFolderId: Uuid?,
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): List<FolderUiModel> {
        val excluded =
            if (currentFolderId == null) {
                emptySet()
            } else {
                setOf(currentFolderId) + collectDescendantIds(currentFolderId)
            }
        val rows =
            if (excluded.isEmpty()) {
                folderDao.getMovableFolders(sortType.name, sortOrder.name)
            } else {
                folderDao.getMovableFoldersExcluding(
                    excluded.toList(),
                    sortType.name,
                    sortOrder.name
                )
            }
        return rows.map { it.toUiModel() }
    }

    private suspend fun loadSiblings(parentId: Uuid?): List<FolderDomainModel> =
        if (parentId == null) {
            folderDao.getRoot()
        } else {
            folderDao.getChildren(parentId)
        }
            .map { it.toModel() }

    private fun buildFolderTree(rows: List<FolderWithBookmarkCount>): List<FolderUiModel> {
        val grouped = rows.groupBy { it.folder.parentId }
        fun build(parentId: Uuid?): List<FolderUiModel> =
            grouped[parentId]?.map { entry ->
                entry.toUiModel(children = build(entry.folder.id))
            }
                ?: emptyList()
        return build(null)
    }

    private suspend fun collectDescendantIds(rootId: Uuid): Set<Uuid> {
        val rows =
            folderDao.getAllFoldersWithBookmarkCounts(
                SortType.CUSTOM.name,
                SortOrderType.ASCENDING.name
            )
        val grouped = rows.groupBy { it.folder.parentId }
        val visited = mutableSetOf<Uuid>()
        fun traverse(parentId: Uuid) {
            grouped[parentId]?.forEach { child ->
                val childId = child.folder.id
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
        val insertIndex =
            when (zone) {
                DropZone.TOP -> targetIndex.coerceAtLeast(0)
                DropZone.BOTTOM -> (targetIndex + 1).coerceAtMost(sorted.size)
                else -> -1
            }
        sorted.add(insertIndex, dragged)
        return sorted.mapIndexed { index, folder -> folder.copy(order = index) }
    }

    private fun normalizeFolderOrders(
        parentId: Uuid?,
        folders: List<FolderDomainModel>,
        timestamp: kotlinx.datetime.Instant,
    ): List<OperationDraft> =
        folders
            .sortedBy { it.order }
            .mapIndexed { index, folder ->
                if (folder.order == index && folder.parentId == parentId) {
                    null
                } else {
                    folder.copy(order = index, parentId = parentId, editedAt = timestamp)
                        .toOperationDraft(OperationKind.REORDER)
                }
            }
            .filterNotNull()
}
