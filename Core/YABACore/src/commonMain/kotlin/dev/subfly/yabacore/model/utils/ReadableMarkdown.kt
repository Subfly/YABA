package dev.subfly.yabacore.model.utils

/**
 * Readable .md files may start with YAML frontmatter for title and author.
 * When saving we write frontmatter so CacheRebuilder can restore title/author when rebuilding.
 */

private const val FRONTMATTER_START = "---"
private const val FRONTMATTER_END = "---"

/**
 * Wraps markdown body with optional YAML frontmatter (title, author).
 * If both are null, returns [markdown] unchanged.
 */
fun writeReadableMarkdownWithFrontmatter(
    markdown: String,
    title: String?,
    author: String?,
): String {
    if (title == null && author == null) return markdown
    val lines = buildList {
        add(FRONTMATTER_START)
        title?.let { add("title: ${yamlQuoted(it)}") }
        author?.let { add("author: ${yamlQuoted(it)}") }
        add(FRONTMATTER_END)
    }
    return lines.joinToString("\n") + "\n\n" + markdown
}

private fun yamlQuoted(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", " ")
    return "\"$escaped\""
}

/**
 * Parses frontmatter from readable .md content.
 * Returns (bodyWithoutFrontmatter, title, author).
 * If no frontmatter block is present, returns (content, null, null).
 */
fun parseReadableMarkdownFrontmatter(content: String): ReadableMarkdownParsed {
    val trimmed = content.trimStart()
    if (!trimmed.startsWith(FRONTMATTER_START)) {
        return ReadableMarkdownParsed(body = content, title = null, author = null)
    }
    val afterStart = trimmed.drop(FRONTMATTER_START.length)
    val firstNewline = afterStart.indexOf('\n')
    if (firstNewline < 0) {
        return ReadableMarkdownParsed(body = content, title = null, author = null)
    }
    val blockStart = firstNewline + 1
    val blockRest = afterStart.drop(blockStart)
    val endMarker = "\n$FRONTMATTER_END"
    val endIndex = blockRest.indexOf(endMarker)
    if (endIndex < 0) {
        return ReadableMarkdownParsed(body = content, title = null, author = null)
    }
    val block = blockRest.substring(0, endIndex)
    val body = blockRest.substring(endIndex + endMarker.length).trimStart()
    var title: String? = null
    var author: String? = null
    block.split('\n').forEach { line: String ->
        when {
            line.startsWith("title:") -> title = parseYamlQuoted(line.drop(6).trim())
            line.startsWith("author:") -> author = parseYamlQuoted(line.drop(7).trim())
        }
    }
    return ReadableMarkdownParsed(body = body, title = title, author = author)
}

private fun parseYamlQuoted(s: String): String? {
    if (s.isEmpty()) return null
    return when {
        s.startsWith("\"") && s.endsWith("\"") -> {
            s.drop(1).dropLast(1)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }
        else -> s
    }
}

data class ReadableMarkdownParsed(
    val body: String,
    val title: String?,
    val author: String?,
)
