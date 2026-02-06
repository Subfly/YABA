package dev.subfly.yabacore.markdown.codehighlight

/**
 * Specification for a programming language used by the code scanner.
 * Register via [CodeLanguages].
 */
data class CodeLanguageSpec(
    /** Canonical language id (e.g. "kotlin"). */
    val id: String,
    /** Aliases for the language (e.g. "kt", "kts"). */
    val aliases: Set<String>,
    /** Reserved keywords (case-sensitive or not depending on language). */
    val keywords: Set<String>,
    /** Line comment prefix (e.g. "//", "#"). Consumes until newline. */
    val lineCommentPrefix: String? = null,
    /** Block comment start (e.g. "/\*"). */
    val blockCommentStart: String? = null,
    /** Block comment end (e.g. "*\/"). */
    val blockCommentEnd: String? = null,
    /**
     * String delimiters: each pair is (opening, closing). Same string for both for simple " or '.
     * Escape character is assumed '\\' unless a language uses something else (handled in scanner).
     */
    val stringDelimiters: List<Pair<String, String>> = listOf(
        "\"" to "\"",
        "'" to "'",
    ),
    /** Whether keywords are case-sensitive (e.g. SQL often not). */
    val keywordsCaseSensitive: Boolean = true,
)
