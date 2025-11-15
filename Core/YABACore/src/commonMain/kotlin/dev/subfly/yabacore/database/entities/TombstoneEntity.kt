package dev.subfly.yabacore.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "tombstones")
data class TombstoneEntity(
    @PrimaryKey val tombstoneId: String,
    val entityType: String, // "bookmark", "folder", or "tag"
    val entityId: String,
    val timestamp: Instant,
    val deviceId: String?,
)
