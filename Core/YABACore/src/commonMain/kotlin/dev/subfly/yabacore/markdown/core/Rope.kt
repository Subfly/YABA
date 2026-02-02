package dev.subfly.yabacore.markdown.core

/**
 * Rope text storage for efficient mid-text edits and cheap slicing.
 * Required for scalable incremental parsing.
 *
 * Implementation: balanced tree of string chunks (MIN 256, MAX 2048 chars per node),
 * each node caches character length and newline count.
 */
interface Rope {
    val length: Int

    fun charAt(index: Int): Char
    fun slice(start: Int, end: Int): CharSequence

    fun insert(pos: Int, text: String): EditResult
    fun delete(start: Int, end: Int): EditResult
    fun replace(start: Int, end: Int, text: String): EditResult

    fun findLineStart(offset: Int): Int
    fun findLineEnd(offset: Int): Int
    fun lineOf(offset: Int): Int
}
