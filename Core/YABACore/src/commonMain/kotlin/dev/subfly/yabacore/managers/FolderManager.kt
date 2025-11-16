package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.model.DropZone
import dev.subfly.yabacore.model.Folder
import dev.subfly.yabacore.model.SortOrderType
import dev.subfly.yabacore.model.SortType
import dev.subfly.yabacore.model.YabaColor
import dev.subfly.yabacore.operations.OpApplier
import dev.subfly.yabacore.operations.OperationDraft
import dev.subfly.yabacore.operations.OperationKind
import dev.subfly.yabacore.operations.toOperationDraft
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class FolderManager(
    private val folderDao: FolderDao,
    private val opApplier: OpApplier,
) {
    private val clock = Clock.System

    fun observeFolders(
        parentId: Uuid?,
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<Folder>> {
        val baseFlow =
            if (parentId == null) {
                folderDao.observeRoot()
            } else {
                folderDao.observeChildren(parentId)
            }
        return baseFlow.map { entities ->
            entities.map { it.toModel() }.sortFolders(sortType, sortOrder)
        }
    }

    suspend fun getFolder(folderId: Uuid): Folder? = folderDao.getById(folderId)?.toModel()

    suspend fun createFolder(
        label: String,
        icon: String,
        color: YabaColor,
        parentId: Uuid?,
        description: String? = null,
    ): Folder {
        val now = clock.now()
        val siblings =
            if (parentId == null) {
                folderDao.getRoot()
            } else {
                folderDao.getChildren(parentId)
            }
                .map { it.toModel() }
        val folder =
            Folder(
                id = Uuid.random(),
                parentId = parentId,
                label = label,
                description = description,
                icon = icon,
                color = color,
                createdAt = now,
                editedAt = now,
                order = siblings.size,
            )
        opApplier.applyLocal(listOf(folder.toOperationDraft(OperationKind.CREATE)))
        return folder
    }

    suspend fun renameFolder(
        folderId: Uuid,
        label: String,
        description: String?,
    ) {
        val folder = getFolder(folderId) ?: return
        val now = clock.now()
        val updated = folder.copy(label = label, description = description, editedAt = now)
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
    }

    suspend fun moveFolder(
        folderId: Uuid,
        targetParentId: Uuid?,
    ) {
        val folder = getFolder(folderId) ?: return
        val now = clock.now()
        val targetSiblings =
            if (targetParentId == null) {
                folderDao.getRoot()
            } else {
                folderDao.getChildren(targetParentId)
            }
                .map { it.toModel() }
                .filterNot { it.id == folderId }
        val moved =
            folder.copy(
                parentId = targetParentId,
                order = targetSiblings.size,
                editedAt = now,
            )
        val drafts = mutableListOf(moved.toOperationDraft(OperationKind.MOVE))
        drafts += normalizeFolderOrders(targetParentId, targetSiblings + moved, now)
        val oldParentId = folder.parentId
        if (oldParentId != targetParentId) {
            val oldSiblings =
                if (oldParentId == null) {
                    folderDao.getRoot()
                } else {
                    folderDao.getChildren(oldParentId)
                }
                    .map { it.toModel() }
                    .filterNot { it.id == folderId }
            drafts += normalizeFolderOrders(oldParentId, oldSiblings, now)
        }
        opApplier.applyLocal(drafts)
    }

    suspend fun reorderFolder(
        draggedId: Uuid,
        targetId: Uuid,
        zone: DropZone,
    ) {
        when (zone) {
            DropZone.NONE -> return
            DropZone.MIDDLE -> {
                moveFolder(draggedId, targetId)
                return
            }

            else -> Unit
        }
        val dragged = getFolder(draggedId) ?: return
        val parentId = dragged.parentId
        val siblings =
            if (parentId == null) {
                folderDao.getRoot()
            } else {
                folderDao.getChildren(parentId)
            }
                .map { it.toModel() }
        val ordered = reorderSiblings(siblings, draggedId, targetId, zone)
        val now = clock.now()
        val drafts = normalizeFolderOrders(parentId, ordered, now)
        opApplier.applyLocal(drafts)
    }

    suspend fun deleteFolder(folderId: Uuid) {
        val folder = getFolder(folderId) ?: return
        val now = clock.now()
        val drafts =
            mutableListOf(folder.copy(editedAt = now).toOperationDraft(OperationKind.DELETE))
        val siblings =
            if (folder.parentId == null) {
                folderDao.getRoot()
            } else {
                folderDao.getChildren(folder.parentId!!)
            }
                .map { it.toModel() }
                .filterNot { it.id == folderId }
        drafts += normalizeFolderOrders(folder.parentId, siblings, now)
        opApplier.applyLocal(drafts)
    }

    private fun List<Folder>.sortFolders(
        sortType: SortType,
        sortOrder: SortOrderType,
    ): List<Folder> {
        val comparator =
            when (sortType) {
                SortType.CUSTOM -> compareBy<Folder> { it.order }
                SortType.CREATED_AT -> compareBy { it.createdAt }
                SortType.EDITED_AT -> compareBy { it.editedAt }
                SortType.LABEL -> compareBy { it.label.lowercase() }
            }
        val sorted = this.sortedWith(comparator)
        return if (sortOrder == SortOrderType.DESCENDING && sortType != SortType.CUSTOM) {
            sorted.reversed()
        } else {
            sorted
        }
    }

    private fun reorderSiblings(
        siblings: List<Folder>,
        draggedId: Uuid,
        targetId: Uuid,
        zone: DropZone,
    ): List<Folder> {
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

    private fun normalizeFolderOrders(
        parentId: Uuid?,
        folders: List<Folder>,
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
