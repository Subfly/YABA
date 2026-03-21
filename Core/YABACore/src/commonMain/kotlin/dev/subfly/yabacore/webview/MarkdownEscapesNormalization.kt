package dev.subfly.yabacore.webview

/**
 * Recovers markdown when JSON escape sequences were stored as **literal** two-character
 * sequences (`\` + `n`) instead of real newlines — e.g. legacy [decodeJsStringResult] bugs
 * or bad persistence from [WebView.evaluateJavascript] round-trips.
 *
 * Uses a heuristic so normal backslashes in user content (e.g. Windows paths) are not
 * rewritten unless the document clearly looks like escaped JSON text.
 */
fun normalizeMarkdownEscapesCorruption(markdown: String): String {
    if (markdown.length < 4) return markdown
    if (markdown.indexOf('\\') < 0) return markdown
    val realLf = markdown.count { it == '\n' }
    val literalSlashN = countBackslashThenChar(markdown, 'n')
    val likelyJsonEscapesLeaked =
        literalSlashN >= 2 && (literalSlashN > realLf + 1 || (realLf < 2 && literalSlashN >= 3))
    if (!likelyJsonEscapesLeaked) return markdown
    return markdown
        .replace("\\r\\n", "\r\n")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
}

private fun countBackslashThenChar(s: String, c: Char): Int {
    var count = 0
    var i = 0
    while (i < s.length - 1) {
        if (s[i] == '\\' && s[i + 1] == c) {
            count++
            i += 2
        } else {
            i++
        }
    }
    return count
}
