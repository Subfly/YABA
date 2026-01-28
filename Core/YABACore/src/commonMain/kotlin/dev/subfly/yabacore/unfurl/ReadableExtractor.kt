package dev.subfly.yabacore.unfurl

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.unfurl.AssetToDownload
import dev.subfly.yabacore.unfurl.MarkdownExtractionResult
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom

/**
 * Extracts readable content from HTML using deterministic heuristics.
 *
 * The extraction pipeline:
 * 1. Parse HTML into minimal DOM
 * 2. Normalize images (lazy-load, srcset, etc.)
 * 3. Pruning (structural, link density, text density, image cluster)
 * 4. Article root selection
 * 5. DOM → Markdown (best-effort)
 * 6. Return markdown + assets to download
 */
internal object ReadableExtractor {
    // Tags to unconditionally remove
    private val PRUNE_TAGS = setOf(
        "nav", "aside", "footer", "header", "form", "iframe", "canvas", "svg"
    )

    // Class/ID keywords that trigger subtree removal (case-insensitive)
    private val PRUNE_KEYWORDS = setOf(
        "nav", "menu", "footer", "header", "sidebar", "comment",
        "related", "share", "promo", "ads", "advertisement", "social",
        "newsletter", "subscribe", "popup", "modal", "cookie"
    )

    // Block-level tags
    private val BLOCK_TAGS = setOf(
        "p", "div", "section", "article", "main", "figure",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "pre", "blockquote", "ul", "ol", "li",
        "table", "tr", "td", "th", "tbody", "thead", "tfoot",
        "dl", "dt", "dd", "address", "hr", "figcaption"
    )

    // Text density threshold
    private const val TEXT_DENSITY_THRESHOLD = 6

    // Link density thresholds
    private const val MIN_TEXT_FOR_LINK_DENSITY = 80
    private const val MAX_LINK_DENSITY = 0.4

    /**
     * Extract readable content from HTML.
     *
     * @param html Raw HTML string
     * @param baseUrl Base URL for resolving relative links/images
     * @param metaTitle Title from metadata (fallback)
     * @param metaAuthor Author from metadata (fallback)
     * @return Markdown extraction result (markdown string, title, author, assets to download)
     */
    fun extract(
        html: String,
        baseUrl: Url,
        metaTitle: String? = null,
        metaAuthor: String? = null,
    ): MarkdownExtractionResult {
        // 1. Parse HTML
        val root = HtmlParser.parse(html)

        // 2. Normalize images (resolve lazy-load, srcset, etc.) before pruning
        normalizeImages(root, baseUrl)

        // 3. Extract author before pruning
        val author = extractAuthor(root, html) ?: metaAuthor

        // 4. Hard structural pruning
        applyHardPruning(root)

        // 5. Link density pruning
        applyLinkDensityPruning(root)

        // 6. Text density pruning
        applyTextDensityPruning(root)

        // 7. Image cluster pruning
        applyImageClusterPruning(root)

        // 8. Article root selection
        val articleRoot = selectArticleRoot(root) ?: root

        // 9. Extract title
        val title = extractTitle(articleRoot) ?: metaTitle

        // 10. DOM → Markdown
        val (rawMarkdown, assetsToDownload) = domToMarkdown(articleRoot, baseUrl)
        val markdown = postProcessMarkdown(rawMarkdown)

        return MarkdownExtractionResult(
            markdown = markdown,
            title = title,
            author = author,
            assetsToDownload = assetsToDownload,
        )
    }

    // ==================== Pruning Passes ====================

    private fun applyHardPruning(element: HtmlElement) {
        val toRemove = mutableListOf<HtmlElement>()

        for (child in element.children) {
            if (child is HtmlElement) {
                // Check tag name
                if (child.tagName in PRUNE_TAGS) {
                    toRemove.add(child)
                    continue
                }

                // Check class/id for keywords
                val classId = "${child.className ?: ""} ${child.id ?: ""}".lowercase()
                if (PRUNE_KEYWORDS.any { classId.contains(it) }) {
                    toRemove.add(child)
                    continue
                }

                // Recurse
                applyHardPruning(child)
            }
        }

        toRemove.forEach { it.remove() }
    }

