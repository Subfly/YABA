package dev.subfly.yabacore.markdown.codehighlight

/**
 * Single-pass scanner that tokenizes source code according to a [CodeLanguageSpec].
 * Produces [CodeSpan]s with half-open [start, end) offsets.
 */
object CodeScanner {

    private const val ESCAPE_CHAR = '\\'

    fun scan(spec: CodeLanguageSpec, code: String): List<CodeSpan> {
        if (code.isEmpty()) return emptyList()
        val spans = mutableListOf<CodeSpan>()
        val keywordsSorted = spec.keywords.sortedByDescending { it.length }
        var i = 0
        while (i < code.length) {
            val start = i
            when {
                spec.lineCommentPrefix != null && code.startsWith(spec.lineCommentPrefix, i) -> {
                    i = code.indexOf('\n', i).let { if (it == -1) code.length else it + 1 }
                    spans.add(CodeSpan(CodeTokenType.Comment, start, i))
                }
                spec.blockCommentStart != null && spec.blockCommentEnd != null &&
                    code.startsWith(spec.blockCommentStart, i) -> {
                    val endMark = spec.blockCommentEnd
                    var j = i + spec.blockCommentStart.length
                    while (j <= code.length - endMark.length) {
                        if (code.startsWith(endMark, j)) {
                            j += endMark.length
                            break
                        }
                        j++
                    }
                    i = j
                    spans.add(CodeSpan(CodeTokenType.Comment, start, i))
                }
                matchString(spec, code, i) != null -> {
                    val end = matchString(spec, code, i)!!
                    i = end
                    spans.add(CodeSpan(CodeTokenType.String, start, i))
                }
                code[i].isDigit() || (code[i] == '.' && i + 1 < code.length && code[i + 1].isDigit()) -> {
                    i = consumeNumber(code, i)
                    spans.add(CodeSpan(CodeTokenType.Number, start, i))
                }
                isIdentifierStart(code[i]) -> {
                    val word = consumeIdentifier(code, i)
                    val keyword = if (spec.keywordsCaseSensitive) {
                        keywordsSorted.firstOrNull { word == it }
                    } else {
                        keywordsSorted.firstOrNull { word.equals(it, ignoreCase = true) }
                    }
                    i = start + word.length
                    spans.add(
                        CodeSpan(
                            if (keyword != null) CodeTokenType.Keyword else CodeTokenType.Identifier,
                            start,
                            i,
                        ),
                    )
                }
                code[i].isWhitespace() -> {
                    while (i < code.length && code[i].isWhitespace()) i++
                    spans.add(CodeSpan(CodeTokenType.Whitespace, start, i))
                }
                else -> {
                    i++
                    spans.add(CodeSpan(CodeTokenType.Operator, start, i))
                }
            }
        }
        return spans
    }

    private fun matchString(spec: CodeLanguageSpec, code: String, start: Int): Int? {
        for ((open, close) in spec.stringDelimiters) {
            if (start + open.length > code.length) continue
            if (!code.startsWith(open, start)) continue
            var i = start + open.length
            while (i <= code.length - close.length) {
                if (code[i] == ESCAPE_CHAR && i + 1 < code.length) {
                    i += 2
                    continue
                }
                if (code.startsWith(close, i)) return i + close.length
                i++
            }
            return code.length
        }
        return null
    }

    private fun consumeNumber(code: String, start: Int): Int {
        var i = start
        if (i < code.length && code[i] == '.') i++
        while (i < code.length && code[i].isDigit()) i++
        if (i < code.length && code[i] == '.') {
            i++
            while (i < code.length && code[i].isDigit()) i++
        }
        if (i < code.length && (code[i] == 'e' || code[i] == 'E')) {
            i++
            if (i < code.length && (code[i] == '+' || code[i] == '-')) i++
            while (i < code.length && code[i].isDigit()) i++
        }
        return i
    }

    private fun isIdentifierStart(c: Char): Boolean =
        c.isLetter() || c == '_'

    private fun isIdentifierPart(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_'

    private fun consumeIdentifier(code: String, start: Int): String {
        var i = start
        while (i < code.length && isIdentifierPart(code[i])) i++
        return code.substring(start, i)
    }
}
