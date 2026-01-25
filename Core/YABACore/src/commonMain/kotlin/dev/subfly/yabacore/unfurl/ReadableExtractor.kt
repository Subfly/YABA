package dev.subfly.yabacore.unfurl

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.model.utils.ReadableAssetRole
import dev.subfly.yabacore.unfurl.ReadableHeadingLevel
import io.ktor.http.Url
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlin.time.Clock

/**
 * Extracts readable content from HTML using deterministic heuristics.
 *
 * The extraction pipeline:
 * 1. Parse HTML into minimal DOM
 * 2. Hard structural pruning (tags + class/id keywords)
 * 3. Link density pruning
 * 4. Text density pruning
 * 5. Image cluster pruning
 * 6. Article root selection
 * 7. Convert to readable AST with block/inline mapping
 * 8. Image retention and role assignment
 * 9. Author extraction
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

    // Image filename patterns to drop
    private val IMAGE_DROP_PATTERNS = setOf(
        "avatar", "icon", "logo", "sprite", "badge", "button",
        "arrow", "bullet", "tracking", "pixel", "analytics", "ad", "banner"
    )

    // Image area thresholds
    private const val IMAGE_KEEP_AREA = 100_000
    private const val IMAGE_DROP_AREA = 40_000

    // Maximum images to retain
    private const val MAX_INLINE_IMAGES = 3

    /**
     * Extract readable content from HTML.
     *
     * @param html Raw HTML string
     * @param baseUrl Base URL for resolving relative links/images
     * @param metaTitle Title from metadata (fallback)
     * @param metaAuthor Author from metadata (fallback)
     * @return Extraction result with document, image candidates, and extracted author
     */
    fun extract(
        html: String,
        baseUrl: Url,
        metaTitle: String? = null,
        metaAuthor: String? = null,
    ): ExtractionResult {
        // 1. Parse HTML
        val root = HtmlParser.parse(html)

        // 2. Extract author before pruning
        val author = extractAuthor(root, html) ?: metaAuthor

        // 3. Hard structural pruning
        applyHardPruning(root)

        // 4. Link density pruning
        applyLinkDensityPruning(root)

        // 5. Text density pruning
        applyTextDensityPruning(root)

        // 6. Image cluster pruning
        applyImageClusterPruning(root)

        // 7. Article root selection
        val articleRoot = selectArticleRoot(root) ?: root

        // 8. Extract title
        val title = extractTitle(articleRoot) ?: metaTitle

        // 9. Collect and score images
        val imageCandidates = collectImageCandidates(articleRoot, baseUrl)

        // 10. Convert to readable blocks
        var blockIndex = 0
        val blocks = mutableListOf<ReadableBlock>()

        fun nextBlockId(): String = "b${blockIndex++}"

        convertToBlocks(articleRoot, blocks, ::nextBlockId, baseUrl, imageCandidates)

        // 11. Assign image roles and filter
        val retainedImages = assignImageRoles(imageCandidates)

        // Create document snapshot (contentVersion will be set by caller)
        val document = ReadableDocumentSnapshot(
            schemaVersion = 1,
            contentVersion = 0, // Placeholder, set by ReadableContentManager
            sourceUrl = baseUrl.toString(),
            title = title,
            author = author,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            blocks = blocks,
        )

        return ExtractionResult(
            document = document,
            imageCandidates = retainedImages,
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

    data class ImageCandidate(
        val src: String,
        val resolvedUrl: String,
        val width: Int?,
        val height: Int?,
        val alt: String?,
        val inFigure: Boolean,
        val hasFigcaption: Boolean,
        val adjacentToHeading: Boolean,
        var role: ReadableAssetRole = ReadableAssetRole.INLINE,
        var assetId: String = "",
    )

    private fun collectImageCandidates(root: HtmlElement, baseUrl: Url): MutableList<ImageCandidate> {
        val candidates = mutableListOf<ImageCandidate>()

        for (img in root.getElementsByTagName("img")) {
            val src = img.attr("src") ?: continue
            if (src.startsWith("data:")) continue

            val resolvedUrl = resolveUrl(baseUrl, src) ?: continue
            val srcLower = src.lowercase()

            // Check drop patterns
            if (IMAGE_DROP_PATTERNS.any { srcLower.contains(it) }) continue

            val width = img.attr("width")?.toIntOrNull()
            val height = img.attr("height")?.toIntOrNull()
            val alt = img.attr("alt")

            // Check parent for figure
            val inFigure = isInsideFigure(img)
            val hasFigcaption = img.parent?.hasDescendant("figcaption") == true
            val adjacentToHeading = hasAdjacentHeading(img)

            candidates.add(
                ImageCandidate(
                    src = src,
                    resolvedUrl = resolvedUrl,
                    width = width,
                    height = height,
                    alt = alt,
                    inFigure = inFigure,
                    hasFigcaption = hasFigcaption,
                    adjacentToHeading = adjacentToHeading,
                )
            )
        }

        return candidates
    }

    private fun isInsideFigure(element: HtmlElement): Boolean {
        var parent = element.parent
        while (parent != null) {
            if (parent.tagName == "figure") return true
            parent = parent.parent
        }
        return false
    }

    private fun assignImageRoles(candidates: MutableList<ImageCandidate>): List<ImageCandidate> {
        val retained = mutableListOf<ImageCandidate>()

        for (candidate in candidates) {
            val area = if (candidate.width != null && candidate.height != null) {
                candidate.width * candidate.height
            } else null

            // Keep conditions (any true = keep)
            val keepImage = when {
                area != null && area >= IMAGE_KEEP_AREA -> true
                candidate.inFigure -> true
                candidate.hasFigcaption -> true
                candidate.adjacentToHeading -> true
                (candidate.alt?.length ?: 0) >= 40 -> true
                else -> false
            }

            // Drop conditions (all true = drop)
            val dropImage = when {
                area != null && area < IMAGE_DROP_AREA -> true
                candidate.alt != null && candidate.alt.length < 10 -> {
                    val srcLower = candidate.src.lowercase()
                    IMAGE_DROP_PATTERNS.any { srcLower.contains(it) }
                }
                else -> false
            }

            if (keepImage || !dropImage) {
                candidate.assetId = IdGenerator.newId()
                retained.add(candidate)
            }
        }

        // Assign roles
        if (retained.isNotEmpty()) {
            // Sort by area (largest first), treating unknown as 0
            val sorted = retained.sortedByDescending { candidate ->
                if (candidate.width != null && candidate.height != null) {
                    candidate.width * candidate.height
                } else 0
            }

            // Largest is hero
            sorted.first().role = ReadableAssetRole.HERO

            // Next few are inline, rest are dropped
            val inlineCount = minOf(MAX_INLINE_IMAGES, sorted.size - 1)
            for (i in 1..inlineCount) {
                sorted[i].role = ReadableAssetRole.INLINE
            }

            // Return only hero + inline images
            return sorted.take(1 + inlineCount)
        }

        return emptyList()
    }

    // ==================== Block Conversion ====================

    private fun convertToBlocks(
        element: HtmlElement,
        blocks: MutableList<ReadableBlock>,
        nextId: () -> String,
        baseUrl: Url,
        imageCandidates: List<ImageCandidate>,
    ) {
        for (child in element.children) {
            when (child) {
                is HtmlText -> {
                    val text = child.content.trim()
                    if (text.isNotBlank()) {
                        // Wrap orphan text in paragraph
                        blocks.add(
                            ReadableBlock.Paragraph(
                                id = nextId(),
                                inlines = listOf(ReadableInline.Text(text)),
                            )
                        )
                    }
                }

                is HtmlElement -> {
                    when (child.tagName) {
                        "p" -> {
                            val inlines = convertToInlines(child, baseUrl)
                            if (inlines.isNotEmpty()) {
                                blocks.add(ReadableBlock.Paragraph(id = nextId(), inlines = inlines))
                            }
                        }

                        "h1", "h2", "h3", "h4", "h5", "h6" -> {
                            val level = ReadableHeadingLevel.fromInt(child.tagName[1].digitToInt())
                            val inlines = convertToInlines(child, baseUrl)
                            if (inlines.isNotEmpty()) {
                                blocks.add(ReadableBlock.Heading(id = nextId(), level = level, inlines = inlines))
                            }
                        }

                        "pre" -> {
                            val codeElement = child.getElementsByTagName("code").firstOrNull()
                            val text = (codeElement ?: child).textContent()
                            val language = codeElement?.className?.let { extractLanguage(it) }
                            blocks.add(ReadableBlock.Code(id = nextId(), language = language, text = text))
                        }

                        "blockquote" -> {
                            val quoteBlocks = mutableListOf<ReadableBlock>()
                            convertToBlocks(child, quoteBlocks, nextId, baseUrl, imageCandidates)
                            if (quoteBlocks.isNotEmpty()) {
                                blocks.add(ReadableBlock.Quote(id = nextId(), children = quoteBlocks))
                            }
                        }

                        "ul" -> {
                            val items = convertListItems(child, nextId, baseUrl, imageCandidates)
                            if (items.isNotEmpty()) {
                                blocks.add(ReadableBlock.ListBlock(id = nextId(), ordered = false, items = items))
                            }
                        }

                        "ol" -> {
                            val items = convertListItems(child, nextId, baseUrl, imageCandidates)
                            if (items.isNotEmpty()) {
                                blocks.add(ReadableBlock.ListBlock(id = nextId(), ordered = true, items = items))
                            }
                        }

                        "hr" -> {
                            blocks.add(ReadableBlock.Divider(id = nextId()))
                        }

                        "figure" -> {
                            // Look for img inside figure
                            val img = child.getElementsByTagName("img").firstOrNull()
                            if (img != null) {
                                val src = img.attr("src")
                                val candidate = imageCandidates.find { it.src == src }
                                if (candidate != null) {
                                    val caption = child.getElementsByTagName("figcaption")
                                        .firstOrNull()?.textContent()?.trim()
                                    blocks.add(
                                        ReadableBlock.Image(
                                            id = nextId(),
                                            assetId = candidate.assetId,
                                            role = candidate.role,
                                            caption = caption,
                                        )
                                    )
                                }
                            } else {
                                // Figure without img, process children
                                convertToBlocks(child, blocks, nextId, baseUrl, imageCandidates)
                            }
                        }

                        "img" -> {
                            val src = child.attr("src")
                            val candidate = imageCandidates.find { it.src == src }
                            if (candidate != null) {
                                blocks.add(
                                    ReadableBlock.Image(
                                        id = nextId(),
                                        assetId = candidate.assetId,
                                        role = candidate.role,
                                        caption = child.attr("alt"),
                                    )
                                )
                            }
                        }

                        "div", "section", "article", "main" -> {
                            // Container elements: process children
                            convertToBlocks(child, blocks, nextId, baseUrl, imageCandidates)
                        }

                        else -> {
                            // Try to extract inline content if it looks like text
                            val text = child.textContent().trim()
                            if (text.isNotBlank() && text.length > 10) {
                                val inlines = convertToInlines(child, baseUrl)
                                if (inlines.isNotEmpty()) {
                                    blocks.add(ReadableBlock.Paragraph(id = nextId(), inlines = inlines))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun convertListItems(
        list: HtmlElement,
        nextId: () -> String,
        baseUrl: Url,
        imageCandidates: List<ImageCandidate>,
    ): List<ListItem> {
        val items = mutableListOf<ListItem>()

        for (child in list.children) {
            if (child is HtmlElement && child.tagName == "li") {
                val itemBlocks = mutableListOf<ReadableBlock>()
                convertToBlocks(child, itemBlocks, nextId, baseUrl, imageCandidates)

                // If no blocks but has text, create paragraph
                if (itemBlocks.isEmpty()) {
                    val inlines = convertToInlines(child, baseUrl)
                    if (inlines.isNotEmpty()) {
                        itemBlocks.add(ReadableBlock.Paragraph(id = nextId(), inlines = inlines))
                    }
                }

                if (itemBlocks.isNotEmpty()) {
                    items.add(ListItem(blocks = itemBlocks))
                }
            }
        }

        return items
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

    // ==================== Inline Conversion ====================

    private fun convertToInlines(element: HtmlElement, baseUrl: Url): List<ReadableInline> {
        val inlines = mutableListOf<ReadableInline>()

        for (child in element.children) {
            when (child) {
                is HtmlText -> {
                    val text = child.content
                    if (text.isNotBlank()) {
                        inlines.add(ReadableInline.Text(text))
                    }
                }

                is HtmlElement -> {
                    val childInlines = convertToInlines(child, baseUrl)
                    if (childInlines.isEmpty()) continue

                    when (child.tagName) {
                        "b", "strong" -> inlines.add(ReadableInline.Bold(childInlines))
                        "i", "em" -> inlines.add(ReadableInline.Italic(childInlines))
                        "u" -> inlines.add(ReadableInline.Underline(childInlines))
                        "s", "del", "strike" -> inlines.add(ReadableInline.Strikethrough(childInlines))
                        "code" -> {
                            val text = child.textContent()
                            inlines.add(ReadableInline.Code(text))
                        }

                        "a" -> {
                            val href = child.attr("href")
                            val resolvedHref = if (href != null) resolveUrl(baseUrl, href) ?: href else ""
                            inlines.add(ReadableInline.Link(resolvedHref, childInlines))
                        }

                        "span" -> {
                            // Check for semantic color (we preserve meaningful colors only)
                            // For now, just flatten spans
                            inlines.addAll(childInlines)
                        }

                        else -> {
                            // Flatten other inline elements
                            inlines.addAll(childInlines)
                        }
                    }
                }
            }
        }

        return mergeAdjacentText(inlines)
    }

    private fun mergeAdjacentText(inlines: List<ReadableInline>): List<ReadableInline> {
        if (inlines.isEmpty()) return inlines

        val result = mutableListOf<ReadableInline>()
        var pendingText: StringBuilder? = null

        for (inline in inlines) {
            if (inline is ReadableInline.Text) {
                if (pendingText == null) {
                    pendingText = StringBuilder(inline.content)
                } else {
                    pendingText.append(inline.content)
                }
            } else {
                if (pendingText != null) {
                    result.add(ReadableInline.Text(pendingText.toString()))
                    pendingText = null
                }
                result.add(inline)
            }
        }

        if (pendingText != null) {
            result.add(ReadableInline.Text(pendingText.toString()))
        }

        return result
    }

    // ==================== URL Resolution ====================

    private fun resolveUrl(base: Url, relative: String): String? =
        runCatching { URLBuilder(base).takeFrom(relative).buildString() }.getOrNull()
            ?: runCatching { URLBuilder().takeFrom(relative).buildString() }.getOrNull()
}

/**
 * Result of readable content extraction.
 */
internal data class ExtractionResult(
    val document: ReadableDocumentSnapshot,
    val imageCandidates: List<ReadableExtractor.ImageCandidate>,
) {
    /** Get resolved URLs for image candidates that need downloading */
    val imageUrls: List<Pair<String, String>> get() =
        imageCandidates.map { it.assetId to it.resolvedUrl }
}
