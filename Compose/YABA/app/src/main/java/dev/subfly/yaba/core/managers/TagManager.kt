package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.TagEntity
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.queue.CoreOperationQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * DB-first tag manager.
 *
 * All tag metadata is stored in Room. No filesystem metadata.
 */
object TagManager {
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val clock = Clock.System
    private const val PINNED_TAG_ID = CoreConstants.Tag.Pinned.ID
    private const val PRIVATE_TAG_ID = CoreConstants.Tag.Private.ID

    /**
     * @param includeEmptySystemTags When false (default), system tags (Pinned, Private) with zero
     * bookmarks are omitted from the list. Pass true only if a caller needs the full list
     * regardless of count (rare).
     */
    fun observeTags(
        sortType: SortType = SortType.LABEL,
        sortOrder: SortOrderType = SortOrderType.ASCENDING,
        includeEmptySystemTags: Boolean = false,
    ): Flow<List<TagUiModel>> =
        tagDao
            .observeTagsWithBookmarkCounts(sortType.name, sortOrder.name)
            .map { rows ->
                rows.map { it.toUiModel() }
                    .let { tags ->
                        if (includeEmptySystemTags) tags
                        else tags.filterNot { tag ->
                            CoreConstants.Tag.isSystemTag(tag.id) && tag.bookmarkCount == 0
                        }
                    }
            }

    suspend fun getTag(tagId: String): TagUiModel? =
        tagDao.getTagWithBookmarkCount(tagId)?.toUiModel()

    fun observeTag(tagId: String): Flow<TagUiModel?> =
        tagDao.observeById(tagId).map { entity ->
            entity?.toUiModel()
        }

    fun createTag(tag: TagUiModel): TagUiModel {
        CoreOperationQueue.queue("CreateTag:${tag.id}") {
            createTagInternal(tag)
        }
        return tag
    }

    private suspend fun createTagInternal(tag: TagUiModel) {
        val now = clock.now().toEpochMilliseconds()

        val entity = TagEntity(
            id = tag.id,
            label = tag.label,
            icon = tag.icon,
            color = tag.color,
            createdAt = now,
            editedAt = now,
            isHidden = false,
        )
        tagDao.upsert(entity)
    }

    fun updateTag(tag: TagUiModel): TagUiModel {
        CoreOperationQueue.queue("UpdateTag:${tag.id}") {
            updateTagInternal(tag)
        }
        return tag
    }

    private suspend fun updateTagInternal(tag: TagUiModel) {
        val existing = tagDao.getById(tag.id) ?: return
        val now = clock.now().toEpochMilliseconds()

        tagDao.upsert(
            existing.copy(
                label = tag.label,
                icon = tag.icon,
                color = tag.color,
                editedAt = now,
            )
        )
    }

    fun deleteTag(tag: TagUiModel) {
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
        val existing = tagDao.getById(tagId) ?: return

        if (existing.isHidden == isHidden) return

        tagDao.upsert(
            existing.copy(
                isHidden = isHidden,
                editedAt = clock.now().toEpochMilliseconds(),
            )
        )
    }

    private suspend fun deleteTagInternal(tag: TagUiModel) {
        removeTagFromAllBookmarksInternal(tag.id)
        tagDao.deleteById(tag.id)
    }

    private suspend fun removeTagFromAllBookmarksInternal(tagId: String) {
        val bookmarkIds = tagBookmarkDao.getBookmarkIdsForTag(tagId)
        bookmarkIds.forEach { bookmarkId ->
            val existing = bookmarkDao.getById(bookmarkId) ?: return@forEach
            val now = clock.now().toEpochMilliseconds()
            tagBookmarkDao.delete(bookmarkId, tagId)
            bookmarkDao.upsert(existing.copy(editedAt = now))
        }
    }

    suspend fun ensurePinnedTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(PINNED_TAG_ID)?.let {
            return it.toUiModel()
        }

        // Insert synchronously — do not use CoreOperationQueue.queueAndAwait here.
        // Callers such as [AllBookmarksManager.syncPinnedSystemTagForBookmark] already run on the
        // operation queue; nesting would deadlock the single worker and the tag would never exist.
        ensurePinnedTagInternal()

        return tagDao.getTagWithBookmarkCount(PINNED_TAG_ID)?.toUiModel()
            ?: createPinnedTagModel()
    }

    private suspend fun ensurePinnedTagInternal() {
        if (tagDao.getById(PINNED_TAG_ID) != null) return

        val now = clock.now().toEpochMilliseconds()
        val entity = TagEntity(
            id = PINNED_TAG_ID,
            label = CoreConstants.Tag.Pinned.NAME,
            icon = CoreConstants.Tag.Pinned.ICON,
            color = YabaColor.YELLOW,
            createdAt = now,
            editedAt = now,
            isHidden = false,
        )
        tagDao.upsert(entity)
    }

    private fun createPinnedTagModel(): TagUiModel {
        val now = clock.now()
        return TagUiModel(
            id = PINNED_TAG_ID,
            label = CoreConstants.Tag.Pinned.NAME,
            icon = CoreConstants.Tag.Pinned.ICON,
            color = YabaColor.YELLOW,
            createdAt = now,
            editedAt = now,
            bookmarkCount = 0,
        )
    }

    suspend fun ensurePrivateTag(): TagUiModel {
        tagDao.getTagWithBookmarkCount(PRIVATE_TAG_ID)?.let {
            return it.toUiModel()
        }

        // Same rationale as [ensurePinnedTag]: avoid nested queue work when callers may already
        // hold the CoreOperationQueue worker.
        ensurePrivateTagInternal()

        return tagDao.getTagWithBookmarkCount(PRIVATE_TAG_ID)?.toUiModel()
            ?: createPrivateTagModel()
    }

    private suspend fun ensurePrivateTagInternal() {
        if (tagDao.getById(PRIVATE_TAG_ID) != null) return

        val now = clock.now().toEpochMilliseconds()
        val entity = TagEntity(
            id = PRIVATE_TAG_ID,
            label = CoreConstants.Tag.Private.NAME,
            icon = CoreConstants.Tag.Private.ICON,
            color = YabaColor.RED,
            createdAt = now,
            editedAt = now,
            isHidden = false,
        )
        tagDao.upsert(entity)
    }

    private fun createPrivateTagModel(): TagUiModel {
        val now = clock.now()
        return TagUiModel(
            id = PRIVATE_TAG_ID,
            label = CoreConstants.Tag.Private.NAME,
            icon = CoreConstants.Tag.Private.ICON,
            color = YabaColor.RED,
            createdAt = now,
            editedAt = now,
            bookmarkCount = 0,
        )
    }
}
