package dev.subfly.yabacore.markdown

/**
 * Result of parsing inline markdown within a paragraph/heading/list item.
 * Each part has optional bold, italic, code styling; [linkUrl] is set for links.
 */
data class InlinePart(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val code: Boolean = false,
    val linkUrl: String? = null,
)

/**
 * Parses inline markdown: **bold**, *italic*, `code`, [text](url).
 * Returns a flat list of parts with combined styles (e.g. bold+italic).
 * Does not handle nested delimiters; first match wins left-to-right.
 */
fun parseInlineMarkdown(input: String): List<InlinePart> {
    if (input.isEmpty()) return emptyList()
    val result = mutableListOf<InlinePart>()
    var i = 0
    while (i < input.length) {
        when {
            // Link: [text](url) - take precedence so we don't parse ** inside
            input[i] == '[' -> {
                val linkMatch = matchLink(input, i)
                if (linkMatch != null) {
                    result.add(InlinePart(text = linkMatch.text, linkUrl = linkMatch.url))
                    i = linkMatch.endIndex
                    continue
                }
            }
            // Bold: **...** (must not be single *)
            i + 1 < input.length && input[i] == '*' && input[i + 1] == '*' -> {
                val end = findUnescaped(input, "**", i + 2)
                if (end != -1) {
                    val inner = input.substring(i + 2, end)
                    result.addAll(parseInlineMarkdown(inner).map { it.copy(bold = true) })
                    i = end + 2
                    continue
                }
            }
            // Italic: *...* (single, not **)
            input[i] == '*' -> {
                val end = findUnescapedSingleAsterisk(input, i + 1)
                if (end != -1) {
                    val inner = input.substring(i + 1, end)
                    result.addAll(parseInlineMarkdown(inner).map { it.copy(italic = true) })
                    i = end + 1
                    continue
                }
            }
            // Underscore italic: _..._
            input[i] == '_' -> {
                val end = findUnescaped(input, "_", i + 1)
                if (end != -1 && (i == 0 || !input[i - 1].isLetterOrDigit()) && (end + 1 >= input.length || !input[end + 1].isLetterOrDigit())) {
                    val inner = input.substring(i + 1, end)
                    result.addAll(parseInlineMarkdown(inner).map { it.copy(italic = true) })
                    i = end + 1
                    continue
                }
            }
            // Inline code: `...`
            input[i] == '`' -> {
                val end = input.indexOf('`', i + 1)
                if (end != -1) {
                    result.add(InlinePart(text = input.substring(i + 1, end), code = true))
                    i = end + 1
                    continue
                }
            }
        }
        // Plain: advance until next special char
        val nextSpecial = input.indexOfAny(charArrayOf('[', '*', '_', '`'), i)
        val end = if (nextSpecial == -1) input.length else nextSpecial
        if (end > i) {
            result.add(InlinePart(text = input.substring(i, end)))
            i = end
        } else {
            result.add(InlinePart(text = input[i].toString()))
            i++
        }
    }
    return result
}

private fun findUnescaped(s: String, delim: String, start: Int): Int {
    var i = start
    while (i <= s.length - delim.length) {
        if (s.substring(i, i + delim.length) == delim && (i == 0 || s[i - 1] != '\\')) return i
        i++
    }
    return -1
}

private fun findUnescapedSingleAsterisk(s: String, start: Int): Int {
    var i = start
    while (i < s.length) {
        when {
            s[i] == '*' -> {
                if (i + 1 < s.length && s[i + 1] == '*') {
                    i += 2
                    continue
                }
                if (i == 0 || s[i - 1] != '\\') return i
            }
        }
        i++
    }
    return -1
}

private data class LinkMatch(val text: String, val url: String, val endIndex: Int)

private fun matchLink(s: String, start: Int): LinkMatch? {
    if (s[start] != '[') return null
    val closeBracket = s.indexOf(']', start + 1)
    if (closeBracket == -1) return null
    val text = s.substring(start + 1, closeBracket)
    var i = closeBracket + 1
    while (i < s.length && s[i] == ' ') i++
    if (i >= s.length || s[i] != '(') return null
    i++
    val closeParen = s.indexOf(')', i)
    if (closeParen == -1) return null
    val url = s.substring(i, closeParen)
    return LinkMatch(text = text, url = url, endIndex = closeParen + 1)
}
