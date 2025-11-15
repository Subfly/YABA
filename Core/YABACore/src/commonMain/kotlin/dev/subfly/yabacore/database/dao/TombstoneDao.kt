package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.subfly.yabacore.database.entities.TombstoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TombstoneDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TombstoneEntity>)

    @Delete
    suspend fun delete(entity: TombstoneEntity)

    @Query("DELETE FROM tombstones WHERE tombstoneId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT * FROM tombstones ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TombstoneEntity>>

    @Query("SELECT * FROM tombstones WHERE entityType = :entityType ORDER BY timestamp DESC")
    fun getByTypeFlow(entityType: String): Flow<List<TombstoneEntity>>

    @Query("DELETE FROM tombstones WHERE timestamp < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
