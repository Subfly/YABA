package dev.subfly.yabacore.database.events

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a CRDT event stored in events.sqlite.
 *
 * Each event represents a field-level change to an entity. Events are
 * append-only and used for incremental sync between devices.
 */
@Entity(
    tableName = "crdt_events",
    indices = [
        Index(value = ["objectId"]),
        Index(value = ["objectType"]),
        Index(value = ["timestamp"]),
    ],
)
data class CRDTEventEntity(
    @PrimaryKey val eventId: String,
    val objectId: String,
    val objectType: String, // ObjectType.name
    val file: String, // FileTarget.name
    val field: String,
    /** JSON string representation of the value */
    val valueJson: String,
    /** JSON string representation of the vector clock */
    val clockJson: String,
    val timestamp: Long,
)
