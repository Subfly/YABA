package dev.subfly.yabacore.database.events

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a CRDT event stored in events.sqlite.
 *
 * Events are typed (CREATE, UPDATE, DELETE) and use patch payloads.
 * - CREATE: Single event per entity lifecycle, contains all initial fields
 * - UPDATE: Contains only changed fields (patch)
 * - DELETE: Dominates all updates, never compacted
 *
 * Events are append-only and used for incremental sync between devices.
 */
@Entity(
    tableName = "crdt_events",
    indices = [
        Index(value = ["objectId"]),
        Index(value = ["objectType"]),
        Index(value = ["eventType"]),
        Index(value = ["timestamp"]),
    ],
)
data class CRDTEventEntity(
    @PrimaryKey val eventId: String,
    val objectId: String,
    val objectType: String, // ObjectType.name
    val eventType: String, // EventType.name (CREATE, UPDATE, DELETE)
    val file: String, // FileTarget.name
    /** JSON string representation of the payload (JsonObject) */
    val payloadJson: String,
    /** JSON string representation of the vector clock */
    val clockJson: String,
    val timestamp: Long,
)
