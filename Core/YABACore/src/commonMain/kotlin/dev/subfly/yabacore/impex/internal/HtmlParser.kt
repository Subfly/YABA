package dev.subfly.yabacore.impex.internal

import kotlin.math.max

internal object HtmlParser {
    data class BookmarkNode(val title: String, val url: String)
    data class FolderNode(
        val label: String,
        val bookmarks: List<BookmarkNode>,
        val children: List<FolderNode>,
    )

    data class ParsedRoot(
        val folders: List<FolderNode>,
        val rootBookmarks: List<BookmarkNode>,
    )

    fun parse(html: String): ParsedRoot {
        val normalized = normalize(html)
        val firstDl = normalized.indexOf("<DL>", ignoreCase = true)
        if (firstDl != -1) {
            val range = findBalancedRange("DL", normalized, firstDl)
            if (range != null) {
                val inner =
                    normalized.substring(range.first + 4, max(range.second - 5, range.first))
                val parsed = parseList(inner, 0)
                return ParsedRoot(parsed.folders, parsed.bookmarks)
            }
        }
        val parsed = parseList(normalized, 0)
        return ParsedRoot(parsed.folders, parsed.bookmarks)
    }

    private data class ParseResult(
        val folders: List<FolderNode>,
        val bookmarks: List<BookmarkNode>,
        val nextIndex: Int,
    )

    private fun parseList(html: String, start: Int): ParseResult {
        val folders = mutableListOf<FolderNode>()
        val bookmarks = mutableListOf<BookmarkNode>()
        var index = start

        while (index < html.length) {
            val dtPos = html.indexOf("<DT>", index, ignoreCase = true)
            if (dtPos == -1) break
            index = dtPos + 4
            index = skipWhitespace(html, index)

            if (html.regionMatches(index, "<H3", 0, 3, ignoreCase = true)) {
                val h3CloseStart = html.indexOf('>', index)
                if (h3CloseStart == -1) break
                val h3End = html.indexOf("</H3>", h3CloseStart + 1, ignoreCase = true)
                if (h3End == -1) break

                val rawLabel = html.substring(h3CloseStart + 1, h3End)
                val label = decodeEntities(rawLabel.trim())

                var afterFolderIndex = h3End + 5
                afterFolderIndex = skipWhitespace(html, afterFolderIndex)

                var childFolders: List<FolderNode> = emptyList()
                var childBookmarks: List<BookmarkNode> = emptyList()
                val dlStart = html.indexOf("<DL>", afterFolderIndex, ignoreCase = true)
                if (dlStart != -1) {
                    val balanced = findBalancedRange("DL", html, dlStart)
                    if (balanced != null) {
                        val inner = html.substring(balanced.first + 4, balanced.second - 5)
                        val parsed = parseList(inner, 0)
                        childFolders = parsed.folders
                        childBookmarks = parsed.bookmarks
                        afterFolderIndex = balanced.second
                    }
                }

                folders += FolderNode(label, childBookmarks, childFolders)
                index = afterFolderIndex
                continue
            }

            if (html.regionMatches(index, "<A ", 0, 3, ignoreCase = true)) {
                val href = findAttribute("HREF", html, index + 3)
                val aEnd = html.indexOf('>', index + 3)
                if (href == null || aEnd == -1) {
                    index++
                    continue
                }
                val close = html.indexOf("</A>", aEnd + 1, ignoreCase = true)
                if (close == -1) {
                    index++
                    continue
                }
                val url = html.substring(href.first, href.second).trim()
                val titleRaw = html.substring(aEnd + 1, close).trim()
                val title = decodeEntities(titleRaw)
                if (url.isNotEmpty()) {
                    bookmarks += BookmarkNode(title, url)
                }
                index = close + 4
                continue
            }

            index += 1
        }

        return ParseResult(folders, bookmarks, index)
    }

    private fun findBalancedRange(tag: String, html: String, start: Int): Pair<Int, Int>? {
        val open = "<$tag>"
        val close = "</$tag>"
        var depth = 0
        var idx = start
        var startIdx: Int? = null
        while (idx < html.length) {
            val nextOpen = html.indexOf(open, idx, ignoreCase = true)
            val nextClose = html.indexOf(close, idx, ignoreCase = true)
            if (nextOpen != -1 && (nextOpen < nextClose || nextClose == -1)) {
                if (startIdx == null) startIdx = nextOpen
                depth += 1
                idx = nextOpen + open.length
                continue
            }
            if (nextClose != -1) {
                depth -= 1
                idx = nextClose + close.length
                if (depth == 0 && startIdx != null) {
                    return startIdx to idx
                }
                continue
            }
            break
        }
        return null
    }

    private fun findAttribute(name: String, html: String, start: Int): Pair<Int, Int>? {
        val key = "$name=\""
        val nameIndex = html.indexOf(key, start, ignoreCase = true)
        if (nameIndex == -1) return null
        val valueStart = nameIndex + key.length
        val endQuote = html.indexOf('"', valueStart)
        if (endQuote == -1) return null
        return valueStart to endQuote
    }

    private fun normalize(html: String): String =
        html
            .replace("<dt>", "<DT>", ignoreCase = true)
            .replace("<DT >", "<DT>", ignoreCase = true)
            .replace("<h3", "<H3", ignoreCase = true)
            .replace("</h3>", "</H3>", ignoreCase = true)
            .replace("<a ", "<A ", ignoreCase = true)
            .replace("href=", "HREF=", ignoreCase = true)
            .replace("</a>", "</A>", ignoreCase = true)
            .replace("<dl>", "<DL>", ignoreCase = true)
            .replace("</dl>", "</DL>", ignoreCase = true)
            .replace("<dd>", "", ignoreCase = true)
            .replace("</dd>", "", ignoreCase = true)
            .replace("<p>", "", ignoreCase = true)
            .replace("</p>", "", ignoreCase = true)

    private fun decodeEntities(text: String): String =
        text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")

    private fun skipWhitespace(html: String, start: Int): Int {
        var idx = start
        while (idx < html.length && html[idx].isWhitespace()) idx++
        return idx
    }
}
