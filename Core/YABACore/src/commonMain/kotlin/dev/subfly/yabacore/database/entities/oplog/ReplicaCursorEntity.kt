package dev.subfly.yabacore.database.entities.oplog

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replica_cursors")
data class ReplicaCursorEntity(
    @PrimaryKey val remoteDeviceId: String,
    val lastSeqSeen: Long,
)

