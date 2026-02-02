package dev.subfly.yabacore.markdown.formatting

import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.ast.InlineNode

/**
 * Generates [EditorStyleSpan] list from AST for editor styling.
 * Cache per block ID; on patch, update spans only for affected blocks.
 */
object EditorStyler {

    fun buildSpans(document: BlockNode.Document): List<EditorStyleSpan> {
        val spans = mutableListOf<EditorStyleSpan>()
        for (block in document.children) {
            collectSpansFromBlock(block, spans)
        }
        return spans
    }

    private fun collectSpansFromBlock(block: BlockNode, out: MutableList<EditorStyleSpan>) {
        when (block) {
            is BlockNode.Document -> block.children.forEach { collectSpansFromBlock(it, out) }
            is BlockNode.Heading -> {
                out.add(EditorStyleSpan(block.range, EditorSpanStyle.HEADING))
                block.inline.forEach { collectSpansFromInline(it, out) }
            }
            is BlockNode.Paragraph -> block.inline.forEach { collectSpansFromInline(it, out) }
            is BlockNode.BlockQuote -> block.children.forEach { collectSpansFromBlock(it, out) }
            is BlockNode.ListBlock -> block.children.forEach { collectSpansFromBlock(it, out) }
            is BlockNode.ListItem -> block.inline.forEach { collectSpansFromInline(it, out) }
            is BlockNode.CodeFence -> {
                out.add(EditorStyleSpan(block.range, EditorSpanStyle.CODE))
            }
            is BlockNode.HorizontalRule, is BlockNode.TableBlock -> {}
        }
    }

    private fun collectSpansFromInline(node: InlineNode, out: MutableList<EditorStyleSpan>) {
        when (node) {
            is InlineNode.Text -> {}
            is InlineNode.Emphasis -> {
                out.add(EditorStyleSpan(node.range, EditorSpanStyle.ITALIC))
                node.children.forEach { collectSpansFromInline(it, out) }
            }
            is InlineNode.Strong -> {
                out.add(EditorStyleSpan(node.range, EditorSpanStyle.BOLD))
                node.children.forEach { collectSpansFromInline(it, out) }
            }
            is InlineNode.Strikethrough -> {
                out.add(EditorStyleSpan(node.range, EditorSpanStyle.STRIKETHROUGH))
                node.children.forEach { collectSpansFromInline(it, out) }
            }
            is InlineNode.InlineCode -> out.add(EditorStyleSpan(node.range, EditorSpanStyle.CODE))
            is InlineNode.Link -> {
                out.add(EditorStyleSpan(node.range, EditorSpanStyle.LINK))
                node.children.forEach { collectSpansFromInline(it, out) }
            }
            is InlineNode.Image -> {}
            is InlineNode.SoftBreak, is InlineNode.HardBreak -> {}
        }
    }
}
