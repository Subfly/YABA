package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.dao.TagDao
import dev.subfly.yabacore.database.domain.TagDomainModel
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.tagLinkOperationDraft
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.DropZone
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class TagManager(
    private val tagDao: TagDao,
    private val opApplier: OpApplier,
) {
    private val clock = Clock.System
    private val pinnedTagId = Uuid.parse(CoreConstants.Tag.Pinned.ID)
    private val privateTagId = Uuid.parse(CoreConstants.Tag.Private.ID)

    fun observeTags(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<TagUiModel>> =
        tagDao
            .observeTagsWithBookmarkCounts(sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    fun observeTagsForBookmark(
        bookmarkId: Uuid,
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<TagUiModel>> =
        tagDao
            .observeTagsForBookmarkWithCounts(bookmarkId, sortType.name, sortOrder.name)
            .map { rows -> rows.map { it.toUiModel() } }

    suspend fun getTag(tagId: Uuid): TagUiModel? =
        tagDao.getTagWithBookmarkCount(tagId)?.toUiModel()

    suspend fun createTag(tag: TagUiModel): TagUiModel {
        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val newTag = TagDomainModel(
            id = tag.id,
            label = tag.label,
            icon = tag.icon,
            color = tag.color,
            createdAt = now,
            editedAt = now,
            order = tagsCount,
        )
        opApplier.applyLocal(listOf(newTag.toOperationDraft(OperationKind.CREATE)))
        return newTag.toUiModel()
    }

    suspend fun updateTag(tag: TagUiModel): TagUiModel? {
        val existing = tagDao.getById(tag.id)?.toModel() ?: return null
        val now = clock.now()
        val updated = existing.copy(
            label = tag.label,
            icon = tag.icon,
            color = tag.color,
            editedAt = now,
        )
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
        return tagDao.getTagWithBookmarkCount(tag.id)?.toUiModel()
    }

    suspend fun reorderTag(
        dragged: TagUiModel,
        target: TagUiModel,
        zone: DropZone,
    ) {
        if (zone == DropZone.NONE || zone == DropZone.MIDDLE) {
            return
        }
        val tags = tagDao.getAll().map { it.toModel() }
        val ordered = reorder(tags, dragged.id, target.id, zone)
        val now = clock.now()
        val drafts = ordered
            .sortedBy { it.order }
            .mapIndexedNotNull { index, tag ->
                if (tag.order == index) {
                    null
                } else {
                    tag.copy(order = index, editedAt = now)
                        .toOperationDraft(OperationKind.REORDER)
                }
            }
        if (drafts.isNotEmpty()) {
            opApplier.applyLocal(drafts)
        }
    }

    suspend fun deleteTag(tag: TagUiModel) {
        val target = tagDao.getById(tag.id)?.toModel() ?: return
        val now = clock.now()
        val remaining = tagDao.getAll().map { it.toModel() }.filterNot { it.id == tag.id }
        val drafts =
            mutableListOf(target.copy(editedAt = now).toOperationDraft(OperationKind.DELETE))
        drafts += remaining
            .sortedBy { it.order }
            .mapIndexedNotNull { index, item ->
                if (item.order == index) {
                    null
                } else {
                    item.copy(order = index, editedAt = now)
                        .toOperationDraft(OperationKind.REORDER)
                }
            }
        opApplier.applyLocal(drafts)
    }

    suspend fun addTagToBookmark(tag: TagUiModel, bookmarkId: Uuid) {
        val now = clock.now()
        val draft = tagLinkOperationDraft(tag.id, bookmarkId, OperationKind.TAG_ADD, now)
        opApplier.applyLocal(listOf(draft))
    }

    suspend fun removeTagFromBookmark(tag: TagUiModel, bookmarkId: Uuid) {
        val now = clock.now()
        val draft = tagLinkOperationDraft(tag.id, bookmarkId, OperationKind.TAG_REMOVE, now)
        opApplier.applyLocal(listOf(draft))
    }

    suspend fun ensurePinnedTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(pinnedTagId)?.let {
            return it.toUiModel()
        }
        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val tag = TagDomainModel(
            id = pinnedTagId,
            label = CoreConstants.Tag.Pinned.NAME,
            icon = CoreConstants.Tag.Pinned.ICON,
            color = YabaColor.YELLOW,
            createdAt = now,
            editedAt = now,
            order = tagsCount,
        )
        opApplier.applyLocal(listOf(tag.toOperationDraft(OperationKind.CREATE)))
        return tag.toUiModel(bookmarkCount = 0)
    }

    suspend fun ensurePrivateTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(privateTagId)?.let {
            return it.toUiModel()
        }
        val now = clock.now()
        val tagsCount = tagDao.getAll().size
        val tag = TagDomainModel(
            id = privateTagId,
            label = CoreConstants.Tag.Private.NAME,
            icon = CoreConstants.Tag.Private.ICON,
            color = YabaColor.RED,
            createdAt = now,
            editedAt = now,
            order = tagsCount,
        )
        opApplier.applyLocal(listOf(tag.toOperationDraft(OperationKind.CREATE)))
        return tag.toUiModel(bookmarkCount = 0)
    }

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
        val insertIndex =
            when (zone) {
                DropZone.TOP -> targetIndex.coerceAtLeast(0)
                DropZone.BOTTOM -> (targetIndex + 1).coerceAtMost(sorted.size)
                else -> -1
            }
        sorted.add(insertIndex, dragged)
        return sorted.mapIndexed { index, tag -> tag.copy(order = index) }
    }
}
