package dev.subfly.yabacore.database.entities.oplog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replica_info")
data class ReplicaInfoEntity(
    @PrimaryKey val singletonId: Int = 0,
    val deviceId: String,
    val nextOriginSeq: Long,
)

