package dev.subfly.yaba.core.webview

/**
 * Escapes a string so it is safe to embed in a JavaScript **single-quoted** literal.
 */
fun escapeForJsSingleQuotedString(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")
