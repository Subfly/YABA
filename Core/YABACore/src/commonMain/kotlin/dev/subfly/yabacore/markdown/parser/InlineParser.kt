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
                    val end = input.indexOf('`', i + 1)
                    if (end != -1) {
                        val literal = input.substring(i + 1, end)
                        val id = IdGenerator.newId()
                        result.add(
                            InlineNode.InlineCode(
                                id,
                                Range(baseOffset + i, baseOffset + end + 1),
                                emptyList(),
                                literal
                            )
                        )
                        i = end + 1
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
            }
            val nextSpecial = input.indexOfAny(charArrayOf('[', '*', '_', '`', '\n', '~'), i)
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
                    val id = IdGenerator.newId()
                    result.add(
                        InlineNode.Text(
                            id,
                            Range(baseOffset + i, baseOffset + end),
                            emptyList(),
                            slice
                        )
                    )
                    i = end
                }
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
}
