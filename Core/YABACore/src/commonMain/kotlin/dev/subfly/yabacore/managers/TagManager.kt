package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.dao.TagDao
import dev.subfly.yabacore.database.mappers.toModel
import dev.subfly.yabacore.model.DropZone
import dev.subfly.yabacore.model.SortOrderType
import dev.subfly.yabacore.model.SortType
import dev.subfly.yabacore.model.Tag
import dev.subfly.yabacore.model.YabaColor
import dev.subfly.yabacore.operations.OpApplier
import dev.subfly.yabacore.operations.OperationDraft
import dev.subfly.yabacore.operations.OperationEntityType
import dev.subfly.yabacore.operations.OperationKind
import dev.subfly.yabacore.operations.TagLinkPayload
import dev.subfly.yabacore.operations.toOperationDraft
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

    fun observeTags(
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<Tag>> =
        tagDao.observeAll().map { entities ->
            entities.map { it.toModel() }.sortTags(sortType, sortOrder)
        }

    fun observeTagsForBookmark(
        bookmarkId: Uuid,
        sortType: SortType = SortType.CUSTOM,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
    ): Flow<List<Tag>> =
        tagDao.observeTagsForBookmark(bookmarkId).map { entities ->
            entities.map { it.toModel() }.sortTags(sortType, sortOrder)
        }

    suspend fun createTag(
        label: String,
        icon: String,
        color: YabaColor,
    ): Tag {
        val now = clock.now()
        val tags = tagDao.getAll().map { it.toModel() }
        val tag =
            Tag(
                id = Uuid.random(),
                label = label,
                icon = icon,
                color = color,
                createdAt = now,
                editedAt = now,
                order = tags.size,
            )
        opApplier.applyLocal(listOf(tag.toOperationDraft(OperationKind.CREATE)))
        return tag
    }

    suspend fun renameTag(tagId: Uuid, label: String) {
        val tag = tagDao.getById(tagId)?.toModel() ?: return
        val updated = tag.copy(label = label, editedAt = clock.now())
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
    }

    suspend fun changeTagAppearance(
        tagId: Uuid,
        icon: String,
        color: YabaColor,
    ) {
        val tag = tagDao.getById(tagId)?.toModel() ?: return
        val updated = tag.copy(icon = icon, color = color, editedAt = clock.now())
        opApplier.applyLocal(listOf(updated.toOperationDraft(OperationKind.UPDATE)))
    }

    suspend fun reorderTag(
        draggedId: Uuid,
        targetId: Uuid,
        zone: DropZone,
    ) {
        if (zone == DropZone.NONE || zone == DropZone.MIDDLE) {
            return
        }
        val tags = tagDao.getAll().map { it.toModel() }
        val ordered = reorder(tags, draggedId, targetId, zone)
        val now = clock.now()
        val drafts =
            ordered
                .sortedBy { it.order }
                .mapIndexedNotNull { index, tag ->
                    if (tag.order == index) null else tag.copy(order = index, editedAt = now)
                        .toOperationDraft(OperationKind.REORDER)
                }
        if (drafts.isNotEmpty()) {
            opApplier.applyLocal(drafts)
        }
    }

    suspend fun deleteTag(tagId: Uuid) {
        val tag = tagDao.getById(tagId)?.toModel() ?: return
        val now = clock.now()
        val remaining = tagDao.getAll().map { it.toModel() }.filterNot { it.id == tagId }
        val drafts = mutableListOf(tag.copy(editedAt = now).toOperationDraft(OperationKind.DELETE))
        drafts +=
            remaining
                .sortedBy { it.order }
                .mapIndexedNotNull { index, tag ->
                    if (tag.order == index) null else tag.copy(order = index, editedAt = now)
                        .toOperationDraft(OperationKind.REORDER)
                }
        opApplier.applyLocal(drafts)
    }

    suspend fun addTagToBookmark(tagId: Uuid, bookmarkId: Uuid) {
        val now = clock.now()
        val draft = tagLinkDraft(tagId, bookmarkId, OperationKind.TAG_ADD, now)
        opApplier.applyLocal(listOf(draft))
    }

    suspend fun removeTagFromBookmark(tagId: Uuid, bookmarkId: Uuid) {
        val now = clock.now()
        val draft = tagLinkDraft(tagId, bookmarkId, OperationKind.TAG_REMOVE, now)
        opApplier.applyLocal(listOf(draft))
    }

    private fun List<Tag>.sortTags(sortType: SortType, sortOrder: SortOrderType): List<Tag> {
        val comparator =
            when (sortType) {
                SortType.CUSTOM -> compareBy<Tag> { it.order }
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

    private fun reorder(
        tags: List<Tag>,
        draggedId: Uuid,
        targetId: Uuid,
        zone: DropZone,
    ): List<Tag> {
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

    private fun tagLinkDraft(
        tagId: Uuid,
        bookmarkId: Uuid,
        kind: OperationKind,
        happenedAt: kotlinx.datetime.Instant,
    ): OperationDraft =
        OperationDraft(
            entityType = OperationEntityType.TAG_LINK,
            entityId = "${tagId}|${bookmarkId}",
            kind = kind,
            happenedAt = happenedAt,
            payload = TagLinkPayload(
                tagId = tagId.toString(),
                bookmarkId = bookmarkId.toString(),
            ),
        )
}
