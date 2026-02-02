package dev.subfly.yabacore.markdown.incremental

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.core.Range
import dev.subfly.yabacore.markdown.parser.BlockParser

/**
 * Reparses only the damaged slice and splices the result into the document.
 * Removes old blocks intersecting the damage range, inserts new blocks, preserves block IDs when structure matches.
 */
object PartialReparser {

    /**
     * Full reparse: parse the entire rope and build a fresh document.
     */
    fun fullParse(source: String): BlockNode.Document {
        val blocks = BlockParser.parse(source)
        val range = if (source.isEmpty()) Range(0, 0) else Range(0, source.length)
        val id = IdGenerator.newId()
        return BlockNode.Document(id, range, blocks)
    }

    /**
     * Parse a fragment of the document [source] in range [damageRange].
     * Returns the list of blocks that would be parsed from that slice (with absolute ranges).
     */
    fun parseFragment(source: String, damageRange: Range): List<BlockNode> {
        val slice = source.substring(damageRange.start, damageRange.end)
        val blocks = BlockParser.parse(slice)
        return blocks.map { block ->
            shiftBlockRange(block, damageRange.start)
        }
    }

    private fun shiftBlockRange(block: BlockNode, delta: Int): BlockNode = when (block) {
        is BlockNode.Document -> block
        is BlockNode.Heading -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
        )

        is BlockNode.Paragraph -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
        )

        is BlockNode.BlockQuote -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
            children = block.children.map { shiftBlockRange(it, delta) },
        )

        is BlockNode.ListBlock -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
            children = block.children.map { shiftBlockRange(it, delta) },
        )

        is BlockNode.ListItem -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
            children = block.children.map { shiftBlockRange(it, delta) },
        )

        is BlockNode.CodeFence -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
        )

        is BlockNode.HorizontalRule -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
        )

        is BlockNode.TableBlock -> block.copy(
            range = Range(block.range.start + delta, block.range.end + delta),
        )
    }

    /**
     * Splice: remove blocks from [currentBlocks] that intersect [damageRange], insert [newBlocks] at the insertion index.
     * Returns the new block list and a [DocumentPatch] describing the change.
     */
    fun splice(
        currentBlocks: List<BlockNode>,
        damageRange: Range,
        newBlocks: List<BlockNode>,
    ): Pair<List<BlockNode>, DocumentPatch> {
        val removed = mutableListOf<String>()
        val before = mutableListOf<BlockNode>()
        var i = 0
        while (i < currentBlocks.size) {
            val block = currentBlocks[i]
            val blockRange = block.range
            if (blockRange.end <= damageRange.start) {
                before.add(block)
                i++
                continue
            }
            if (blockRange.start >= damageRange.end) {
                break
            }
            removed.add(block.id)
            i++
        }
        val after = currentBlocks.subList(i, currentBlocks.size)
        val result = before + newBlocks + after
        return result to DocumentPatch(
            addedBlocks = newBlocks,
            removedBlockIds = removed,
            updatedBlocks = emptyList(),
            dirtyRange = damageRange,
        )
    }
}
