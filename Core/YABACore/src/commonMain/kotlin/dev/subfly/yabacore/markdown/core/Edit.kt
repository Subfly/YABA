package dev.subfly.yabacore.markdown.core

/**
 * Result of a rope edit (insert / delete / replace).
 * Used for incremental parsing to compute damage range.
 */
data class EditResult(
    /** Length of the rope before the edit */
    val oldLength: Int,
    /** Length of the rope after the edit */
    val newLength: Int,
    /** newLength - oldLength */
    val delta: Int,
    /** Character range in the new rope that was affected by the edit (half-open) */
    val affectedRange: Range,
)
