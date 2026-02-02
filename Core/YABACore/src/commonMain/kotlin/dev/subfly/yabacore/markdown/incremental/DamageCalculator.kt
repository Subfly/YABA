package dev.subfly.yabacore.markdown.incremental

import dev.subfly.yabacore.markdown.core.EditResult
import dev.subfly.yabacore.markdown.core.Range
import dev.subfly.yabacore.markdown.core.Rope

/**
 * Computes the damage range for incremental reparse after an edit.
 * Expands to line boundaries and over-expands around code fences, lists, blockquotes, tables.
 */
object DamageCalculator {

    /**
     * Given the rope and the [editResult] from the last edit, returns the damage range [start, end)
     * that should be reparsed. Over-expand if unsure.
     */
    fun computeDamage(rope: Rope, editResult: EditResult): Range {
        val start = editResult.affectedRange.start
        val end = editResult.affectedRange.end
        val lineStart = rope.findLineStart(start)
        var lineEnd = rope.findLineEnd(end)
        if (lineEnd < rope.length && rope.charAt(lineEnd) == '\n') lineEnd++
        var damageStart = lineStart
        var damageEnd = lineEnd
        val text = rope.slice(0, rope.length).toString()
        damageStart = expandBackward(text, damageStart)
        damageEnd = expandForward(text, damageEnd)
        return Range(damageStart, damageEnd.coerceAtMost(rope.length))
    }

    private fun expandBackward(text: String, from: Int): Int {
        var i = from
        if (i <= 0) return 0
        var inFence = false
        while (i > 0) {
            i--
            val lineStart = text.lastIndexOf('\n', i - 1).coerceAtLeast(-1) + 1
            val line = if (lineStart < i) text.substring(lineStart, minOf(i + 1, text.length)) else ""
            when {
                line.trimStart().startsWith("```") -> {
                    if (inFence) {
                        if (line.trim().startsWith("```")) i = lineStart
                        break
                    }
                    inFence = true
                    i = lineStart
                }
                line.trimStart().startsWith(">") -> i = lineStart
                line.trim().matches(Regex("^[-*+]\\s+.*")) -> i = lineStart
                line.trim().matches(Regex("^\\d+\\.\\s+.*")) -> i = lineStart
                line.trim().matches(Regex("^\\|.+\\|$")) -> i = lineStart
                line.isBlank() -> {
                    i = lineStart
                    break
                }
                else -> {
                    i = lineStart
                    break
                }
            }
        }
        return i.coerceAtLeast(0)
    }

    private fun expandForward(text: String, from: Int): Int {
        var i = from
        if (i >= text.length) return text.length
        var inFence = false
        while (i < text.length) {
            val lineEnd = text.indexOf('\n', i).let { if (it == -1) text.length else it + 1 }
            val line = text.substring(i, lineEnd)
            when {
                line.trimStart().startsWith("```") -> {
                    if (inFence) {
                        i = lineEnd
                        break
                    }
                    inFence = true
                    i = lineEnd
                }
                line.trimStart().startsWith(">") -> i = lineEnd
                line.trim().matches(Regex("^[-*+]\\s+.*")) -> i = lineEnd
                line.trim().matches(Regex("^\\d+\\.\\s+.*")) -> i = lineEnd
                line.trim().matches(Regex("^\\|.+\\|$")) -> i = lineEnd
                line.isBlank() -> {
                    i = lineEnd
                    break
                }
                else -> {
                    i = lineEnd
                    break
                }
            }
        }
        return i.coerceAtMost(text.length)
    }
}
