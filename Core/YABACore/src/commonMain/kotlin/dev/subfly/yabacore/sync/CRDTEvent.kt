package dev.subfly.yabacore.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Represents the type of entity that a CRDT event applies to.
 */
enum class ObjectType {
    BOOKMARK,
    FOLDER,
    TAG,
    HIGHLIGHT,
}

/**
 * Represents which JSON file in an entity's folder the event applies to.
 */
enum class FileTarget {
    /** The meta.json file containing base entity metadata */
    META_JSON,
    /** The link.json file containing link-specific bookmark data */
    LINK_JSON,
    /** The highlight annotation JSON file in /content/annotations/ */
    HIGHLIGHT_JSON,
}

/**
 * Represents the type of CRDT event.
 *
 * The CRDT ruleset:
 * - Exactly one CREATE event per entity lifecycle
 * - UPDATE events contain patch payloads (multi-field allowed)
 * - DELETE events dominate updates and are never compacted
 * - No resurrection; deleted entities stay deleted
 */
enum class EventType {
    /** Entity creation. Payload contains all initial fields. */
    CREATE,
    /** Entity update. Payload contains only changed fields (patch). */
    UPDATE,
    /** Entity deletion. Dominates all updates. Never compacted. */
    DELETE,
}

/**
 * A CRDT event representing a change to an entity.
 *
 * Events are:
 * - Append-only: once created, they are never modified
 * - Typed: CREATE, UPDATE, or DELETE
 * - Idempotent: applying the same event multiple times has the same effect
 * - Order-independent: events can be applied in any order and converge
 *
 * DELETE events have "delete dominance" - once an entity is deleted,
 * all subsequent updates are ignored. DELETE events are never compacted.
 *
 * Events are stored in `events.sqlite` and used for incremental sync.
 * The filesystem JSON files are the authoritative source of truth.
 *
 * @property eventId Unique identifier for this event (UUID)
 * @property objectId The entity UUID this event applies to
 * @property objectType The type of entity (BOOKMARK, FOLDER, TAG, HIGHLIGHT)
 * @property eventType The type of event (CREATE, UPDATE, DELETE)
 * @property file Which JSON file the event applies to (META_JSON, LINK_JSON)
 * @property payload The event payload as a JsonObject. For CREATE/UPDATE, contains field values.
 *                   For DELETE, may be empty or contain deletion metadata.
 * @property clock The vector clock at the time of this change
 * @property timestamp Unix timestamp in milliseconds when the event was created
 */
@Serializable
data class CRDTEvent(
    val eventId: String,
    val objectId: String,
    val objectType: ObjectType,
    val eventType: EventType,
    val file: FileTarget,
    val payload: JsonObject,
    val clock: VectorClock,
    val timestamp: Long,
) {
    /**
     * Returns true if this is a DELETE event.
     */
    fun isDelete(): Boolean = eventType == EventType.DELETE

    /**
     * Returns true if this is a CREATE event.
     */
    fun isCreate(): Boolean = eventType == EventType.CREATE

    /**
     * Returns true if this is an UPDATE event.
     */
    fun isUpdate(): Boolean = eventType == EventType.UPDATE
}

/**
 * Result of merging CRDT events for a single entity.
 *
 * Contains the resolved field values for each JSON file.
 */
data class MergedState(
    val objectId: String,
    val objectType: ObjectType,
    /** Whether the entity has been deleted */
    val isDeleted: Boolean,
    /** Merged field values for meta.json */
    val metaFields: Map<String, JsonElement>,
    /** Merged field values for link.json (only for BOOKMARK type) */
    val linkFields: Map<String, JsonElement>,
    /** Merged field values for highlight.json (only for HIGHLIGHT type) */
    val highlightFields: Map<String, JsonElement> = emptyMap(),
    /** The merged vector clock representing all applied events */
    val mergedClock: VectorClock,
)