    private fun applyLinkDensityPruning(element: HtmlElement) {
        val toRemove = mutableListOf<HtmlElement>()

        for (child in element.children) {
            if (child is HtmlElement) {
                if (child.tagName in BLOCK_TAGS) {
                    val totalText = child.totalTextLength()
                    if (totalText < MIN_TEXT_FOR_LINK_DENSITY) {
                        val linkText = child.getElementsByTagName("a").sumOf { it.totalTextLength() }
                        val linkDensity = if (totalText > 0) linkText.toDouble() / totalText else 0.0
                        if (linkDensity > MAX_LINK_DENSITY) {
                            toRemove.add(child)
                            continue
                        }
                    }
                }
                applyLinkDensityPruning(child)
            }
        }

        toRemove.forEach { it.remove() }
    }

    private fun applyTextDensityPruning(element: HtmlElement) {
        val toRemove = mutableListOf<HtmlElement>()

        for (child in element.children) {
            if (child is HtmlElement) {
                val descendantCount = child.descendantCount()
                if (descendantCount > 0) {
                    val textDensity = child.totalTextLength().toDouble() / descendantCount
                    if (textDensity < TEXT_DENSITY_THRESHOLD && child.tagName in BLOCK_TAGS) {
                        // Check if it's not a semantic container
                        if (child.tagName !in setOf("article", "main", "section", "blockquote", "figure")) {
                            toRemove.add(child)
                            continue
                        }
                    }
                }
                applyTextDensityPruning(child)
            }
        }

        toRemove.forEach { it.remove() }
    }

    private fun applyImageClusterPruning(element: HtmlElement) {
        val toRemove = mutableListOf<HtmlElement>()

        for (child in element.children) {
            if (child is HtmlElement) {
                val imageCount = child.getElementsByTagName("img").size
                val textBlocks = countTextBlocks(child)

                if (imageCount > textBlocks * 2 &&
                    !child.hasDescendant("figcaption") &&
                    !hasAdjacentHeading(child)
                ) {
                    toRemove.add(child)
                    continue
                }

                applyImageClusterPruning(child)
            }
        }

        toRemove.forEach { it.remove() }
    }

    private fun countTextBlocks(element: HtmlElement): Int {
        var count = 0
        for (child in element.children) {
            when (child) {
                is HtmlText -> if (child.content.trim().length > 20) count++
                is HtmlElement -> {
                    if (child.tagName == "p" && child.totalTextLength() > 20) count++
                    count += countTextBlocks(child)
                }
            }
        }
        return count
    }

    private fun hasAdjacentHeading(element: HtmlElement): Boolean {
        val parent = element.parent ?: return false
        val index = parent.children.indexOf(element)

        // Check previous sibling
        if (index > 0) {
            val prev = parent.children[index - 1]
            if (prev is HtmlElement && prev.tagName.matches(Regex("h[1-6]"))) return true
        }

        // Check next sibling
        if (index < parent.children.size - 1) {
            val next = parent.children[index + 1]
            if (next is HtmlElement && next.tagName.matches(Regex("h[1-6]"))) return true
        }

        return false
    }

    // ==================== Article Root Selection ====================

