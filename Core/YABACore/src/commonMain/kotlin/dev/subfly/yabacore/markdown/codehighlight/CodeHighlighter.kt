package dev.subfly.yabacore.markdown.codehighlight

/**
 * Highlights code by tokenizing with a language spec and producing [CodeSpan]s.
 * Use [CodeHighlighter.default] for the built-in registry and scanner.
 */
interface CodeHighlighter {
    fun highlight(languageId: String?, code: String): List<CodeSpan>
}

/**
 * Default highlighter: resolves language via [CodeLanguages], tokenizes with [CodeScanner].
 * Unknown or null language returns a single span (whole code as default/plain).
 */
object DefaultCodeHighlighter : CodeHighlighter {
    override fun highlight(languageId: String?, code: String): List<CodeSpan> {
        if (code.isEmpty()) return emptyList()
        val spec = CodeLanguages.resolve(languageId) ?: return listOf(
            CodeSpan(CodeTokenType.Identifier, 0, code.length),
        )
        return CodeScanner.scan(spec, code)
    }
}
