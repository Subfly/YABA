package dev.subfly.yabacore.markdown.core

/**
 * Rope implementation: balanced tree of string chunks.
 * Chunk size: MIN 256, MAX 2048. Each node caches length and newline count.
 */
internal sealed class RopeNode {
    abstract val length: Int
    abstract val newlineCount: Int

    abstract fun charAt(index: Int): Char
    abstract fun sliceToSequence(start: Int, end: Int): CharSequence
    abstract fun appendTo(sb: StringBuilder, start: Int, end: Int)
}

internal class LeafNode(internal val text: String) : RopeNode() {
    override val length: Int = text.length
    override val newlineCount: Int = text.count { it == '\n' }

    override fun charAt(index: Int): Char = text[index]
    override fun sliceToSequence(start: Int, end: Int): CharSequence = text.substring(start, end)
    override fun appendTo(sb: StringBuilder, start: Int, end: Int) {
        sb.append(text, start, end)
    }
}

internal class BranchNode(
    private val left: RopeNode,
    private val right: RopeNode,
) : RopeNode() {
    override val length: Int = left.length + right.length
    override val newlineCount: Int = left.newlineCount + right.newlineCount

    override fun charAt(index: Int): Char {
        return if (index < left.length) left.charAt(index)
        else right.charAt(index - left.length)
    }

    override fun sliceToSequence(start: Int, end: Int): CharSequence {
        val sb = StringBuilder(end - start)
        appendSlice(sb, start, end)
        return sb.toString()
    }

    override fun appendTo(sb: StringBuilder, start: Int, end: Int) {
        appendSlice(sb, start, end)
    }

    private fun appendSlice(sb: StringBuilder, start: Int, end: Int) {
        val leftLen = left.length
        when {
            end <= leftLen -> left.appendTo(sb, start, end)
            start >= leftLen -> right.appendTo(sb, start - leftLen, end - leftLen)
            else -> {
                left.appendTo(sb, start, leftLen)
                right.appendTo(sb, 0, end - leftLen)
            }
        }
    }
}

object RopeConstants {
    const val MAX_CHUNK = 2048
}

private fun RopeNode.rebalance(): RopeNode = when (this) {
    is LeafNode -> this
    is BranchNode -> {
        val total = length
        if (total <= RopeConstants.MAX_CHUNK) {
            LeafNode(buildString(total) { appendTo(this, 0, total) })
        } else {
            val mid = total / 2
            BranchNode(
                sliceToSequence(0, mid).toRopeNode().rebalance(),
                sliceToSequence(mid, total).toRopeNode().rebalance(),
            )
        }
    }
}

private fun CharSequence.toRopeNode(): RopeNode {
    return when (val len = length) {
        0 -> LeafNode("")
        in 1..RopeConstants.MAX_CHUNK -> LeafNode(toString())
        else -> {
            val mid = len / 2
            BranchNode(substring(0, mid).toRopeNode(), substring(mid, len).toRopeNode())
        }
    }
}

class RopeImpl private constructor(private var root: RopeNode) : Rope {

    override val length: Int get() = root.length

    override fun charAt(index: Int): Char {
        require(index in 0 until length) { "index $index out of range [0, $length)" }
        return root.charAt(index)
    }

    override fun slice(start: Int, end: Int): CharSequence {
        require(start in 0..length && end in start..length) { "slice [$start, $end) out of range" }
        if (start == end) return ""
        return root.sliceToSequence(start, end)
    }

    override fun insert(pos: Int, text: String): EditResult {
        require(pos in 0..length) { "insert position $pos out of range [0, $length]" }
        val oldLen = length
        val insertLen = text.length
        root = when {
            pos == 0 -> concat(LeafNode(text), root)
            pos == oldLen -> concat(root, LeafNode(text))
            else -> concat(
                root.sliceToSequence(0, pos).toRopeNode(),
                concat(LeafNode(text), root.sliceToSequence(pos, oldLen).toRopeNode()),
            )
        }
        return EditResult(
            oldLength = oldLen,
            newLength = length,
            delta = insertLen,
            affectedRange = Range(pos, pos + insertLen),
        )
    }

    override fun delete(start: Int, end: Int): EditResult {
        require(start in 0..length && end in start..length) { "delete [$start, $end) out of range" }
        val oldLen = length
        val deleteLen = end - start
        if (deleteLen == 0) return EditResult(oldLen, oldLen, 0, Range(start, start))
        root = when {
            start == 0 -> root.sliceToSequence(end, oldLen).toRopeNode()
            end == oldLen -> root.sliceToSequence(0, start).toRopeNode()
            else -> concat(root.sliceToSequence(0, start).toRopeNode(), root.sliceToSequence(end, oldLen).toRopeNode())
        }
        return EditResult(
            oldLength = oldLen,
            newLength = length,
            delta = -deleteLen,
            affectedRange = Range(start, start),
        )
    }

    override fun replace(start: Int, end: Int, text: String): EditResult {
        require(start in 0..length && end in start..length) { "replace [$start, $end) out of range" }
        val oldLen = length
        val deleteLen = end - start
        val insertLen = text.length
        root = when {
            start == 0 && end == oldLen -> LeafNode(text).takeIf { text.length <= RopeConstants.MAX_CHUNK }
                ?: text.toRopeNode()
            start == 0 -> concat(LeafNode(text), root.sliceToSequence(end, oldLen).toRopeNode())
            end == oldLen -> concat(root.sliceToSequence(0, start).toRopeNode(), LeafNode(text))
            else -> concat(
                root.sliceToSequence(0, start).toRopeNode(),
                concat(LeafNode(text), root.sliceToSequence(end, oldLen).toRopeNode()),
            )
        }
        return EditResult(
            oldLength = oldLen,
            newLength = length,
            delta = insertLen - deleteLen,
            affectedRange = Range(start, start + insertLen),
        )
    }

    override fun findLineStart(offset: Int): Int {
        require(offset in 0..length) { "offset $offset out of range" }
        var i = offset
        while (i > 0 && charAt(i - 1) != '\n') i--
        return i
    }

    override fun findLineEnd(offset: Int): Int {
        require(offset in 0..length) { "offset $offset out of range" }
        var i = offset
        while (i < length && charAt(i) != '\n') i++
        return i
    }

    override fun lineOf(offset: Int): Int {
        require(offset in 0..length) { "offset $offset out of range" }
        var line = 0
        var i = 0
        while (i < offset) {
            if (charAt(i) == '\n') line++
            i++
        }
        return line
    }

    private fun concat(a: RopeNode, b: RopeNode): RopeNode {
        if (a.length == 0) return b
        if (b.length == 0) return a
        return BranchNode(a, b)
    }
}