    private fun selectArticleRoot(root: HtmlElement): HtmlElement? {
        var bestCandidate: HtmlElement? = null
        var bestScore = 0

        fun scoreElement(element: HtmlElement): Int {
            val textLength = element.totalTextLength()
            val headingCount = (1..6).sumOf { element.getElementsByTagName("h$it").size }
            val codeBlockCount = element.getElementsByTagName("pre").size

            return textLength + (headingCount * 100) + (codeBlockCount * 80)
        }

        // Look for semantic containers first
        val semanticContainers = listOf("article", "main")
        for (tag in semanticContainers) {
            val candidates = root.getElementsByTagName(tag)
            for (candidate in candidates) {
                val score = scoreElement(candidate)
                if (score > bestScore) {
                    bestScore = score
                    bestCandidate = candidate
                }
            }
        }

        // If no semantic container, find best scoring div/section
        if (bestCandidate == null) {
            val containers = root.getElementsByTagName("div") + root.getElementsByTagName("section")
            for (candidate in containers) {
                val score = scoreElement(candidate)
                if (score > bestScore) {
                    bestScore = score
                    bestCandidate = candidate
                }
            }
        }

        // Fall back to body if exists
        if (bestCandidate == null) {
            bestCandidate = root.getElementsByTagName("body").firstOrNull()
        }

        return bestCandidate
    }

    // ==================== Author Extraction ====================

    private fun extractAuthor(root: HtmlElement, html: String): String? {
        // 1. Meta tags
        val metaAuthorPatterns = listOf(
            """<meta\s+name\s*=\s*["']author["'][^>]*content\s*=\s*["']([^"']+)["']""",
            """<meta\s+content\s*=\s*["']([^"']+)["'][^>]*name\s*=\s*["']author["']""",
            """<meta\s+property\s*=\s*["']article:author["'][^>]*content\s*=\s*["']([^"']+)["']""",
        )

        for (pattern in metaAuthorPatterns) {
            val match = Regex(pattern, RegexOption.IGNORE_CASE).find(html)
            if (match != null) {
                val author = match.groupValues[1].trim()
                if (isValidAuthor(author)) return author
            }
        }

        // 2. rel="author" link
        val authorLinks = root.allDescendants().filter {
            it.tagName == "a" && it.attr("rel")?.contains("author", ignoreCase = true) == true
        }
        for (link in authorLinks) {
            val author = link.textContent().trim()
            if (isValidAuthor(author)) return author
        }

        // 3. Class/ID containing author keywords
        val authorKeywords = setOf("author", "byline", "written-by", "writer")
        val candidates = root.allDescendants().filter { element ->
            val classId = "${element.className ?: ""} ${element.id ?: ""}".lowercase()
            authorKeywords.any { classId.contains(it) }
        }

        for (candidate in candidates) {
            val author = candidate.textContent().trim()
            if (isValidAuthor(author)) return author
        }

        return null
    }

    private fun isValidAuthor(text: String): Boolean {
        if (text.length > 60) return false
        if (text.contains("http://") || text.contains("https://")) return false
        if (text.isBlank()) return false
        return true
    }

    // ==================== Title Extraction ====================

    private fun extractTitle(root: HtmlElement): String? {
        // Look for h1 first
        val h1 = root.getElementsByTagName("h1").firstOrNull()
        if (h1 != null) {
            val title = h1.textContent().trim()
            if (title.isNotBlank()) return title
        }

        // Then h2
        val h2 = root.getElementsByTagName("h2").firstOrNull()
        if (h2 != null) {
            val title = h2.textContent().trim()
            if (title.isNotBlank()) return title
        }

        return null
    }

    // ==================== Image Handling ====================

    /** Placeholder URL patterns to reject (lazy-load placeholders, tracking pixels, etc.) */
    private val IMAGE_PLACEHOLDER_PATTERNS = setOf(
        "1x1", "pixel", "spacer", "blank", "data:image", "transparent.gif", "tracking"
    )

    /**
     * Normalizes all images in the DOM: resolves best URL from srcset/data-src/etc.,
     * rejects placeholders, and writes the resolved URL back to img src so downstream
     * code sees a single consistent attribute.
     */
    private fun normalizeImages(root: HtmlElement, baseUrl: Url) {
        for (img in root.getElementsByTagName("img")) {
            val bestSrc = resolveBestImageUrl(img, baseUrl) ?: continue
            img.attributes["src"] = bestSrc
        }
    }

