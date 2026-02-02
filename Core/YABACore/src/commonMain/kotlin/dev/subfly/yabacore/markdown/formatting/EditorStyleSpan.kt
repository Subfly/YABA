package dev.subfly.yabacore.markdown.formatting

import dev.subfly.yabacore.markdown.core.Range

/**
 * A span for editor styling: range in document + style to apply (e.g. bold, italic, code).
 * Used to style plain text in the editor without changing layout.
 */
data class EditorStyleSpan(
    val range: Range,
    val style: EditorSpanStyle,
)

enum class EditorSpanStyle {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    CODE,
    LINK,
    HEADING,
}
