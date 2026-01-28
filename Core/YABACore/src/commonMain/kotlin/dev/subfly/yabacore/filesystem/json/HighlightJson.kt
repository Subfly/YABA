package dev.subfly.yabacore.filesystem.json

import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.YabaColorSerializer
import kotlinx.serialization.Serializable

/**
 * JSON schema for highlight data stored at `/bookmarks/<uuid>/content/annotations/<highlightId>.json`.
 *
 * Highlights are mutable and CRDT-merged. They reference a specific content version
 * and use character offsets into the markdown string for that version.
 *
 * This is the authoritative representation of a highlight in the filesystem.
 */
@Serializable
data class HighlightJson(
    /** Unique highlight identifier */
    val id: String,
    /** Parent bookmark ID */
    val bookmarkId: String,
    /** Content version this highlight is anchored to */
    val contentVersion: Int,
    /** Character offset (inclusive) into the markdown string for this content version */
    val startOffset: Int,
    /** Character offset (exclusive) into the markdown string for this content version */
    val endOffset: Int,
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
