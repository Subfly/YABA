package dev.subfly.yabacore.markdown.parser

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.markdown.ast.BlockNode
import dev.subfly.yabacore.markdown.ast.InlineNode
import dev.subfly.yabacore.markdown.ast.TableAlignment
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

                i + 1 < lines.size && line.isNotBlank() && lines[i + 1].trim().let { next ->
                    next.isNotEmpty() && next.all { it == '=' }
                } -> {
                    val (content, customId) = parseHeadingContent(line.trim())
                    val contentStart = lineStart + line.indexOf(line.trim())
                    val inline = InlineParser.parse(content, contentStart)
                    val id = IdGenerator.newId()
                    val underlineStart = charOffset + line.length + 1
                    val underlineLine = lines[i + 1]
                    val underlineEnd = underlineStart + underlineLine.length
                    blocks.add(
                        BlockNode.Heading(
                            id,
                            Range(lineStart, underlineEnd),
                            emptyList(),
                            1,
                            inline,
                            customId
                        )
                    )
                    charOffset = underlineEnd + 1
                    i += 2
                    continue
                }

                i + 1 < lines.size && line.isNotBlank() && lines[i + 1].trim().let { next ->
                    next.isNotEmpty() && next.all { it == '-' }
                } -> {
                    val (content, customId) = parseHeadingContent(line.trim())
                    val contentStart = lineStart + line.indexOf(line.trim())
                    val inline = InlineParser.parse(content, contentStart)
                    val id = IdGenerator.newId()
                    val underlineStart = charOffset + line.length + 1
                    val underlineLine = lines[i + 1]
                    val underlineEnd = underlineStart + underlineLine.length
                    blocks.add(
                        BlockNode.Heading(
                            id,
                            Range(lineStart, underlineEnd),
                            emptyList(),
                            2,
                            inline,
                            customId
                        )
                    )
                    charOffset = underlineEnd + 1
                    i += 2
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

                isIndentedCodeLine(line) -> {
                    val codeLines = mutableListOf<String>()
                    var codeCharOffset = charOffset
                    while (i < lines.size && isIndentedCodeLine(lines[i])) {
                        val stripped = stripIndentedCodeLine(lines[i])
                        codeLines.add(stripped)
                        codeCharOffset += lines[i].length + 1
                        i++
                    }
                    val codeEnd = codeCharOffset - 1
                    val codeText = codeLines.joinToString("\n")
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.CodeFence(
                            id,
                            Range(lineStart, codeEnd),
                            emptyList(),
                            null,
                            codeText
                        )
                    )
                    charOffset = codeCharOffset
                    continue
                }

                line.trimStart().startsWith(">") -> {
                    val (nextI, quoteEndOffset, paragraphContents) = parseBlockQuoteLines(lines, i, charOffset)
                    val firstQuoteLine = lines[i]
                    val contentStartOffset = lineStart +
                            firstQuoteLine.takeWhile { it == ' ' }.length +
                            1 +
                            (if (firstQuoteLine.trimStart().startsWith("> ")) 1 else 0)
                    var runningOffset = contentStartOffset
                    val paragraphBlocks = paragraphContents.map { content ->
                        val range = Range(runningOffset, runningOffset + content.length)
                        runningOffset += content.length + 2
                        BlockNode.Paragraph(
                            IdGenerator.newId(),
                            range,
                            emptyList(),
                            InlineParser.parse(content, range.start)
                        )
                    }
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.BlockQuote(
                            id,
                            Range(lineStart, quoteEndOffset),
                            paragraphBlocks
                        )
                    )
                    charOffset = quoteEndOffset + 1
                    i = nextI
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
                    val (hashes, rawContent) = headingRegex.find(trimmed)!!.destructured
                    val (content, customId) = parseHeadingContent(rawContent.trim())
                    val contentStart = lineStart + line.indexOf(trimmed) + hashes.length + 1
                    val inline = InlineParser.parse(content, contentStart)
                    val id = IdGenerator.newId()
                    blocks.add(
                        BlockNode.Heading(
                            id,
                            Range(lineStart, lineEnd),
                            emptyList(),
                            hashes.length,
                            inline,
                            customId
                        )
                    )
                    charOffset = lineEnd + 1
                    i++
                    continue
                }

                tableRowRegex.matches(line.trim()) -> {
                    val (nextI, headerCells, rows, alignments) = parseTable(source, lines, i, charOffset)
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
                            rowInlines,
                            alignments
                        )
                    )
                    i = nextI
                    charOffset = if (i < lines.size) charOffsetForLine(lines, i) else source.length
                    continue
                }

                i + 1 < lines.size && line.isNotBlank() && lines[i + 1].trimStart().startsWith(":") -> {
                    val (nextI, items) = parseDefinitionList(lines, i, charOffset)
                    val defItemBlocks = items.map { (termContent, termRange, defContents) ->
                        val termInline = InlineParser.parse(termContent, termRange.start)
                        val defInlines = defContents.map { (defContent, defRange) ->
                            InlineParser.parse(defContent, defRange.start)
                        }
                        BlockNode.DefinitionItem(
                            IdGenerator.newId(),
                            termRange,
                            emptyList(),
                            termInline,
                            defInlines
                        )
                    }
                    val listId = IdGenerator.newId()
                    val listRange = Range(
                        lineStart,
                        if (nextI < lines.size) charOffsetForLine(lines, nextI) - 1 else source.length
                    )
                    blocks.add(BlockNode.DefinitionList(listId, listRange, defItemBlocks))
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
                        !isIndentedCodeLine(lines[i]) &&
                        !listItemUnorderedRegex.matches(lines[i].trim()) &&
                        !listItemOrderedRegex.matches(lines[i].trim()) &&
                        !tableRowRegex.matches(lines[i].trim()) &&
                        lines[i].trim() != "---" && lines[i].trim() != "***" && lines[i].trim() != "___" &&
                        !lines[i].trim().let { t -> t.isNotEmpty() && t.all { c -> c == '=' } } &&
                        !lines[i].trim().let { t -> t.isNotEmpty() && t.all { c -> c == '-' } }
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
    ): Quad<Int, List<Pair<String, Range>>, List<List<Pair<String, Range>>>, List<TableAlignment>> {
        fun splitCells(row: String): List<String> =
            row.trim().removeSurrounding("|").split("|").map { it.trim().replace("\\|", "|") }

        fun parseAlignment(cell: String): TableAlignment {
            val t = cell.trim()
            return when {
                t.startsWith(":") && t.endsWith(":") -> TableAlignment.CENTER
                t.endsWith(":") -> TableAlignment.RIGHT
                t.startsWith(":") -> TableAlignment.LEFT
                else -> TableAlignment.LEFT
            }
        }

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
        var alignments = List(headerCells.size) { TableAlignment.LEFT }
        val rows = mutableListOf<List<Pair<String, Range>>>()
        while (i < lines.size && tableRowRegex.matches(lines[i].trim())) {
            val row = lines[i].trim()
            if (row.contains("---")) {
                val delimCells = splitCells(row)
                alignments = delimCells.map { parseAlignment(it) }
                if (alignments.size != headerCells.size) {
                    alignments = List(headerCells.size) { TableAlignment.LEFT }
                }
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
        return Quad(i, headerRanges, rows, alignments)
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /** If content ends with {#id}, returns (content without suffix, id); else (content, null). */
    private fun parseHeadingContent(content: String): Pair<String, String?> {
        val regex = Regex("\\s*\\{\\s*#([^}]+)\\s*\\}\\s*$")
        val match = regex.find(content) ?: return content to null
        val text = content.removeRange(match.range)
        return text.trimEnd() to match.groupValues[1].trim()
    }

    private fun isIndentedCodeLine(line: String): Boolean {
        if (line.isEmpty()) return false
        return line.startsWith("    ") || line[0] == '\t'
    }

    private fun stripIndentedCodeLine(line: String): String {
        return when {
            line.startsWith("    ") -> line.drop(4)
            line.isNotEmpty() && line[0] == '\t' -> line.drop(1)
            else -> line
        }
    }

    /**
     * Parses definition list starting at [start]. Returns (next line index, list of (termContent, termRange, list of (defContent, defRange))).
     */
    private fun parseDefinitionList(
        lines: List<String>,
        start: Int,
        startCharOffset: Int,
    ): Pair<Int, List<Triple<String, Range, List<Pair<String, Range>>>>> {
        val items = mutableListOf<Triple<String, Range, List<Pair<String, Range>>>>()
        var i = start
        var charOffset = startCharOffset
        while (i < lines.size) {
            val line = lines[i]
            val lineStart = charOffset
            val trimmed = line.trim()
            if (trimmed.isBlank()) {
                i++
                charOffset += line.length + 1
                break
            }
            val termContent = trimmed
            val termRange = Range(lineStart + line.indexOf(trimmed), lineStart + line.length)
            charOffset += line.length + 1
            i++
            val defContents = mutableListOf<Pair<String, Range>>()
            while (i < lines.size && lines[i].trimStart().startsWith(":")) {
                val defLine = lines[i]
                val defStart = charOffset + defLine.takeWhile { it == ' ' }.length
                val defContent = defLine.trimStart().removePrefix(":").trim()
                val defRange = Range(defStart, defStart + defContent.length)
                defContents.add(defContent to defRange)
                charOffset += defLine.length + 1
                i++
            }
            items.add(Triple(termContent, termRange, defContents))
        }
        return i to items
    }

    /**
     * Consumes ">" lines and returns (next line index, quote end char offset, list of paragraph contents).
     * Blank ">" or "> " lines start a new paragraph.
     */
    private fun parseBlockQuoteLines(
        lines: List<String>,
        start: Int,
        startCharOffset: Int,
    ): Triple<Int, Int, List<String>> {
        val paragraphs = mutableListOf<String>()
        var current = mutableListOf<String>()
        var i = start
        var charOffset = startCharOffset
        while (i < lines.size && lines[i].trimStart().startsWith(">")) {
            val content = lines[i].trimStart().removePrefix(">").trim()
            if (content.isEmpty()) {
                if (current.isNotEmpty()) {
                    paragraphs.add(current.joinToString("\n"))
                    current = mutableListOf()
                }
            } else {
                current.add(content)
            }
            charOffset += lines[i].length + 1
            i++
        }
        if (current.isNotEmpty()) paragraphs.add(current.joinToString("\n"))
        val quoteEnd = if (i > start) charOffset - 1 else startCharOffset
        return Triple(i, quoteEnd, paragraphs)
    }
}
