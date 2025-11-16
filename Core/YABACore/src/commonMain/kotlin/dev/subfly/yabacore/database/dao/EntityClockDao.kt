package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.oplog.EntityClockEntity

@Dao
interface EntityClockDao {
    @Upsert
    suspend fun upsert(clock: EntityClockEntity)

    @Upsert
    suspend fun upsertAll(clocks: List<EntityClockEntity>)

    @Query("SELECT * FROM entity_clock WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun get(entityType: String, entityId: String): EntityClockEntity?

    @Query("DELETE FROM entity_clock")
    suspend fun clear()
}

