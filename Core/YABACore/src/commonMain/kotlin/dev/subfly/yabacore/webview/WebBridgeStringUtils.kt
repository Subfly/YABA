package dev.subfly.yabacore.webview

/**
 * Normalizes optional strings from the WebView/JSON bridge: blank strings and the literal words
 * `"null"` / `"undefined"` (any case) become Kotlin null.
 */
fun String?.normalizeBridgeOptionalString(): String? {
    val t = this?.trim() ?: return null
    if (t.isEmpty()) return null
    val lower = t.lowercase()
    if (lower == "null" || lower == "undefined") return null
    return t
}
