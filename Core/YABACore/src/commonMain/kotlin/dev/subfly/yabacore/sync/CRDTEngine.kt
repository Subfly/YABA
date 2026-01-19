package dev.subfly.yabacore.sync

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEntities
import dev.subfly.yabacore.database.events.toEntity
import dev.subfly.yabacore.database.events.toEvents
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * CRDT Engine for handling typed event operations.
 *
 * This engine:
 * - Generates CRDT events for local changes (CREATE, UPDATE, DELETE)
 * - Stores events in events.sqlite
 * - Merges events for objects to produce final state
 * - Resolves conflicts using vector clock comparison with delete dominance
 *
 * Event resolution rules:
 * 1. DELETE events dominate all other events (delete dominance)
 * 2. For CREATE/UPDATE: compare vector clocks, higher clock wins
 * 3. Concurrent edits: deterministic tie-breaker (lexicographic deviceId comparison)
 * 4. DELETE events are never compacted
 */
object CRDTEngine {
    private val eventsDao get() = EventsDatabaseProvider.eventsDao
    private val clock = Clock.System

    // ==================== Event Recording ====================

    /**
     * Records a CREATE event for a new entity.
     * There should be exactly one CREATE event per entity lifecycle.
     *
     * @param objectId The entity UUID
     * @param objectType The type of entity
     * @param file Which JSON file the payload applies to
     * @param payload All initial field values as a JsonObject
     * @param currentClock The current vector clock (typically empty for new entities)
     */
    suspend fun recordCreate(
        objectId: String,
        objectType: ObjectType,
        file: FileTarget,
        payload: Map<String, JsonElement>,
        currentClock: VectorClock = VectorClock.empty(),
    ): CRDTEvent {
        val deviceId = DeviceIdProvider.get()
        val newClock = currentClock.increment(deviceId)
        val event = CRDTEvent(
            eventId = IdGenerator.newId(),
            objectId = objectId,
            objectType = objectType,
            eventType = EventType.CREATE,
            file = file,
            payload = JsonObject(payload),
            clock = newClock,
            timestamp = clock.now().toEpochMilliseconds(),
        )
        eventsDao.insertEvent(event.toEntity())
        return event
    }

    /**
     * Records an UPDATE event for an existing entity.
     * The payload contains only the changed fields (patch semantics).
     *
     * @param objectId The entity UUID
     * @param objectType The type of entity
     * @param file Which JSON file the payload applies to
     * @param changes Only the changed field values as a Map
     * @param currentClock The current vector clock of the entity
     */
    suspend fun recordUpdate(
        objectId: String,
        objectType: ObjectType,
        file: FileTarget,
        changes: Map<String, JsonElement>,
        currentClock: VectorClock,
    ): CRDTEvent? {
        if (changes.isEmpty()) return null
        val deviceId = DeviceIdProvider.get()
        val newClock = currentClock.increment(deviceId)
        val event = CRDTEvent(
            eventId = IdGenerator.newId(),
            objectId = objectId,
            objectType = objectType,
            eventType = EventType.UPDATE,
            file = file,
            payload = JsonObject(changes),
            clock = newClock,
            timestamp = clock.now().toEpochMilliseconds(),
        )
        eventsDao.insertEvent(event.toEntity())
        return event
    }

    /**
     * Records a DELETE event for an entity.
     * DELETE events dominate all updates and are never compacted.
     *
     * @param objectId The entity UUID
     * @param objectType The type of entity
     * @param currentClock The current vector clock of the entity
     */
    suspend fun recordDelete(
        objectId: String,
        objectType: ObjectType,
        currentClock: VectorClock,
    ): CRDTEvent {
        val deviceId = DeviceIdProvider.get()
        val newClock = currentClock.increment(deviceId)
        val event = CRDTEvent(
            eventId = IdGenerator.newId(),
            objectId = objectId,
            objectType = objectType,
            eventType = EventType.DELETE,
            file = FileTarget.META_JSON, // DELETE applies to entire entity
            payload = buildJsonObject { }, // Empty payload for delete
            clock = newClock,
            timestamp = clock.now().toEpochMilliseconds(),
        )
        eventsDao.insertEvent(event.toEntity())
        return event
    }

    // ==================== Event Application ====================

    /**
     * Applies a single event from a remote source.
     */
    suspend fun applyEvent(event: CRDTEvent) {
        eventsDao.insertEvent(event.toEntity())
    }

    /**
     * Applies multiple events from a remote source.
     */
    suspend fun applyEvents(events: List<CRDTEvent>) {
        if (events.isEmpty()) return
        eventsDao.insertEvents(events.toEntities())
    }

    // ==================== Event Merging ====================

