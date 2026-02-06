package dev.subfly.yabacore.markdown.codehighlight

/**
 * Registry of language id/alias -> [CodeLanguageSpec].
 * Extensible: add new specs and register with [register].
 */
object CodeLanguages {

    private val byId = mutableMapOf<String, CodeLanguageSpec>()
    private val byAlias = mutableMapOf<String, CodeLanguageSpec>()

    init {
        listOf(
            kotlinSpec(),
            javaSpec(),
            swiftSpec(),
            javascriptSpec(),
            typescriptSpec(),
            pythonSpec(),
            goSpec(),
            rustSpec(),
            cSpec(),
            cppSpec(),
            csharpSpec(),
            rubySpec(),
            phpSpec(),
            sqlSpec(),
            bashSpec(),
        ).forEach { register(it) }
    }

    fun register(spec: CodeLanguageSpec) {
        byId[spec.id] = spec
        spec.aliases.forEach { byAlias[it] = spec }
    }

    fun resolve(languageId: String?): CodeLanguageSpec? {
        if (languageId.isNullOrBlank()) return null
        val normalized = languageId.trim().lowercase()
        return byId[normalized] ?: byAlias[normalized]
    }

    private fun kotlinSpec() = CodeLanguageSpec(
        id = "kotlin",
        aliases = setOf("kt", "kts"),
        keywords = setOf(
            "as", "as?", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "!in", "interface", "is", "!is", "null", "object", "package", "return",
            "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun javaSpec() = CodeLanguageSpec(
        id = "java",
        aliases = setOf("javac"),
        keywords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun swiftSpec() = CodeLanguageSpec(
        id = "swift",
        aliases = setOf("sw"),
        keywords = setOf(
            "associatedtype", "class", "deinit", "enum", "extension", "fileprivate", "func",
            "import", "init", "inout", "let", "open", "operator", "private", "protocol", "public",
            "rethrows", "static", "struct", "subscript", "typealias", "var", "break", "case",
            "continue", "default", "defer", "do", "else", "fallthrough", "for", "guard", "if",
            "in", "repeat", "return", "switch", "where", "while", "as", "catch", "false", "is",
            "nil", "super", "self", "throw", "true", "try", "#available", "#colorLiteral", "#column",
            "#else", "#elseif", "#endif", "#file", "#fileLiteral", "#function", "#if", "#imageLiteral",
            "#line", "#selector", "#sourceLocation", "#warning", "Any", "Self",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun javascriptSpec() = CodeLanguageSpec(
        id = "javascript",
        aliases = setOf("js", "jsx", "mjs", "cjs"),
        keywords = setOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue", "debugger",
            "default", "delete", "do", "else", "export", "extends", "finally", "for", "function",
            "if", "import", "in", "instanceof", "let", "new", "return", "super", "switch",
            "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield", "true", "false", "null",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun typescriptSpec() = CodeLanguageSpec(
        id = "typescript",
        aliases = setOf("ts", "tsx"),
        keywords = setOf(
            "abstract", "any", "as", "async", "await", "boolean", "break", "case", "catch",
            "class", "const", "constructor", "continue", "debugger", "declare", "default",
            "delete", "do", "else", "enum", "export", "extends", "false", "finally", "for",
            "from", "function", "if", "implements", "import", "in", "infer", "instanceof", "interface",
            "is", "keyof", "let", "module", "namespace", "never", "new", "null", "number",
            "object", "of", "package", "private", "protected", "public", "readonly", "require",
            "return", "set", "string", "super", "switch", "symbol", "this", "throw", "true",
            "try", "type", "typeof", "undefined", "unique", "unknown", "var", "void", "while",
            "with", "yield",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun pythonSpec() = CodeLanguageSpec(
        id = "python",
        aliases = setOf("py", "py3"),
        keywords = setOf(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally", "for", "from", "global",
            "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise",
            "return", "try", "while", "with", "yield",
        ),
        lineCommentPrefix = "#",
        blockCommentStart = null,
        blockCommentEnd = null,
        stringDelimiters = listOf(
            "\"" to "\"",
            "'" to "'",
            "\"\"\"" to "\"\"\"",
            "'''" to "'''",
        ),
    )

    private fun goSpec() = CodeLanguageSpec(
        id = "go",
        aliases = setOf("golang"),
        keywords = setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough",
            "for", "func", "go", "goto", "if", "import", "interface", "map", "package", "range",
            "return", "select", "struct", "switch", "type", "var", "nil", "true", "false",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun rustSpec() = CodeLanguageSpec(
        id = "rust",
        aliases = setOf("rs"),
        keywords = setOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn", "else",
            "enum", "extern", "false", "fn", "for", "if", "impl", "in", "let", "loop",
            "match", "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static",
            "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while",
            "async", "dyn", "abstract", "become", "box", "do", "final", "macro", "override",
            "priv", "try", "typeof", "unsized", "virtual", "yield",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun cSpec() = CodeLanguageSpec(
        id = "c",
        aliases = setOf("h"),
        keywords = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "int", "long", "register",
            "return", "short", "signed", "sizeof", "static", "struct", "switch", "typedef",
            "union", "unsigned", "void", "volatile", "while", "true", "false", "NULL",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun cppSpec() = CodeLanguageSpec(
        id = "cpp",
        aliases = setOf("c++", "cc", "cxx", "hpp", "h++"),
        keywords = setOf(
            "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor", "bool",
            "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t", "class", "compl",
            "concept", "const", "consteval", "constexpr", "const_cast", "continue", "co_await",
            "co_return", "co_yield", "decltype", "default", "delete", "do", "double", "dynamic_cast",
            "else", "enum", "explicit", "export", "extern", "false", "float", "for", "friend",
            "goto", "if", "inline", "int", "long", "mutable", "namespace", "new", "noexcept",
            "not", "not_eq", "nullptr", "operator", "or", "or_eq", "private", "protected", "public",
            "register", "reinterpret_cast", "requires", "return", "short", "signed", "sizeof",
            "static", "static_assert", "static_cast", "struct", "switch", "template", "this",
            "thread_local", "throw", "true", "try", "typedef", "typeid", "typename", "union",
            "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while", "xor", "xor_eq",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun csharpSpec() = CodeLanguageSpec(
        id = "csharp",
        aliases = setOf("cs", "c#"),
        keywords = setOf(
            "abstract", "as", "base", "bool", "break", "byte", "case", "catch", "char",
            "checked", "class", "const", "continue", "decimal", "default", "delegate", "do",
            "double", "else", "enum", "event", "explicit", "extern", "false", "finally", "fixed",
            "float", "for", "foreach", "goto", "if", "implicit", "in", "int", "interface",
            "internal", "is", "lock", "long", "namespace", "new", "null", "object", "operator",
            "out", "override", "params", "private", "protected", "public", "readonly", "ref",
            "return", "sbyte", "sealed", "short", "sizeof", "stackalloc", "static", "string",
            "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", "ulong",
            "unchecked", "unsafe", "ushort", "using", "virtual", "void", "volatile", "while",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
    )

    private fun rubySpec() = CodeLanguageSpec(
        id = "ruby",
        aliases = setOf("rb", "gemfile"),
        keywords = setOf(
            "BEGIN", "END", "alias", "and", "begin", "break", "case", "class", "def", "defined?",
            "do", "else", "elsif", "end", "ensure", "false", "for", "if", "in", "module",
            "next", "nil", "not", "or", "redo", "rescue", "retry", "return", "self", "super",
            "then", "true", "undef", "unless", "until", "when", "while", "yield",
        ),
        lineCommentPrefix = "#",
        blockCommentStart = null,
        blockCommentEnd = null,
    )

    private fun phpSpec() = CodeLanguageSpec(
        id = "php",
        aliases = setOf("php3", "php4", "php5", "phtml"),
        keywords = setOf(
            "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class",
            "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else",
            "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch",
            "endwhile", "eval", "exit", "extends", "final", "finally", "for", "foreach", "function",
            "global", "goto", "if", "implements", "include", "include_once", "instanceof",
            "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print",
            "private", "protected", "public", "require", "require_once", "return", "static",
            "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor", "yield",
            "true", "false", "null",
        ),
        lineCommentPrefix = "//",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
        stringDelimiters = listOf("\"" to "\"", "'" to "'"),
    )

    private fun sqlSpec() = CodeLanguageSpec(
        id = "sql",
        aliases = setOf("mysql", "pgsql", "sqlite"),
        keywords = setOf(
            "add", "all", "alter", "and", "any", "as", "asc", "backup", "between", "by",
            "case", "check", "column", "constraint", "create", "database", "default", "delete",
            "desc", "distinct", "drop", "exec", "exists", "foreign", "from", "full", "group",
            "having", "in", "index", "inner", "insert", "into", "is", "join", "key", "left",
            "like", "limit", "not", "null", "or", "order", "outer", "primary", "procedure",
            "replace", "right", "rownum", "select", "set", "table", "top", "truncate",
            "union", "unique", "update", "values", "view", "where",
        ),
        lineCommentPrefix = "--",
        blockCommentStart = "/*",
        blockCommentEnd = "*/",
        keywordsCaseSensitive = false,
    )

    private fun bashSpec() = CodeLanguageSpec(
        id = "bash",
        aliases = setOf("sh", "zsh", "shell", "ksh"),
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while", "until",
            "do", "done", "in", "function", "select", "time", "coproc", "[[", "]]",
            "break", "continue", "return", "exit", "local", "declare", "readonly", "export",
            "true", "false", "echo", "printf", "read", "eval", "exec", "shift", "set", "unset",
        ),
        lineCommentPrefix = "#",
        blockCommentStart = null,
        blockCommentEnd = null,
        stringDelimiters = listOf("\"" to "\"", "'" to "'"),
    )
}
