package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.TagWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TagEntity>)

    @Update
    suspend fun update(entity: TagEntity)

    @Update
    suspend fun updateAll(entities: List<TagEntity>)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Delete
    suspend fun deleteAll(entities: List<TagEntity>)

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY editedAt DESC")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query(
        "SELECT t.id, t.label, t.iconName, t.color, t.version, " +
                "COUNT(r.bookmarkId) AS bookmarkCount " +
                "FROM tags t " +
                "LEFT JOIN bookmark_tag_cross_ref r ON r.tagId = t.id " +
                "GROUP BY t.id " +
                "ORDER BY t.editedAt DESC"
    )
    fun getTagsWithBookmarkCountFlow(): Flow<List<TagWithCount>>
}
