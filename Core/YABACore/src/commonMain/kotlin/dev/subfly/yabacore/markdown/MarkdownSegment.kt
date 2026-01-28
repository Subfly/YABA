package dev.subfly.yabacore.markdown

/**
 * A segment produced by the markdown subset parser.
 * Used to build a single AnnotatedString with inline content placeholders.
 */
sealed interface MarkdownSegment {
    /** Plain or styled text to append */
    data class Text(
        val content: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val code: Boolean = false,
        val linkUrl: String? = null,
    ) : MarkdownSegment

    /** Heading (level 1–6); content is the heading text */
    data class Heading(val level: Int, val content: String) : MarkdownSegment

    /** Image placeholder; key = "img:<assetId>" */
    data class Image(
        val assetId: String,
        val path: String,
        val alt: String?,
        val caption: String?,
    ) : MarkdownSegment

    /** Code block; key = "code:<index>" */
    data class CodeBlock(val language: String?, val text: String) : MarkdownSegment

    /** Table; key = "tbl:<index>" */
    data class Table(val header: List<String>, val rows: List<List<String>>) : MarkdownSegment

    /** Blockquote; key = "quote:<index>" */
    data class Quote(val content: String) : MarkdownSegment

    /** List; key = "list:<index>" */
    data class ListBlock(val ordered: Boolean, val items: List<String>) : MarkdownSegment

    /** Horizontal rule */
    data object Divider : MarkdownSegment
}
