package dev.subfly.yabacore.markdown.ast

import dev.subfly.yabacore.markdown.core.Range

enum class TableAlignment {
    LEFT,
    CENTER,
    RIGHT,
}

/**
 * Block-level AST node. All nodes have stable id and range [start, end).
 * Children are either block nodes (for container blocks) or inline nodes (for leaf blocks).
 */
sealed interface BlockNode {
    val id: String
    val range: Range
    val children: List<BlockNode>

    data class Document(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
    ) : BlockNode

    data class Heading(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode> = emptyList(),
        val level: Int,
        val inline: List<InlineNode>,
        val customId: String? = null,
    ) : BlockNode

    data class Paragraph(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode> = emptyList(),
        val inline: List<InlineNode>,
    ) : BlockNode

    data class BlockQuote(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
    ) : BlockNode

    data class ListBlock(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
        val ordered: Boolean,
    ) : BlockNode

    data class ListItem(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
        val checked: Boolean? = null,
        val inline: List<InlineNode>,
    ) : BlockNode

    data class CodeFence(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode> = emptyList(),
        val language: String? = null,
        val literal: String,
    ) : BlockNode

    data class HorizontalRule(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode> = emptyList(),
    ) : BlockNode

    data class TableBlock(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode> = emptyList(),
        /** Header cells: each element is the inline content of one cell */
        val header: List<List<InlineNode>>,
        /** Rows: each row is a list of cells, each cell is a list of inline nodes */
        val rows: List<List<List<InlineNode>>>,
        /** Column alignments (left, center, right); size should match header/row cell count */
        val alignments: List<TableAlignment> = emptyList(),
    ) : BlockNode

    data class DefinitionList(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
    ) : BlockNode

    data class DefinitionItem(
        override val id: String,
        override val range: Range,
        override val children: List<BlockNode>,
        val term: List<InlineNode>,
        val definitions: List<List<InlineNode>>,
    ) : BlockNode
}
