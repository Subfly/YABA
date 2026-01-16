package dev.subfly.yabacore.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents the type of entity that a CRDT event applies to.
 */
enum class ObjectType {
    BOOKMARK,
    FOLDER,
    TAG,
}

/**
 * Represents which JSON file in an entity's folder the event applies to.
 */
enum class FileTarget {
    /** The meta.json file containing base entity metadata */
    META_JSON,
    /** The link.json file containing link-specific bookmark data */
    LINK_JSON,
}

/**
 * A CRDT event representing a field-level change to an entity.
 *
 * Events are:
 * - Append-only: once created, they are never modified
 * - Field-level granularity: each event changes a single field
 * - Idempotent: applying the same event multiple times has the same effect
 * - Order-independent: events can be applied in any order and converge
 *
 * Events are stored in `events.sqlite` and used for incremental sync.
 * The filesystem JSON files are the authoritative source of truth.
 *
 * @property eventId Unique identifier for this event (UUID)
 * @property objectId The entity UUID this event applies to
 * @property objectType The type of entity (BOOKMARK, FOLDER, TAG)
 * @property file Which JSON file the field belongs to (META_JSON, LINK_JSON)
 * @property field The name of the field being changed (e.g., "label", "url")
 * @property value The new value for the field as a JsonElement
 * @property clock The vector clock at the time of this change
 * @property timestamp Unix timestamp in milliseconds when the event was created
 */
@Serializable
data class CRDTEvent(
    val eventId: String,
    val objectId: String,
    val objectType: ObjectType,
    val file: FileTarget,
    val field: String,
    val value: JsonElement,
    val clock: VectorClock,
    val timestamp: Long,
)

/**
 * Result of merging CRDT events for a single entity.
 *
 * Contains the resolved field values for each JSON file.
 */
data class MergedState(
    val objectId: String,
    val objectType: ObjectType,
    /** Merged field values for meta.json */
    val metaFields: Map<String, JsonElement>,
    /** Merged field values for link.json (only for BOOKMARK type) */
    val linkFields: Map<String, JsonElement>,
    /** The merged vector clock representing all applied events */
    val mergedClock: VectorClock,
)

/**
 * Result of resolving a single field from multiple events.
 */
data class ResolvedValue(
    val field: String,
    val value: JsonElement,
    val winningClock: VectorClock,
)
