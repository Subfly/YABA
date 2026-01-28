package dev.subfly.yabacore.unfurl

/**
 * Minimal DOM representation for HTML parsing.
 *
 * This is an internal structure used for heuristic processing.
 * It captures only the elements and attributes needed for readable extraction.
 */
internal sealed interface HtmlNode {
    var parent: HtmlElement?
}

/**
 * An HTML element node with tag name, attributes, and children.
 */
internal class HtmlElement(
    val tagName: String,
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<HtmlNode> = mutableListOf(),
) : HtmlNode {
    override var parent: HtmlElement? = null

    /** Get attribute value (case-insensitive) */
    fun attr(name: String): String? = attributes[name.lowercase()]

    /** Get id attribute */
    val id: String? get() = attr("id")

    /** Get class attribute */
    val className: String? get() = attr("class")

    /** Get all text content recursively */
    fun textContent(): String = buildString {
        for (child in children) {
            when (child) {
                is HtmlText -> append(child.content)
                is HtmlElement -> append(child.textContent())
            }
        }
    }

    /** Get direct text length (excluding children elements) */
    fun directTextLength(): Int = children.filterIsInstance<HtmlText>().sumOf { it.content.length }

    /** Get total text length recursively */
    fun totalTextLength(): Int = textContent().length

    /** Count descendant nodes recursively */
    fun descendantCount(): Int {
        var count = 0
        for (child in children) {
            count++
            if (child is HtmlElement) {
                count += child.descendantCount()
            }
        }
        return count
    }

    /** Find all descendant elements with given tag name */
    fun getElementsByTagName(tag: String): List<HtmlElement> {
        val result = mutableListOf<HtmlElement>()
        collectElementsByTagName(tag.lowercase(), result)
        return result
    }

    private fun collectElementsByTagName(tag: String, result: MutableList<HtmlElement>) {
        for (child in children) {
            if (child is HtmlElement) {
                if (child.tagName == tag) {
                    result.add(child)
                }
                child.collectElementsByTagName(tag, result)
            }
        }
    }

    /** Find first element matching predicate */
    fun findFirst(predicate: (HtmlElement) -> Boolean): HtmlElement? {
        for (child in children) {
            if (child is HtmlElement) {
                if (predicate(child)) return child
                child.findFirst(predicate)?.let { return it }
            }
        }
        return null
    }

    /** Get all descendant elements */
    fun allDescendants(): List<HtmlElement> {
        val result = mutableListOf<HtmlElement>()
        collectAllDescendants(result)
        return result
    }

    private fun collectAllDescendants(result: MutableList<HtmlElement>) {
        for (child in children) {
            if (child is HtmlElement) {
                result.add(child)
                child.collectAllDescendants(result)
            }
        }
    }

    /** Check if element has a descendant with given tag */
    fun hasDescendant(tag: String): Boolean = getElementsByTagName(tag).isNotEmpty()

    /** Remove this element from its parent */
    fun remove() {
        parent?.children?.remove(this)
    }

    /** Get child elements (not text nodes) */
    fun childElements(): List<HtmlElement> = children.filterIsInstance<HtmlElement>()

    override fun toString(): String = "<$tagName>"
}

/**
 * A text node containing character content.
 */
internal class HtmlText(
    val content: String,
) : HtmlNode {
    override var parent: HtmlElement? = null
}

/**
 * Tolerant HTML parser that builds a minimal DOM tree.
 *
 * Features:
 * - Handles malformed HTML gracefully
 * - Captures relevant attributes for heuristics
 * - Ignores script, style, and comment content
 * - Self-closing tag support
 */
