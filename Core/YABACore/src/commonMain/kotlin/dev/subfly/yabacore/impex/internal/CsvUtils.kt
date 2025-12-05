package dev.subfly.yabacore.impex.internal

internal object CsvUtils {
    val expectedHeader: List<String> = listOf(
        "bookmarkId",
        "label",
        "bookmarkDescription",
        "link",
        "domain",
        "createdAt",
        "editedAt",
        "imageUrl",
        "iconUrl",
        "videoUrl",
        "type",
        "version",
    )

    fun parse(content: String): List<List<String>> =
        content
            .split("\n")
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .map { parseRow(it) }

    fun parseRow(row: String): List<String> {
        val result = mutableListOf<String>()
        val builder = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < row.length) {
            val ch = row[i]
            if (ch == '"') {
                val next = i + 1
                if (insideQuotes && next < row.length && row[next] == '"') {
                    builder.append('"')
                    i += 2
                } else {
                    insideQuotes = !insideQuotes
                    i += 1
                }
            } else if (ch == ',' && !insideQuotes) {
                result += builder.toString().trim()
                builder.clear()
                i += 1
            } else {
                builder.append(ch)
                i += 1
            }
        }
        result += builder.toString().trim()
        return result
    }

    fun escape(field: String): String {
        val needsQuotes = field.contains(",")
                || field.contains("\"")
                || field.contains("\n")
        var escaped = field.replace("\"", "\"\"")
        if (needsQuotes) escaped = "\"$escaped\""
        return escaped
    }
}
