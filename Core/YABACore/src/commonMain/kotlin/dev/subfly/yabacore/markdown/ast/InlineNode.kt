package dev.subfly.yabacore.markdown.ast

import dev.subfly.yabacore.markdown.core.Range

/**
 * Inline-level AST node. All nodes have stable id and rope-based range [start, end).
 */
sealed interface InlineNode {
    val id: String
    val range: Range
    val children: List<InlineNode>

    data class Text(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode> = emptyList(),
        val literal: String,
    ) : InlineNode

    data class Emphasis(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode>,
    ) : InlineNode

    data class Strong(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode>,
    ) : InlineNode

    data class Strikethrough(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode>,
    ) : InlineNode

    data class InlineCode(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode> = emptyList(),
        val literal: String,
    ) : InlineNode

    data class Link(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode>,
        val url: String,
        val title: String? = null,
    ) : InlineNode

    data class Image(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode> = emptyList(),
        val url: String,
        val alt: String? = null,
        val title: String? = null,
    ) : InlineNode

    data class SoftBreak(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode> = emptyList(),
    ) : InlineNode

    data class HardBreak(
        override val id: String,
        override val range: Range,
        override val children: List<InlineNode> = emptyList(),
    ) : InlineNode
}
