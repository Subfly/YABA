package dev.subfly.yabacore.filesystem.json

import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.YabaColorSerializer
import kotlinx.serialization.Serializable

/**
 * JSON schema for highlight data stored at `/bookmarks/<uuid>/content/annotations/<highlightId>.json`.
 *
 * Highlights are mutable and CRDT-merged. They reference a specific content version
 * and use section-anchored offsets (deterministic sectionKey + offset within section).
 */
@Serializable
data class HighlightJson(
    /** Unique highlight identifier */
    val id: String,
    /** Parent bookmark ID */
    val bookmarkId: String,
    /** Content version this highlight is anchored to */
    val contentVersion: Int,
    /** Section key where the highlight starts (e.g. "b:0") */
    val startSectionKey: String,
    /** Character offset within the start section (inclusive) */
    val startOffsetInSection: Int,
    /** Section key where the highlight ends (e.g. "b:0" for same block) */
    val endSectionKey: String,
    /** Character offset within the end section (exclusive) */
    val endOffsetInSection: Int,
    /** Semantic color role for the highlight */
    @Serializable(with = YabaColorSerializer::class)
    val colorRole: YabaColor,
    /** Optional user note attached to this highlight */
    val note: String? = null,
    /** Creation timestamp in epoch milliseconds */
    val createdAt: Long,
    /** Last edit timestamp in epoch milliseconds */
    val editedAt: Long,
    /** Vector clock: Map of deviceId to sequence number */
    val clock: Map<String, Long>,
)
