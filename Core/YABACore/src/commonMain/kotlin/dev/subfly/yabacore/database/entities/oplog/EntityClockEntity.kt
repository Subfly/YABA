package dev.subfly.yabacore.database.entities.oplog

import androidx.room.Entity

@Entity(
    tableName = "entity_clock",
    primaryKeys = ["entityType", "entityId"],
)
data class EntityClockEntity(
    val entityType: String,
    val entityId: String,
    val lastDeviceId: String,
    val lastSeq: Long,
)

