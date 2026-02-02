package dev.subfly.yabacore.markdown.core

/**
 * Half-open character range [start, end) into a rope or string.
 * All offsets are absolute document positions.
 */
data class Range(
    val start: Int,
    val end: Int,
) {
    init {
        require(start in 0..end) { "Invalid range: start=$start, end=$end" }
    }

    val length: Int get() = end - start

    fun contains(offset: Int): Boolean = offset in start until end
    fun contains(other: Range): Boolean = other.start >= start && other.end <= end
    fun overlaps(other: Range): Boolean = start < other.end && other.start < end
}
