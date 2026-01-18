package dev.subfly.yabacore.sync

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEntities
import dev.subfly.yabacore.database.events.toEntity
import dev.subfly.yabacore.database.events.toEvents
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Clock

/**
 * CRDT Engine for handling field-level merge operations.
 *
 * This engine:
 * - Generates CRDT events for local changes
 * - Stores events in events.sqlite
 * - Merges events for objects to produce final state
 * - Resolves conflicts using vector clock comparison
 *
 * Field resolution rules:
 * 1. Compare vector clocks
 * 2. Higher clock wins
 * 3. Concurrent edits: deterministic tie-breaker (lexicographic deviceId comparison)
 */
object CRDTEngine {
    private val eventsDao get() = EventsDatabaseProvider.eventsDao
    private val clock = Clock.System

    /**
     * Generates and stores a CRDT event for a field change.
     */
    suspend fun recordFieldChange(
        objectId: String,
        objectType: ObjectType,
        file: FileTarget,
        field: String,
        value: JsonElement,
        currentClock: VectorClock,
    ): CRDTEvent {
        val deviceId = DeviceIdProvider.get()
        val newClock = currentClock.increment(deviceId)
        val event = CRDTEvent(
            eventId = IdGenerator.newId(),
            objectId = objectId,
            objectType = objectType,
            file = file,
            field = field,
            value = value,
            clock = newClock,
            timestamp = clock.now().toEpochMilliseconds(),
        )
        eventsDao.insertEvent(event.toEntity())
        return event
    }

    /**
     * Records multiple field changes as separate events.
     */
    suspend fun recordFieldChanges(
        objectId: String,
        objectType: ObjectType,
        file: FileTarget,
        changes: Map<String, JsonElement>,
        currentClock: VectorClock,
    ): List<CRDTEvent> {
        if (changes.isEmpty()) return emptyList()
        val deviceId = DeviceIdProvider.get()
        var workingClock = currentClock
        val events = changes.map { (field, value) ->
            workingClock = workingClock.increment(deviceId)
            CRDTEvent(
                eventId = IdGenerator.newId(),
                objectId = objectId,
                objectType = objectType,
                file = file,
                field = field,
                value = value,
                clock = workingClock,
                timestamp = clock.now().toEpochMilliseconds(),
            )
        }
        eventsDao.insertEvents(events.toEntities())
        return events
    }

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

    /**
     * Merges all events for an object and returns the resolved state.
     */
    suspend fun mergeEventsForObject(objectId: String): MergedState? {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return null

        val objectType = events.first().objectType
        val metaFields = mutableMapOf<String, JsonElement>()
        val linkFields = mutableMapOf<String, JsonElement>()
        var mergedClock = VectorClock.empty()

        // Group events by file target and field
        val metaEvents = events.filter { it.file == FileTarget.META_JSON }
        val linkEvents = events.filter { it.file == FileTarget.LINK_JSON }

        // Resolve each field
        for ((field, fieldEvents) in metaEvents.groupBy { it.field }) {
            val resolved = resolveFieldFromEvents(fieldEvents)
            metaFields[field] = resolved.value
            mergedClock = mergedClock.merge(resolved.winningClock)
        }

        for ((field, fieldEvents) in linkEvents.groupBy { it.field }) {
            val resolved = resolveFieldFromEvents(fieldEvents)
            linkFields[field] = resolved.value
            mergedClock = mergedClock.merge(resolved.winningClock)
        }

        return MergedState(
            objectId = objectId,
            objectType = objectType,
            metaFields = metaFields,
            linkFields = linkFields,
            mergedClock = mergedClock,
        )
    }

    /**
     * Resolves a single field from multiple events.
     */
    fun resolveFieldFromEvents(events: List<CRDTEvent>): ResolvedValue {
        require(events.isNotEmpty()) { "Cannot resolve empty event list" }
        require(events.map { it.field }.distinct().size == 1) { "All events must be for the same field" }

        val field = events.first().field
        var winner = events.first()

        for (event in events.drop(1)) {
            winner = pickWinner(winner, event)
        }

        return ResolvedValue(
            field = field,
            value = winner.value,
            winningClock = winner.clock,
        )
    }

    /**
     * Picks the winning event between two events for the same field.
     */
    private fun pickWinner(a: CRDTEvent, b: CRDTEvent): CRDTEvent {
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
     * Deletes all events for an object (used during compaction).
     */
    suspend fun deleteEventsForObject(objectId: String) {
        eventsDao.deleteEventsForObject(objectId)
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
