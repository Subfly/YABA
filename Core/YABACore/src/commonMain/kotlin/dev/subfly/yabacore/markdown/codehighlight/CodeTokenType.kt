package dev.subfly.yabacore.markdown.codehighlight

/**
 * Token type for syntax highlighting. Used to map spans to colors in the UI.
 */
enum class CodeTokenType {
    Comment,
    String,
    Keyword,
    Number,
    Identifier,
    Operator,
    Whitespace,
}
