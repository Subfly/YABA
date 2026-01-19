package dev.subfly.yabacore.database.events

import dev.subfly.yabacore.sync.CRDTEvent
import dev.subfly.yabacore.sync.EventType
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Converts a CRDTEvent to a CRDTEventEntity for database storage.
 */
fun CRDTEvent.toEntity(): CRDTEventEntity = CRDTEventEntity(
    eventId = eventId,
    objectId = objectId,
    objectType = objectType.name,
    eventType = eventType.name,
    file = file.name,
    payloadJson = json.encodeToString(payload),
    clockJson = json.encodeToString(clock.clocks),
    timestamp = timestamp,
)

/**
 * Converts a CRDTEventEntity back to a CRDTEvent.
 */
fun CRDTEventEntity.toEvent(): CRDTEvent = CRDTEvent(
    eventId = eventId,
    objectId = objectId,
    objectType = ObjectType.valueOf(objectType),
    eventType = EventType.valueOf(eventType),
    file = FileTarget.valueOf(file),
    payload = json.decodeFromString<JsonObject>(payloadJson),
    clock = VectorClock(json.decodeFromString<Map<String, Long>>(clockJson)),
    timestamp = timestamp,
)

/**
 * Converts a list of CRDTEventEntity to a list of CRDTEvent.
 */
fun List<CRDTEventEntity>.toEvents(): List<CRDTEvent> = map { it.toEvent() }

/**
 * Converts a list of CRDTEvent to a list of CRDTEventEntity.
 */
fun List<CRDTEvent>.toEntities(): List<CRDTEventEntity> = map { it.toEntity() }
