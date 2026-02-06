package dev.subfly.yabacore.markdown.parser

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.markdown.ast.InlineNode
import dev.subfly.yabacore.markdown.core.Range

/**
 * Parses inline markdown with a character scanner.
 * Returns inline nodes with absolute document offsets (baseOffset + local offset).
 */
object InlineParser {

    fun parse(input: String, baseOffset: Int = 0): List<InlineNode> {
        if (input.isEmpty()) return emptyList()
        val result = mutableListOf<InlineNode>()
        var i = 0
        while (i < input.length) {
            when {
                input[i] == '\n' -> {
                    val id = IdGenerator.newId()
                    result.add(InlineNode.SoftBreak(id, Range(baseOffset + i, baseOffset + i + 1)))
                    i++
                }

                input[i] == '[' -> {
                    val footnoteRef = matchFootnoteRef(input, i)
                    if (footnoteRef != null) {
                        val (endIdx, refId) = footnoteRef
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.FootnoteRef(
                                id,
                                Range(baseOffset + i, baseOffset + endIdx),
                                emptyList(),
                                refId
                            )
                        )
                        i = endIdx
                        continue
                    }
                    val linkMatch = matchLink(input, i)
                    if (linkMatch != null) {
                        val (textNodes, endIdx) = linkMatch
                        val id = IdGenerator.newId()
                        val range = Range(baseOffset + i, baseOffset + endIdx)
                        val url = extractLinkUrl(input, i, endIdx)
                        val title = extractLinkTitle(input, i, endIdx)
                        result.add(InlineNode.Link(id, range, textNodes, url, title))
                        i = endIdx
                        continue
                    }
                }

                input[i] == '!' && i + 1 < input.length && input[i + 1] == '[' -> {
                    val imageMatch = matchImage(input, i)
                    if (imageMatch != null) {
                        val (endIdx, url, alt, title) = imageMatch
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Image(
                                id,
                                Range(baseOffset + i, baseOffset + endIdx),
                                emptyList(),
                                url,
                                alt,
                                title
                            )
                        )
                        i = endIdx
                        continue
                    }
                }

                i + 1 < input.length && input[i] == '*' && input[i + 1] == '*' -> {
                    val end = findUnescaped(input, "**", i + 2)
                    if (end != -1) {
                        val inner = input.substring(i + 2, end)
                        val innerNodes = parse(inner, baseOffset + i + 2)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Strong(
                                id,
                                Range(baseOffset + i, baseOffset + end + 2),
                                innerNodes
                            )
                        )
                        i = end + 2
                        continue
                    }
                }