    /**
     * Resolves the best image URL for an img element: parses srcset (picks largest width),
     * falls back to data-src, data-original, data-lazy-src, data-url, then src.
     * Rejects placeholders and returns absolute URL or null.
     */
    private fun resolveBestImageUrl(img: HtmlElement, baseUrl: Url): String? {
        val rawCandidates = mutableListOf<Pair<String, Int>>()

        // 1. srcset or data-srcset (pick largest width)
        val srcset = img.attr("srcset") ?: img.attr("data-srcset")
        if (srcset != null) {
            // Parse "url1 100w, url2 200w" or "url1 1x, url2 2x"
            val parts = srcset.split(Regex("""\s*,\s*"""))
            for (part in parts) {
                val tokens = part.trim().split(Regex("""\s+"""))
                val url = tokens.firstOrNull() ?: continue
                var width = 0
                if (tokens.size >= 2) {
                    val desc = tokens[1].lowercase()
                    when {
                        desc.endsWith("w") -> width = desc.removeSuffix("w").toIntOrNull() ?: 0
                        desc.endsWith("x") -> width = (desc.removeSuffix("x").toDoubleOrNull()?.times(100)?.toInt()) ?: 0
                    }
                }
                rawCandidates.add(url to width)
            }
        }

        if (rawCandidates.isNotEmpty()) {
            val best = rawCandidates.maxByOrNull { it.second } ?: rawCandidates.first()
            val candidate = best.first
            if (!isImagePlaceholder(candidate)) {
                resolveUrl(baseUrl, candidate)?.let { return it }
            }
        }

        // 2. data-src, data-original, data-lazy-src, data-url, then src
        val fallbacks = listOf(
            img.attr("data-src"),
            img.attr("data-original"),
            img.attr("data-lazy-src"),
            img.attr("data-url"),
            img.attr("src"),
        )
        for (candidate in fallbacks) {
            if (candidate != null && !isImagePlaceholder(candidate)) {
                resolveUrl(baseUrl, candidate)?.let { return it }
            }
        }

        return null
    }

    private fun isImagePlaceholder(url: String): Boolean {
        val u = url.lowercase()
        return IMAGE_PLACEHOLDER_PATTERNS.any { u.contains(it) }
    }

    // ==================== DOM → Markdown ====================

    private fun domToMarkdown(root: HtmlElement, baseUrl: Url): Pair<String, List<AssetToDownload>> {
        val sb = StringBuilder()
        val assetsToDownload = mutableListOf<AssetToDownload>()
        appendDomToMarkdown(root, baseUrl, sb, assetsToDownload)
        return sb.toString() to assetsToDownload
    }

