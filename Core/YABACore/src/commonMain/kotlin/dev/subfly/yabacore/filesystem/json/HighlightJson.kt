package dev.subfly.yabacore.filesystem.json

import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.YabaColorSerializer
import kotlinx.serialization.Serializable

/**
 * JSON schema for highlight data stored at `/bookmarks/<uuid>/content/annotations/<highlightId>.json`.
 *
 * Highlights are mutable and CRDT-merged. They reference a specific content version
 * and anchor to positions within the readable document's block/inline structure.
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
    /** Start position anchor */
    val startAnchor: HighlightAnchor,
    /** End position anchor */
    val endAnchor: HighlightAnchor,
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

/**
 * Anchor position within a readable document.
 *
 * An anchor points to a specific character offset within a text node,
 * identified by block ID and the path through inline elements.
 */
@Serializable
data class HighlightAnchor(
    /** Block ID (e.g., "b0", "b5") */
    val blockId: String,
    /**
     * Path through inline elements to reach the text node.
     * Each integer is the child index at that level.
     * Empty list means direct text child of the block.
     */
    val inlinePath: List<Int>,
    /** Character offset within the target text node */
    val offset: Int,
)
