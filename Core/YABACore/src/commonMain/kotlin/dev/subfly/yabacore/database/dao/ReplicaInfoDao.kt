package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.oplog.ReplicaInfoEntity

@Dao
interface ReplicaInfoDao {
    @Upsert
    suspend fun upsert(info: ReplicaInfoEntity)

    @Query("SELECT * FROM replica_info LIMIT 1")
    suspend fun get(): ReplicaInfoEntity?
}