internal object HtmlParser {
    private val SELF_CLOSING_TAGS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr"
    )

    private val IGNORED_TAGS = setOf("script", "style", "noscript", "template")

    private val RELEVANT_ATTRIBUTES = setOf(
        "id", "class", "href", "src", "width", "height",
        "rel", "name", "property", "content", "alt", "title",
        "srcset", "data-src", "data-srcset", "data-original", "data-lazy-src", "data-url"
    )

    /**
     * Parse HTML string into a DOM tree.
     *
     * @param html Raw HTML string
     * @return Root element containing parsed content
     */
    fun parse(html: String): HtmlElement {
        val root = HtmlElement("root")
        var current = root
        var i = 0
        val len = html.length

        while (i < len) {
            val ch = html[i]

            if (ch == '<') {
                // Check for comment
                if (i + 3 < len && html.substring(i, i + 4) == "<!--") {
                    val commentEnd = html.indexOf("-->", i + 4)
                    i = if (commentEnd >= 0) commentEnd + 3 else len
                    continue
                }

                // Check for doctype
                if (i + 8 < len && html.substring(i, i + 9).equals("<!DOCTYPE", ignoreCase = true)) {
                    val doctypeEnd = html.indexOf('>', i)
                    i = if (doctypeEnd >= 0) doctypeEnd + 1 else len
                    continue
                }

                // Check for CDATA
                if (i + 8 < len && html.substring(i, i + 9) == "<![CDATA[") {
                    val cdataEnd = html.indexOf("]]>", i + 9)
                    i = if (cdataEnd >= 0) cdataEnd + 3 else len
                    continue
                }

                // Find tag end
                val tagEnd = findTagEnd(html, i + 1)
                if (tagEnd < 0) {
                    i++
                    continue
                }

                val tagContent = html.substring(i + 1, tagEnd).trim()
                if (tagContent.isEmpty()) {
                    i = tagEnd + 1
                    continue
                }

                // Closing tag
                if (tagContent.startsWith("/")) {
                    val closingTagName = tagContent.drop(1).trim().lowercase().split(Regex("\\s+")).firstOrNull() ?: ""
                    if (closingTagName.isNotEmpty()) {
                        // Pop stack until we find matching tag or hit root
                        var p: HtmlElement? = current
                        while (p != null && p.tagName != "root") {
                            if (p.tagName == closingTagName) {
                                current = p.parent ?: root
                                break
                            }
                            p = p.parent
                        }
                    }
                    i = tagEnd + 1
                    continue
                }

                // Parse opening tag
                val (tagName, attrs, selfClosing) = parseTag(tagContent)

                if (tagName in IGNORED_TAGS) {
                    // Skip entire content of ignored tags
                    val closeTag = "</$tagName>"
                    val closeIdx = html.indexOf(closeTag, tagEnd + 1, ignoreCase = true)
                    i = if (closeIdx >= 0) closeIdx + closeTag.length else tagEnd + 1
                    continue
                }

                val element = HtmlElement(tagName, attrs)
                element.parent = current
                current.children.add(element)

                if (!selfClosing && tagName !in SELF_CLOSING_TAGS) {
                    current = element
                }

                i = tagEnd + 1
            } else {
                // Text content
                val textEnd = html.indexOf('<', i)
                val text = if (textEnd >= 0) {
                    html.substring(i, textEnd)
                } else {
                    html.substring(i)
                }

                val decoded = decodeHtmlEntities(text)
                if (decoded.isNotBlank()) {
                    val textNode = HtmlText(decoded)
                    textNode.parent = current
                    current.children.add(textNode)
                }

                i = if (textEnd >= 0) textEnd else len
            }
        }

        return root
    }

    private fun findTagEnd(html: String, start: Int): Int {
        var i = start
        var inQuote: Char? = null
        while (i < html.length) {
            val ch = html[i]
            if (inQuote != null) {
                if (ch == inQuote) inQuote = null
            } else {
                when (ch) {
                    '"', '\'' -> inQuote = ch
                    '>' -> return i
                }
            }
            i++
        }
        return -1
    }

    private fun parseTag(content: String): Triple<String, MutableMap<String, String>, Boolean> {
        val attrs = mutableMapOf<String, String>()
        val selfClosing = content.endsWith("/")
        val cleanContent = if (selfClosing) content.dropLast(1).trim() else content

        // Extract tag name
        val firstSpace = cleanContent.indexOfFirst { it.isWhitespace() }
        val tagName = if (firstSpace > 0) {
            cleanContent.substring(0, firstSpace).lowercase()
        } else {
            cleanContent.lowercase()
        }

        if (firstSpace > 0) {
            val attrString = cleanContent.substring(firstSpace)
            parseAttributes(attrString, attrs)
        }

        return Triple(tagName, attrs, selfClosing)
    }

    private fun parseAttributes(attrString: String, attrs: MutableMap<String, String>) {
        // Simple attribute parsing with regex
        val attrRegex = Regex("""(\w[\w-]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|(\S+))""")
        val standaloneAttrRegex = Regex("""(\w[\w-]*)(?=\s|$)""")

        for (match in attrRegex.findAll(attrString)) {
            val name = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty {
                match.groupValues[3].ifEmpty { match.groupValues[4] }
            }
            if (name in RELEVANT_ATTRIBUTES) {
                attrs[name] = decodeHtmlEntities(value)
            }
        }

        // Handle standalone boolean attributes (like "disabled", "checked")
        for (match in standaloneAttrRegex.findAll(attrString)) {
            val name = match.groupValues[1].lowercase()
            if (name !in attrs && name in RELEVANT_ATTRIBUTES) {
                attrs[name] = name
            }
        }
    }

    private fun decodeHtmlEntities(text: String): String {
        var result = text
        val entities = mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
            "&nbsp;" to " ",
            "&ndash;" to "–",
            "&mdash;" to "—",
            "&lsquo;" to "'",
            "&rsquo;" to "'",
            "&ldquo;" to """,
            "&rdquo;" to """,
            "&hellip;" to "…",
            "&copy;" to "©",
            "&reg;" to "®",
            "&trade;" to "™",
        )
        entities.forEach { (entity, replacement) ->
            result = result.replace(entity, replacement, ignoreCase = true)
        }

        // Numeric entities
        result = result.replace(Regex("&#(\\d+);")) { match ->
            val code = match.groupValues[1].toIntOrNull()
            code?.toChar()?.toString() ?: match.value
        }
        result = result.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            val code = match.groupValues[1].toIntOrNull(16)
            code?.toChar()?.toString() ?: match.value
        }

        return result
    }
}
