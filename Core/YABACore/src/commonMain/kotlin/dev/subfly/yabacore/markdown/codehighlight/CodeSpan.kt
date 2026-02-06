package dev.subfly.yabacore.markdown.codehighlight

/**
 * A contiguous span of code with a single token type. [start] and [end] are character offsets (half-open).
 */
data class CodeSpan(
    val type: CodeTokenType,
    val start: Int,
    val end: Int,
)
