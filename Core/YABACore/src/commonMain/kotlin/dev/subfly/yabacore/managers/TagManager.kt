package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.dao.BookmarkTagDao
import dev.subfly.yabacore.database.dao.TagDao
import dev.subfly.yabacore.database.dao.TombstoneDao
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.entities.TombstoneEntity
import dev.subfly.yabacore.database.models.TagWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class TagManager(
    private val tagDao: TagDao,
    private val bookmarkTagDao: BookmarkTagDao,
    private val tombstoneDao: TombstoneDao,
) {
    data class TagUpdate(
        val label: String? = null,
        val iconName: String? = null,
        val color: Int? = null,
    )

    fun getTagsFlow(): Flow<List<TagEntity>> = tagDao.getAllFlow()
    fun getTagsWithBookmarkCountFlow(): Flow<List<TagWithCount>> = tagDao.getTagsWithBookmarkCountFlow()

    suspend fun createTag(label: String, iconName: String, color: Int): TagEntity {
        val now = Clock.System.now()
        val entity = TagEntity(
            id = Uuid.random().toString(),
            label = label,
            iconName = iconName,
            color = color,
            createdAt = now,
            editedAt = now,
            version = 1,
        )
        tagDao.insert(entity)
        return entity
    }

    suspend fun updateTag(id: String, updates: TagUpdate) {
        val existing = tagDao.getById(id) ?: return
        val now = Clock.System.now()
        val updated = existing.copy(
            label = updates.label ?: existing.label,
            iconName = updates.iconName ?: existing.iconName,
            color = updates.color ?: existing.color,
            editedAt = now,
            version = existing.version + 1,
        )
        tagDao.update(updated)
    }

    suspend fun deleteTag(id: String, deviceId: String? = null) {
        val existing = tagDao.getById(id) ?: return
        val tombstone = TombstoneEntity(
            tombstoneId = Uuid.random().toString(),
            entityType = "tag",
            entityId = id,
            timestamp = Clock.System.now(),
            deviceId = deviceId,
        )
        tombstoneDao.insert(tombstone)
        bookmarkTagDao.deleteAllForTag(id)
        tagDao.delete(existing)
    }
}


