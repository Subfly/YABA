package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.FolderEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface FolderDao {
    @Upsert
    suspend fun upsert(entity: FolderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FolderEntity>)

    @Delete
    suspend fun delete(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Uuid): FolderEntity?

    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun observeById(id: Uuid): Flow<FolderEntity?>

    @Query("SELECT * FROM folders ORDER BY `order` ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    fun observeRoot(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    fun observeChildren(parentId: Uuid): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    suspend fun getChildren(parentId: Uuid): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    suspend fun getRoot(): List<FolderEntity>
}