    /**
     * Merges all events for an object and returns the resolved state.
     * Applies delete dominance: if any DELETE event wins, the entity is deleted.
     */
    suspend fun mergeEventsForObject(objectId: String): MergedState? {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return null

        val objectType = events.first().objectType

        // Check for delete dominance first
        val deleteEvents = events.filter { it.isDelete() }
        if (deleteEvents.isNotEmpty()) {
            // Find the winning delete event
            val winningDelete = deleteEvents.reduce { a, b -> pickWinner(a, b) }

            // Check if delete dominates all other events
            val allNonDeleteEvents = events.filterNot { it.isDelete() }
            val deleteWins = allNonDeleteEvents.all { other ->
                winningDelete.clock.isNewerOrEqual(other.clock) ||
                    winningDelete.clock.isNewerThan(other.clock) ||
                    !other.clock.isNewerThan(winningDelete.clock)
            }

            if (deleteWins || allNonDeleteEvents.isEmpty()) {
                return MergedState(
                    objectId = objectId,
                    objectType = objectType,
                    isDeleted = true,
                    metaFields = emptyMap(),
                    linkFields = emptyMap(),
                    mergedClock = winningDelete.clock,
                )
            }
        }

        // No winning delete - merge CREATE/UPDATE events
        val metaFields = mutableMapOf<String, JsonElement>()
        val linkFields = mutableMapOf<String, JsonElement>()
        var mergedClock = VectorClock.empty()

        // Group events by file target
        val metaEvents = events.filter { it.file == FileTarget.META_JSON && !it.isDelete() }
        val linkEvents = events.filter { it.file == FileTarget.LINK_JSON && !it.isDelete() }

        // Merge meta events
        mergeEventPayloads(metaEvents, metaFields)
        metaEvents.forEach { mergedClock = mergedClock.merge(it.clock) }

        // Merge link events
        mergeEventPayloads(linkEvents, linkFields)
        linkEvents.forEach { mergedClock = mergedClock.merge(it.clock) }

        return MergedState(
            objectId = objectId,
            objectType = objectType,
            isDeleted = false,
            metaFields = metaFields,
            linkFields = linkFields,
            mergedClock = mergedClock,
        )
    }

    /**
     * Merges event payloads into a field map, resolving conflicts per-field.
     */
    private fun mergeEventPayloads(
        events: List<CRDTEvent>,
        target: MutableMap<String, JsonElement>,
    ) {
        // Collect all fields and their events
        val fieldEvents = mutableMapOf<String, MutableList<Pair<CRDTEvent, JsonElement>>>()

        for (event in events) {
            for ((field, value) in event.payload) {
                fieldEvents.getOrPut(field) { mutableListOf() }.add(event to value)
            }
        }

        // Resolve each field
        for ((field, eventValuePairs) in fieldEvents) {
            // Find the winning event for this field
            var winner = eventValuePairs.first()
            for (pair in eventValuePairs.drop(1)) {
                if (pickWinner(pair.first, winner.first) == pair.first) {
                    winner = pair
                }
            }
            target[field] = winner.second
        }
    }

    /**
     * Picks the winning event between two events.
     * DELETE events always win against non-DELETE events.
     */
    private fun pickWinner(a: CRDTEvent, b: CRDTEvent): CRDTEvent {
        // Delete dominance
        if (a.isDelete() && !b.isDelete()) return a
        if (b.isDelete() && !a.isDelete()) return b

        return when {
            a.clock.isNewerThan(b.clock) -> a
            b.clock.isNewerThan(a.clock) -> b
            else -> {
                // Concurrent: use deterministic tie-breaker
                val comparison = VectorClock.deterministicCompare(a.clock, b.clock)
                if (comparison >= 0) a else b
            }
        }
    }

    // ==================== Event Queries ====================

    /**
     * Gets all pending events for sync.
     */
    suspend fun getPendingEvents(): List<CRDTEvent> {
        return eventsDao.getAllEvents().toEvents()
    }

    /**
     * Gets events after a specific timestamp.
     */
    suspend fun getEventsAfterTimestamp(afterTimestamp: Long): List<CRDTEvent> {
        return eventsDao.getEventsAfterTimestamp(afterTimestamp).toEvents()
    }

    /**
     * Deletes non-DELETE events for an object (used during compaction).
     * DELETE events are never deleted.
     */
    suspend fun deleteNonDeleteEventsForObject(objectId: String) {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        val nonDeleteEventIds = events.filterNot { it.isDelete() }.map { it.eventId }
        if (nonDeleteEventIds.isNotEmpty()) {
            eventsDao.deleteEventsByIds(nonDeleteEventIds)
        }
    }

    /**
     * Deletes DELETE events for an object.
     * Used for system entities where legacy DELETE events must not dominate visibility updates.
     */
    suspend fun deleteDeleteEventsForObject(objectId: String) {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        val deleteEventIds = events.filter { it.isDelete() }.map { it.eventId }
        if (deleteEventIds.isNotEmpty()) {
            eventsDao.deleteEventsByIds(deleteEventIds)
        }
    }

    /**
     * Gets the count of stored events.
     */
    suspend fun getEventCount(): Long {
        return eventsDao.getEventCount()
    }

    /**
     * Gets all object IDs that have pending events.
     */
    suspend fun getObjectIdsWithEvents(): List<String> {
        return eventsDao.getObjectIdsWithEvents()
    }

    /**
     * Checks if an object has a DELETE event.
     */
    suspend fun hasDeleteEvent(objectId: String): Boolean {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        return events.any { it.isDelete() }
    }

    // ==================== Helper Functions for JSON Values ====================

    fun stringValue(value: String): JsonElement = JsonPrimitive(value)
    fun longValue(value: Long): JsonElement = JsonPrimitive(value)
    fun intValue(value: Int): JsonElement = JsonPrimitive(value)
    fun booleanValue(value: Boolean): JsonElement = JsonPrimitive(value)
    fun nullValue(): JsonElement = JsonNull
    fun nullableStringValue(value: String?): JsonElement = value?.let { JsonPrimitive(it) } ?: JsonNull

    fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.content
    fun JsonElement.asLong(): Long? = (this as? JsonPrimitive)?.longOrNull
    fun JsonElement.asInt(): Int? = (this as? JsonPrimitive)?.longOrNull?.toInt()
    fun JsonElement.asBoolean(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull
}
