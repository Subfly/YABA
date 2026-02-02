package dev.subfly.yabacore.markdown.parser

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.ast.InlineNode
import dev.subfly.yabacore.markdown.core.Range

/**
 * Line-based block parser. Tracks absolute character offsets into the source.
 * Priority: code fences > headings > hr > blockquotes > lists > tables > paragraphs.
 */
object BlockParser {

    private val headingRegex = Regex("^(#{1,6})\\s+(.+)$")
    private val tableRowRegex = Regex("^\\|(.+)\\|$")
    private val listItemUnorderedRegex = Regex("^[-*+]\\s+(.*)$")
    private val listItemOrderedRegex = Regex("^\\d+\\.\\s+(.*)$")
    private val taskListItemRegex = Regex("^[-*+]\\s+\\[([ xX])]\\s+(.*)$")

    fun parse(source: String): List<BlockNode> {
        val blocks = mutableListOf<BlockNode>()
        val lines = source.split("\n")
        var charOffset = 0
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val lineStart = charOffset
            val lineEnd = charOffset + line.length
            when {
                line.isBlank() -> {
                    charOffset = lineEnd + 1
                    i++
                    continue
                }

                line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                    val id = IdGenerator.newId()
                    blocks.add(BlockNode.HorizontalRule(id, Range(lineStart, lineEnd)))
                    charOffset = lineEnd + 1
                    i++
                    continue
                }

                line.trimStart().startsWith("```") -> {
                    val fenceStart = lineStart + line.takeWhile { it == ' ' }.length
                    val lang = line.trimStart().removePrefix("```").trim().ifBlank { null }
                    val codeLines = mutableListOf<String>()
                    charOffset = lineEnd + 1
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        charOffset += lines[i].length + 1
                        i++
                    }
                    val fenceEnd =
                        if (i < lines.size) charOffset + lines[i].length + 1 else charOffset
                    val codeText = codeLines.joinToString("\n")
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.CodeFence(
                            id,
                            Range(fenceStart, fenceEnd),
                            emptyList(),
                            lang,
                            codeText
                        )
                    )
                    if (i < lines.size) {
                        charOffset = fenceEnd
                        i++
                    }
                    continue
                }

                line.trimStart().startsWith(">") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        quoteLines.add(lines[i].trimStart().removePrefix(">").trim())
                        charOffset += lines[i].length + 1
                        i++
                    }
                    val quoteEnd = charOffset - 1
                    val quoteContent = quoteLines.joinToString("\n")
                    val firstQuoteLine = lines[i - quoteLines.size]
                    val contentStartOffset = lineStart +
                            firstQuoteLine.takeWhile { it == ' ' }.length +
                            1 +
                            (if (firstQuoteLine.trimStart().startsWith("> ")) 1 else 0)
                    val inline = InlineParser.parse(quoteContent, contentStartOffset)
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.BlockQuote(
                            id, Range(lineStart, quoteEnd), listOf(
                                BlockNode.Paragraph(
                                    IdGenerator.newId(),
                                    Range(lineStart, quoteEnd),
                                    emptyList(),
                                    inline
                                )
                            )
                        )
                    )
                    continue
                }

                taskListItemRegex.matches(line.trim()) -> {
                    val (nextI, items) = parseList(
                        source,
                        lines,
                        i,
                        charOffset,
                        ordered = false,
                        taskList = true
                    )
                    val id = IdGenerator.newId()
                    val itemBlocks = items.map { (content, checked, range) ->
                        val inl = InlineParser.parse(content, range.start)
                        BlockNode.ListItem(IdGenerator.newId(), range, emptyList(), checked, inl)
                    }
                    val listRange = Range(
                        lineStart,
                        if (nextI < lines.size) charOffsetForLine(
                            lines,
                            nextI
                        ) - 1 else source.length
                    )
                    blocks.add(BlockNode.ListBlock(id, listRange, itemBlocks, false))
                    i = nextI
                    charOffset = if (i < lines.size) charOffsetForLine(lines, i) else source.length
                    continue
                }

                listItemUnorderedRegex.matches(line.trim()) -> {
                    val (nextI, items) = parseList(
                        source,
                        lines,
                        i,
                        charOffset,
                        ordered = false,
                        taskList = false
                    )
                    val id = IdGenerator.newId()
                    val itemBlocks = items.map { (content, _, range) ->
                        val inl = InlineParser.parse(content, range.start)
                        BlockNode.ListItem(IdGenerator.newId(), range, emptyList(), null, inl)
                    }
                    val listRange = Range(
                        lineStart,
                        if (nextI < lines.size) charOffsetForLine(
                            lines,
                            nextI
                        ) - 1 else source.length
                    )
                    blocks.add(BlockNode.ListBlock(id, listRange, itemBlocks, false))
                    i = nextI
                    charOffset = if (i < lines.size) charOffsetForLine(lines, i) else source.length
                    continue
                }

                listItemOrderedRegex.matches(line.trim()) -> {
                    val (nextI, items) = parseList(
                        source,
                        lines,
                        i,
                        charOffset,
                        ordered = true,
                        taskList = false
                    )
                    val id = IdGenerator.newId()
                    val itemBlocks = items.map { (content, _, range) ->
                        val inl = InlineParser.parse(content, range.start)
                        BlockNode.ListItem(IdGenerator.newId(), range, emptyList(), null, inl)
                    }
                    val listRange = Range(
                        lineStart,
                        if (nextI < lines.size) charOffsetForLine(
                            lines,
                            nextI
                        ) - 1 else source.length
                    )
                    blocks.add(BlockNode.ListBlock(id, listRange, itemBlocks, true))
                    i = nextI
                    charOffset = if (i < lines.size) charOffsetForLine(lines, i) else source.length
                    continue
                }

                headingRegex.matches(line.trim()) -> {
                    val trimmed = line.trim()
                    val (hashes, content) = headingRegex.find(trimmed)!!.destructured
                    val contentStart = lineStart + line.indexOf(trimmed) + hashes.length + 1
                    val inline = InlineParser.parse(content.trim(), contentStart)
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.Heading(
                            id,
                            Range(lineStart, lineEnd),
                            emptyList(),
                            hashes.length,
                            inline
                        )
                    )
                    charOffset = lineEnd + 1
                    i++
                    continue
                }

                tableRowRegex.matches(line.trim()) -> {
                    val (nextI, headerCells, rows) = parseTable(source, lines, i, charOffset)
                    val headerInline = headerCells.map { (text, r) ->
                        InlineParser.parse(text, r.start).ifEmpty {
                            listOf(
                                InlineNode.Text(
                                    IdGenerator.newId(),
                                    r,
                                    emptyList(),
                                    text
                                )
                            )
                        }
                    }
                    val rowInlines = rows.map { row ->
                        row.map { (text, r) ->
                            InlineParser.parse(text, r.start).ifEmpty {
                                listOf(
                                    InlineNode.Text(
                                        IdGenerator.newId(),
                                        r,
                                        emptyList(),
                                        text
                                    )
                                )
                            }
                        }
                    }
                    val tableStart = lineStart
                    val tableEnd = if (nextI < lines.size) charOffsetForLine(
                        lines,
                        nextI
                    ) - 1 else source.length
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.TableBlock(
                            id,
                            Range(tableStart, tableEnd),
                            emptyList(),
                            headerInline,
                            rowInlines
                        )
                    )
                    i = nextI
                    charOffset = if (i < lines.size) charOffsetForLine(lines, i) else source.length
                    continue
                }

                else -> {
                    val paraStart = lineStart
                    val paraLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !headingRegex.matches(lines[i].trim()) &&
                        !lines[i].trimStart().startsWith("```") &&
                        !lines[i].trimStart().startsWith(">") &&
                        !listItemUnorderedRegex.matches(lines[i].trim()) &&
                        !listItemOrderedRegex.matches(lines[i].trim()) &&
                        !tableRowRegex.matches(lines[i].trim()) &&
                        lines[i].trim() != "---" && lines[i].trim() != "***" && lines[i].trim() != "___"
                    ) {
                        paraLines.add(lines[i])
                        charOffset += lines[i].length + 1
                        i++
                    }
                    val paraEnd = charOffset - 1
                    val paraText = paraLines.joinToString("\n").trim()
                    if (paraText.isNotBlank()) {
                        val inline = InlineParser.parse(paraText, paraStart)
                        val id = IdGenerator.newId()
                        blocks.add(
                            BlockNode.Paragraph(
                                id,
                                Range(paraStart, paraEnd),
                                emptyList(),
                                inline
                            )
                        )
                    }
                    continue
                }
            }
        }
        return blocks
    }

    private fun charOffsetForLine(lines: List<String>, lineIndex: Int): Int {
        var o = 0
        for (j in 0 until lineIndex) o += lines[j].length + 1
        return o
    }

    private fun parseList(
        source: String,
        lines: List<String>,
        start: Int,
        startCharOffset: Int,
        ordered: Boolean,
        taskList: Boolean,
    ): Pair<Int, List<Triple<String, Boolean?, Range>>> {
        val items = mutableListOf<Triple<String, Boolean?, Range>>()
        var i = start
        var charOffset = startCharOffset
        val regex =
            if (ordered) listItemOrderedRegex else if (taskList) taskListItemRegex else listItemUnorderedRegex
        while (i < lines.size) {
            val line = lines[i]
            val lineStart = charOffset
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                i++
                charOffset += line.length + 1
                break
            }
            val match = regex.find(trimmed) ?: break
            val prefixLen = line.indexOf(trimmed) + match.range.first + match.value.length
            val contentStart = lineStart + prefixLen
            val contentEnd = lineStart + line.length
            val (content, checked) = when {
                taskList -> {
                    val taskMatch = taskListItemRegex.find(trimmed)!!
                    taskMatch.groupValues[2].trim() to (taskMatch.groupValues[1].lowercase() == "x")
                }

                ordered -> match.groupValues[1].trim() to null
                else -> match.groupValues[1].trim() to null
            }
            items.add(Triple(content, checked, Range(contentStart, contentEnd)))
            charOffset = lineStart + line.length + 1
            i++
        }
        return i to items
    }

    private fun parseTable(
        source: String,
        lines: List<String>,
        start: Int,
        startCharOffset: Int,
    ): Triple<Int, List<Pair<String, Range>>, List<List<Pair<String, Range>>>> {
        fun splitCells(row: String): List<String> =
            row.trim().removeSurrounding("|").split("|").map { it.trim().replace("\\|", "|") }

        var i = start
        var charOffset = startCharOffset
        val headerRow = lines[i].trim()
        val headerCells = splitCells(headerRow)
        val headerRanges = mutableListOf<Pair<String, Range>>()
        var cellStart = charOffset + headerRow.indexOf(headerRow.trimStart())
        for (cell in headerCells) {
            val idx = headerRow.indexOf(cell, (cellStart - charOffset).coerceAtLeast(0))
            val s = charOffset + idx
            val e = s + cell.length
            headerRanges.add(cell to Range(s, e))
            cellStart = e + 1
        }
        charOffset += lines[i].length + 1
        i++
        val rows = mutableListOf<List<Pair<String, Range>>>()
        while (i < lines.size && tableRowRegex.matches(lines[i].trim())) {
            val row = lines[i].trim()
            if (row.contains("---")) {
                charOffset += lines[i].length + 1
                i++
                continue
            }
            val cells = splitCells(row)
            val cellRanges = mutableListOf<Pair<String, Range>>()
            var cs = charOffset + row.indexOf(row.trimStart())
            for (cell in cells) {
                val idx = row.indexOf(cell, (cs - charOffset).coerceAtLeast(0))
                val s = charOffset + idx
                val e = s + cell.length
                cellRanges.add(cell to Range(s, e))
                cs = e + 1
            }
            rows.add(cellRanges)
            charOffset += lines[i].length + 1
            i++
        }
        return Triple(i, headerRanges, rows)
    }
}
