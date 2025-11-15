package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.models.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<FolderEntity>)

    @Update
    suspend fun update(entity: FolderEntity)

    @Update
    suspend fun updateAll(entities: List<FolderEntity>)

    @Delete
    suspend fun delete(entity: FolderEntity)

    @Delete
    suspend fun deleteAll(entities: List<FolderEntity>)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY `order` ASC")
    fun getAllFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    fun getRootFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    fun getChildrenFlow(parentId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    suspend fun getChildrenList(parentId: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    suspend fun getRootList(): List<FolderEntity>

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM folders WHERE parentId IS NULL")
    suspend fun getMaxOrderForRoot(): Int?

    @Query("SELECT COALESCE(MAX(`order`), -1) FROM folders WHERE parentId = :parentId")
    suspend fun getMaxOrderForParent(parentId: String): Int?

    @Query(
        "SELECT f.id, f.label, f.iconName, f.color, f.parentId, f.`order`, f.version, " +
                "COUNT(b.id) AS bookmarkCount " +
                "FROM folders f " +
                "LEFT JOIN bookmarks b ON b.folderId = f.id " +
                "GROUP BY f.id " +
                "ORDER BY f.`order` ASC"
    )
    fun getFoldersWithBookmarkCountFlow(): Flow<List<FolderWithCount>>
}
