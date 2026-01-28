package dev.subfly.yabacore.markdown

/**
 * Parses a subset of Markdown (as emitted by ReadableExtractor) into [MarkdownSegment] list.
 * Handles: headings, paragraphs, images ![](path), code blocks, tables, blockquotes, lists, hr.
 */
object MarkdownParser {
    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val imageRegex = Regex("!\\[(.*?)]\\((.*?)\\)")
    private val tableRowRegex = Regex("^\\|(.+)\\|$")
    private val listItemUnorderedRegex = Regex("^[-*+]\\s+(.*)$")
    private val listItemOrderedRegex = Regex("^\\d+\\.\\s+(.*)$")

    fun parse(markdown: String): List<MarkdownSegment> {
        val segments = mutableListOf<MarkdownSegment>()
        val lines = markdown.split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            when {
                line.isBlank() -> {
                    i++
                    continue
                }
                line.trim() == "---" -> {
                    segments.add(MarkdownSegment.Divider)
                    i++
                    continue
                }
                headingRegex.matches(line) -> {
                    val (hashes, content) = headingRegex.find(line)!!.destructured
                    segments.add(MarkdownSegment.Heading(hashes.length, content.trim()))
                    i++
                    continue
                }
                line.trimStart().startsWith("```") -> {
                    val lang = line.trimStart().removePrefix("```").trim().ifBlank { null }
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    segments.add(MarkdownSegment.CodeBlock(lang, codeLines.joinToString("\n")))
                    if (i < lines.size) i++
                    continue
                }
                line.trimStart().startsWith(">") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        quoteLines.add(lines[i].trimStart().removePrefix(">").trim())
                        i++
                    }
                    segments.add(MarkdownSegment.Quote(quoteLines.joinToString("\n")))
                    continue
                }
                listItemUnorderedRegex.matches(line.trim()) -> {
                    val (nextI, items) = parseList(lines, i, ordered = false)
                    segments.add(MarkdownSegment.ListBlock(false, items))
                    i = nextI
                    continue
                }
                listItemOrderedRegex.matches(line.trim()) -> {
                    val (nextI, items) = parseList(lines, i, ordered = true)
                    segments.add(MarkdownSegment.ListBlock(true, items))
                    i = nextI
                    continue
                }
                tableRowRegex.matches(line.trim()) -> {
                    val (nextI, header, rows) = parseTable(lines, i)
                    segments.add(MarkdownSegment.Table(header, rows))
                    i = nextI
                    continue
                }
                else -> {
                    val paragraphLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !headingRegex.matches(lines[i]) &&
                        !lines[i].trimStart().startsWith("```") &&
                        !lines[i].trimStart().startsWith(">") &&
                        !listItemUnorderedRegex.matches(lines[i].trim()) &&
                        !listItemOrderedRegex.matches(lines[i].trim()) &&
                        !tableRowRegex.matches(lines[i].trim()) &&
                        lines[i].trim() != "---"
                    ) {
                        paragraphLines.add(lines[i])
                        i++
                    }
                    val paragraphText = paragraphLines.joinToString("\n").trim()
                    if (paragraphText.isNotBlank()) {
                        segments.addAll(parseInlineSegments(paragraphText))
                    }
                    continue
                }
            }
        }
        return segments
    }

    private fun parseList(lines: List<String>, start: Int, ordered: Boolean): Pair<Int, List<String>> {
        val items = mutableListOf<String>()
        var i = start
        val regex = if (ordered) listItemOrderedRegex else listItemUnorderedRegex
        while (i < lines.size) {
            val trimmed = lines[i].trim()
            if (trimmed.isBlank()) {
                i++
                break
            }
            val match = regex.find(trimmed) ?: break
            items.add(match.groupValues[1].trim())
            i++
        }
        return i to items
    }

    private fun parseTable(lines: List<String>, start: Int): Triple<Int, List<String>, List<List<String>>> {
        fun splitCells(row: String): List<String> =
            row.trim().removeSurrounding("|").split("|").map { it.trim().replace("\\|", "|") }

        var i = start
        val header = splitCells(lines[i])
        i++
        val rows = mutableListOf<List<String>>()
        while (i < lines.size && tableRowRegex.matches(lines[i].trim())) {
            val row = lines[i].trim()
            if (row.contains("---")) {
                i++
                continue
            }
            rows.add(splitCells(row))
            i++
        }
        return Triple(i, header, rows)
    }

    /**
     * Splits a paragraph into text and image segments (images are ![](path) with path like ../assets/xxx.ext).
     */
    private fun parseInlineSegments(paragraph: String): List<MarkdownSegment> {
        val result = mutableListOf<MarkdownSegment>()
        var remaining = paragraph
        while (true) {
            val match = imageRegex.find(remaining) ?: break
            val before = remaining.substring(0, match.range.first)
            if (before.isNotBlank()) {
                result.add(MarkdownSegment.Text(before))
            }
            val alt = match.groupValues[1].replace("\\]", "]")
            val path = match.groupValues[2]
            val assetId = path.substringAfterLast("/").substringBeforeLast(".")
            result.add(MarkdownSegment.Image(assetId = assetId, path = path, alt = alt.ifBlank { null }, caption = null))
            remaining = remaining.substring(match.range.last + 1)
        }
        if (remaining.isNotBlank()) {
            result.add(MarkdownSegment.Text(remaining))
        }
        if (result.isEmpty() && paragraph.isNotBlank()) {
            result.add(MarkdownSegment.Text(paragraph))
        }
        return result
    }
}
