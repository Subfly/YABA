package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.TagEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(entity: TagEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TagEntity>)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Uuid): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    fun observeById(id: Uuid): Flow<TagEntity?>

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN tag_bookmarks ON tags.id = tag_bookmarks.tagId
        WHERE tag_bookmarks.bookmarkId = :bookmarkId
        ORDER BY tags.`order` ASC
        """
    )
    fun observeTagsForBookmark(bookmarkId: Uuid): Flow<List<TagEntity>>
}
