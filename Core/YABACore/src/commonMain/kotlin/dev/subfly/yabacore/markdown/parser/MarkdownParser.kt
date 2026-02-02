package dev.subfly.yabacore.markdown.parser

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.ast.DocumentNode
import dev.subfly.yabacore.markdown.core.Range

/**
 * Entry point for parsing markdown into an AST.
 * Produces a DocumentNode (root) whose children are the top-level blocks.
 */
object MarkdownParser {

    /**
     * Parses [source] into a [DocumentNode] with block children.
     * All node ranges are absolute offsets into [source].
     */
    fun parse(source: String): DocumentNode {
        val blocks = BlockParser.parse(source)
        val range = if (source.isEmpty()) Range(0, 0) else Range(0, source.length)
        val id = IdGenerator.newId()
        return BlockNode.Document(id, range, blocks)
    }
}