                input[i] == '*' -> {
                    val end = findUnescapedSingleAsterisk(input, i + 1)
                    if (end != -1) {
                        val inner = input.substring(i + 1, end)
                        val innerNodes = parse(inner, baseOffset + i + 1)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Emphasis(
                                id,
                                Range(baseOffset + i, baseOffset + end + 1),
                                innerNodes
                            )
                        )
                        i = end + 1
                        continue
                    }
                }

                input[i] == '_' -> {
                    val end = findUnescaped(input, "_", i + 1)
                    if (end != -1 && (i == 0 || !input[i - 1].isLetterOrDigit()) && (end + 1 >= input.length || !input[end + 1].isLetterOrDigit())) {
                        val inner = input.substring(i + 1, end)
                        val innerNodes = parse(inner, baseOffset + i + 1)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Emphasis(
                                id,
                                Range(baseOffset + i, baseOffset + end + 1),
                                innerNodes
                            )
                        )
                        i = end + 1
                        continue
                    }
                }

                input[i] == '`' -> {
                    val n = countBackticks(input, i)
                    val end = findClosingBackticks(input, i + n, n)
                    if (end != -1) {
                        val literal = input.substring(i + n, end)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.InlineCode(
                                id,
                                Range(baseOffset + i, baseOffset + end + n),
                                emptyList(),
                                literal
                            )
                        )
                        i = end + n
                        continue
                    }
                }

                input.startsWith("~~", i) -> {
                    val end = findUnescaped(input, "~~", i + 2)
                    if (end != -1) {
                        val inner = input.substring(i + 2, end)
                        val innerNodes = parse(inner, baseOffset + i + 2)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Strikethrough(
                                id,
                                Range(baseOffset + i, baseOffset + end + 2),
                                innerNodes
                            )
                        )
                        i = end + 2
                        continue
                    }
                }

                input[i] == '^' && i + 1 < input.length && !input[i + 1].isWhitespace() -> {
                    val end = findUnescaped(input, "^", i + 1)
                    if (end != -1 && end > i + 1 && (end == i + 2 || !input[end - 1].isWhitespace())) {
                        val inner = input.substring(i + 1, end)
                        val innerNodes = parse(inner, baseOffset + i + 1)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Superscript(
                                id,
                                Range(baseOffset + i, baseOffset + end + 1),
                                innerNodes
                            )
                        )
                        i = end + 1
                        continue
                    }
                }

                input[i] == '~' && i + 1 < input.length && input[i + 1] != '~' -> {
                    val end = input.indexOf('~', i + 1)
                    if (end != -1 && end > i + 1 && (end == i + 2 || !input[end - 1].isWhitespace()) && (i + 1 >= input.length || !input[i + 1].isWhitespace())) {
                        val inner = input.substring(i + 1, end)
                        val innerNodes = parse(inner, baseOffset + i + 1)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.Subscript(
                                id,
                                Range(baseOffset + i, baseOffset + end + 1),
                                innerNodes
                            )
                        )
                        i = end + 1
                        continue
                    }
                }
            }
            matchAutolink(input, i)?.let { (linkEnd, url) ->
                val id = IdGenerator.newId()
                result.add(
                    InlineNode.Link(
                        id,
                        Range(baseOffset + i, baseOffset + linkEnd),
                        listOf(
                            InlineNode.Text(
                                IdGenerator.newId(),
                                Range(baseOffset + i, baseOffset + linkEnd),
                                emptyList(),
                                url
                            )
                        ),
                        url,
                        null
                    )
                )
                i = linkEnd
                continue
            }

            val nextSpecial =
                input.indexOfAny(charArrayOf('[', '*', '_', '`', '\n', '~', '\\', '^'), i)
            val end = if (nextSpecial == -1) input.length else nextSpecial
            if (end > i) {
                val slice = input.substring(i, end)
                if (slice == "  \n" || (slice.endsWith("  ") && i + 2 < input.length && input[i + 2] == '\n')) {
                    val hardEnd = if (slice == "  \n") i + 3 else i + 2
                    val id = IdGenerator.newId()
                    result.add(
                        InlineNode.HardBreak(
                            id,
                            Range(baseOffset + i, baseOffset + hardEnd)
                        )
                    )
                    i = hardEnd
                } else {
                    val displayText = processBackslashEscapes(slice)
                    val id = IdGenerator.newId()
                    result.add(
                        InlineNode.Text(
                            id,
                            Range(baseOffset + i, baseOffset + end),
                            emptyList(),
                            displayText
                        )
                    )
                    i = end
                }
            } else {
                if (input[i] == '\\' && i + 1 < input.length) {
                    val displayChar = processBackslashEscapes(input.substring(i, i + 2))
                    val id = IdGenerator.newId()
                    result.add(
                        InlineNode.Text(
                            id,
                            Range(baseOffset + i, baseOffset + i + 2),
                            emptyList(),
                            displayChar
                        )
                    )
                    i += 2
                } else {
                    val id = IdGenerator.newId()
                    result.add(
                        InlineNode.Text(
                            id,
                            Range(baseOffset + i, baseOffset + i + 1),
                            emptyList(),
                            input[i].toString()
                        )
                    )
                    i++
                }
            }
        }
        return result
    }

    private data class LinkMatch(val textNodes: List<InlineNode>, val endIndex: Int)

    private fun matchLink(s: String, start: Int): LinkMatch? {
        if (s[start] != '[') return null
        val closeBracket = s.indexOf(']', start + 1)
        if (closeBracket == -1) return null
        val textSlice = s.substring(start + 1, closeBracket)
        var i = closeBracket + 1
        while (i < s.length && s[i] == ' ') i++
        if (i >= s.length || s[i] != '(') return null
        i++
        val closeParen = s.indexOf(')', i)
        if (closeParen == -1) return null
        val textNodes = parse(textSlice, start + 1)
        return LinkMatch(textNodes, closeParen + 1)
    }

    private fun extractLinkUrl(s: String, start: Int, endIndex: Int): String {
        val openParen = s.indexOf('(', s.indexOf(']', start + 1) + 1)
        if (openParen == -1) return ""
        val closeParen = s.indexOf(')', openParen + 1)
        if (closeParen == -1) return ""
        val afterUrl = s.indexOf(' ', openParen + 1)
        val urlEnd = when {
            afterUrl == -1 -> closeParen
            afterUrl < closeParen -> afterUrl
            else -> closeParen
        }
        return s.substring(openParen + 1, urlEnd).trim()
    }

    private fun extractLinkTitle(s: String, start: Int, endIndex: Int): String? {
        val openParen = s.indexOf('(', s.indexOf(']', start + 1) + 1)
        if (openParen == -1) return null
        val quote = s.indexOf('"', openParen + 1)
        if (quote == -1) return null
        val closeQuote = s.indexOf('"', quote + 1)
        if (closeQuote == -1) return null
        return s.substring(quote + 1, closeQuote)
    }

    private fun matchImage(s: String, start: Int): ImageMatch? {
        if (s[start] != '!' || start + 1 >= s.length || s[start + 1] != '[') return null
        val closeBracket = s.indexOf(']', start + 2)
        if (closeBracket == -1) return null
        var i = closeBracket + 1
        while (i < s.length && s[i] == ' ') i++
        if (i >= s.length || s[i] != '(') return null
        i++
        val closeParen = s.indexOf(')', i)
        if (closeParen == -1) return null
        val alt = s.substring(start + 2, closeBracket).takeIf { it.isNotBlank() }
        val urlContent = s.substring(i, closeParen)
        val url = urlContent.substringBefore(" ").trim().removeSurrounding("\"", "\"")
        val titlePart = urlContent.substringAfter(" ", "").trim()
        val title = when {
            titlePart.startsWith("\"") && titlePart.length > 2 -> titlePart.substring(
                1,
                titlePart.length - 1
            )

            else -> null
        }
        return ImageMatch(closeParen + 1, url, alt, title)
    }

    private data class ImageMatch(
        val endIndex: Int,
        val url: String,
        val alt: String?,
        val title: String?,
    )

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

    /** Markdown escapable characters (backslash followed by one of these is dropped). */
    private val escapableChars = setOf(
        '\\', '`', '*', '_', '{', '}', '[', ']', '(', ')', '#', '+', '-', '.', '!', '|'
    )

    private fun processBackslashEscapes(s: String): String {
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length && s[i + 1] in escapableChars) {
                out.append(s[i + 1])
                i += 2
            } else {
                out.append(s[i])
                i++
            }
        }
        return out.toString()
    }

    /**
     * If at [start] we have a bare URL (http://, https://, or www.), returns (endIndex, url).
     * End of URL: space, newline, or closing punctuation like ), ], etc.
     */
    private fun matchFootnoteRef(input: String, start: Int): Pair<Int, String>? {
        if (start >= input.length || input[start] != '[' || start + 2 > input.length || input[start + 1] != '^') return null
        val closeBracket = input.indexOf(']', start + 2)
        if (closeBracket == -1) return null
        val refId = input.substring(start + 2, closeBracket).trim()
        if (refId.isEmpty()) return null
        return closeBracket + 1 to refId
    }

    private fun matchAutolink(input: String, start: Int): Pair<Int, String>? {
        if (start >= input.length) return null
        val prefix: String
        val urlStart: Int
        when {
            input.startsWith("https://", start) -> {
                prefix = "https://"
                urlStart = start
            }

            input.startsWith("http://", start) -> {
                prefix = "http://"
                urlStart = start
            }

            input.startsWith("www.", start) -> {
                prefix = "www."
                urlStart = start
            }

            else -> return null
        }
        var i = urlStart + prefix.length
        while (i < input.length) {
            val c = input[i]
            when {
                c.isWhitespace() -> break
                c in ")\\]>\"'`*,;:!?" -> break
                c == '<' -> break
            }
            i++
        }
        if (i == urlStart + prefix.length) return null
        val url = input.substring(urlStart, i)
        return i to url
    }

    private fun countBackticks(input: String, start: Int): Int {
        var i = start
        while (i < input.length && input[i] == '`') i++
        return i - start
    }

    /** Find closing run of exactly [n] backticks starting at [start]. Returns start index of closing run, or -1. */
    private fun findClosingBackticks(input: String, start: Int, n: Int): Int {
        var i = start
        while (i <= input.length - n) {
            if (input[i] != '`') {
                i++
                continue
            }
            var j = i
            while (j < input.length && input[j] == '`') j++
            val count = j - i
            if (count == n) return i
            i = j
        }
        return -1
    }
}
