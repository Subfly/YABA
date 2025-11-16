package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.oplog.ReplicaCursorEntity

@Dao
interface ReplicaCursorDao {
    @Upsert
    suspend fun upsert(cursor: ReplicaCursorEntity)

    @Query("SELECT * FROM replica_cursors WHERE remoteDeviceId = :deviceId LIMIT 1")
    suspend fun get(deviceId: String): ReplicaCursorEntity?

    @Query("SELECT * FROM replica_cursors")
    suspend fun getAll(): List<ReplicaCursorEntity>
}