    private fun appendDomToMarkdown(
        element: HtmlElement,
        baseUrl: Url,
        sb: StringBuilder,
        assetsToDownload: MutableList<AssetToDownload>,
    ) {
        for (child in element.children) {
            when (child) {
                is HtmlText -> {
                    val text = child.content.trim()
                    if (text.isNotBlank()) sb.append(escapeInlineMarkdown(text))
                }
                is HtmlElement -> {
                    when (child.tagName) {
                        "p" -> {
                            appendInlineMarkdown(child, baseUrl, sb)
                            sb.append("\n\n")
                        }
                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            val level = child.tagName[1].digitToInt()
                            sb.append("#".repeat(level)).append(" ")
                            appendInlineMarkdown(child, baseUrl, sb)
                            sb.append("\n\n")
                        }
                        "pre" -> {
                            val codeEl = child.getElementsByTagName("code").firstOrNull()
                            val text = (codeEl ?: child).textContent().trim()
                            val lang = codeEl?.className?.let { extractLanguage(it) }
                            sb.append("\n```").append(lang ?: "").append("\n").append(text).append("\n```\n\n")
                        }
                        "blockquote" -> {
                            for (c in child.children) {
                                when (c) {
                                    is HtmlText -> if (c.content.trim().isNotBlank()) sb.append("> ").append(c.content.trim().replace("\n", "\n> ")).append("\n")
                                    is HtmlElement -> {
                                        if (c.tagName == "p") {
                                            sb.append("> ")
                                            appendInlineMarkdown(c, baseUrl, sb)
                                            sb.append("\n")
                                        } else {
                                            appendDomToMarkdown(c, baseUrl, sb, assetsToDownload)
                                        }
                                    }
                                }
                            }
                            sb.append("\n")
                        }
                        "ul" -> {
                            for (li in child.childElements().filter { it.tagName == "li" }) {
                                sb.append("- ")
                                appendInlineOrBlockMarkdown(li, baseUrl, sb, assetsToDownload)
                                sb.append("\n")
                            }
                            sb.append("\n")
                        }
                        "ol" -> {
                            var index = 1
                            for (li in child.childElements().filter { it.tagName == "li" }) {
                                sb.append("$index. ")
                                appendInlineOrBlockMarkdown(li, baseUrl, sb, assetsToDownload)
                                sb.append("\n")
                                index++
                            }
                            sb.append("\n")
                        }
                        "hr" -> sb.append("---\n\n")
                        "figure" -> {
                            val img = child.getElementsByTagName("img").firstOrNull()
                            if (img != null) {
                                val caption = child.getElementsByTagName("figcaption").firstOrNull()?.textContent()?.trim()
                                emitImageMarkdown(img, baseUrl, sb, assetsToDownload, caption)
                            } else {
                                appendDomToMarkdown(child, baseUrl, sb, assetsToDownload)
                            }
                        }
                        "img" -> {
                            emitImageMarkdown(child, baseUrl, sb, assetsToDownload, null)
                        }
                        "table" -> {
                            emitTableMarkdown(child, sb)
                        }
                        "div", "section", "article", "main" -> appendDomToMarkdown(child, baseUrl, sb, assetsToDownload)
                        else -> {
                            val text = child.textContent().trim()
                            if (text.isNotBlank() && text.length > 10) {
                                appendInlineMarkdown(child, baseUrl, sb)
                                sb.append("\n\n")
                            } else {
                                appendDomToMarkdown(child, baseUrl, sb, assetsToDownload)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun appendInlineOrBlockMarkdown(
        element: HtmlElement,
        baseUrl: Url,
        sb: StringBuilder,
        assetsToDownload: MutableList<AssetToDownload>,
    ) {
        val directText = element.children.filterIsInstance<HtmlText>().joinToString("") { it.content.trim() }.trim()
        val hasBlockChildren = element.childElements().any { it.tagName in setOf("p", "ul", "ol", "div") }
        if (hasBlockChildren) {
            appendDomToMarkdown(element, baseUrl, sb, assetsToDownload)
        } else {
            appendInlineMarkdown(element, baseUrl, sb)
        }
    }

    private fun appendInlineMarkdown(element: HtmlElement, baseUrl: Url, sb: StringBuilder) {
        for (child in element.children) {
            when (child) {
                is HtmlText -> if (child.content.isNotBlank()) sb.append(escapeInlineMarkdown(child.content))
                is HtmlElement -> {
                    when (child.tagName) {
                        "b", "strong" -> {
                            sb.append("**")
                            appendInlineMarkdown(child, baseUrl, sb)
                            sb.append("**")
                        }
                        "i", "em" -> {
                            sb.append("*")
                            appendInlineMarkdown(child, baseUrl, sb)
                            sb.append("*")
                        }
                        "code" -> sb.append("`").append(child.textContent().trim()).append("`")
                        "a" -> {
                            val href = child.attr("href")
                            val url = if (href != null) resolveUrl(baseUrl, href) ?: href else ""
                            sb.append("[")
                            appendInlineMarkdown(child, baseUrl, sb)
                            sb.append("]($url)")
                        }
                        else -> appendInlineMarkdown(child, baseUrl, sb)
                    }
                }
            }
        }
    }

    private fun escapeInlineMarkdown(text: String): String =
        text.replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("[", "\\[")
            .replace("]", "\\]")

    private fun emitImageMarkdown(
        img: HtmlElement,
        baseUrl: Url,
        sb: StringBuilder,
        assetsToDownload: MutableList<AssetToDownload>,
        caption: String? = null,
    ) {
        val src = img.attr("src") ?: return
        if (src.startsWith("data:") || isImagePlaceholder(src)) return
        val resolvedUrl = resolveUrl(baseUrl, src) ?: return
        val assetId = IdGenerator.newId()
        val ext = extensionFromUrl(src)
        val relativePath = "../assets/$assetId.$ext"
        val alt = img.attr("alt")?.take(200) ?: ""
        assetsToDownload.add(
            AssetToDownload(
                assetId = assetId,
                resolvedUrl = resolvedUrl,
                relativePath = relativePath,
                alt = alt.ifBlank { null },
                caption = caption?.take(500)?.ifBlank { null },
            )
        )
        sb.append("![").append(alt.replace("]", "\\]")).append("](").append(relativePath).append(")\n\n")
        if (!caption.isNullOrBlank()) sb.append("*").append(caption.replace("*", "\\*")).append("*\n\n")
    }

    private fun extensionFromUrl(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".png") -> "png"
            lower.contains(".gif") -> "gif"
            lower.contains(".webp") -> "webp"
            lower.contains(".jpeg") || lower.contains(".jpg") -> "jpg"
            else -> "jpg"
        }
    }

    private fun emitTableMarkdown(table: HtmlElement, sb: StringBuilder) {
        val rows = table.getElementsByTagName("tr")
        if (rows.isEmpty()) return
        val headerCells = rows.first().getElementsByTagName("th").ifEmpty { rows.first().getElementsByTagName("td") }
        val hasTh = rows.first().getElementsByTagName("th").isNotEmpty()
        if (headerCells.isEmpty()) return
        sb.append("\n")
        sb.append("| ").append(headerCells.joinToString(" | ") { it.textContent().trim().replace("|", "\\|") }).append(" |\n")
        sb.append("| ").append(headerCells.map { "---" }.joinToString(" | ")).append(" |\n")
        for (i in 1 until rows.size) {
            val cells = rows[i].getElementsByTagName("td")
            if (cells.isNotEmpty()) {
                sb.append("| ").append(cells.joinToString(" | ") { it.textContent().trim().replace("|", "\\|") }).append(" |\n")
            }
        }
        sb.append("\n")
    }

    private fun postProcessMarkdown(markdown: String): String {
        var s = markdown
        s = s.replace(Regex("\n{3,}"), "\n\n")
        s = s.trim()
        s = s.replace(Regex("(?m) +$"), "")
        return s
    }

    private fun extractLanguage(className: String): String? {
        // Common patterns: "language-kotlin", "lang-python", "kotlin", etc.
        val patterns = listOf(
            Regex("""language-(\w+)"""),
            Regex("""lang-(\w+)"""),
            Regex("""brush:\s*(\w+)"""),
        )

        for (pattern in patterns) {
            val match = pattern.find(className.lowercase())
            if (match != null) return match.groupValues[1]
        }

        // Check if class is just the language name
        val knownLanguages = setOf(
            "kotlin", "java", "python", "javascript", "typescript", "rust",
            "go", "swift", "c", "cpp", "csharp", "ruby", "php", "sql",
            "html", "css", "json", "xml", "yaml", "bash", "shell"
        )
        val classes = className.lowercase().split(Regex("\\s+"))
        return classes.find { it in knownLanguages }
    }

    // ==================== URL Resolution ====================

    private fun resolveUrl(base: Url, relative: String): String? =
        runCatching { URLBuilder(base).takeFrom(relative).buildString() }.getOrNull()
            ?: runCatching { URLBuilder().takeFrom(relative).buildString() }.getOrNull()
}
