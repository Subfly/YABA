package dev.subfly.yabacore.unfurl

import co.touchlab.kermit.Logger
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.takeFrom
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object Unfurler {
    private const val META_TITLE = "title"
    private const val META_DESCRIPTION = "description"
    private const val META_TYPE = "type"
    private const val META_DOMAIN = "domain"
    private const val META_VIDEO = "video"
    private const val META_IMAGE = "image"

    private val client = UnfurlHttpClient.client
    private val logger = Logger.withTag("Unfurler")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun unfurl(urlString: String): YabaLinkPreview? {
        val normalized = normalizeURL(urlString)
        val cleaned = LinkCleaner.clean(normalized)
        val baseUrl = runCatching { Url(cleaned) }.getOrElse {
            logger.e { "Cannot create url for: $cleaned (original: $urlString)" }
            throw UnfurlError.CannotCreateUrl(cleaned)
        }

        val html = loadURL(cleaned) ?: throw UnfurlError.UnableToUnfurl

        val tags = extractAllMetaTags(html)
        val metadata = extractMetaData(tags).toMutableMap()

        val jsonLdMetadata = extractJsonLDMetadata(html)
        for ((key, value) in jsonLdMetadata) {
            if (!metadata.containsKey(key)) {
                metadata[key] = value
            }
        }

        val extras = extractAdditionalMetadata(html)
        for ((key, value) in extras) {
            if (!metadata.containsKey(key)) {
                metadata[key] = value
            }
        }

        // Decode HTML entities to clean up values
        metadata.keys.toList().forEach { key ->
            metadata[key]?.let { metadata[key] = decodeHTMLEntities(it) }
        }

        val faviconUrl = extractFaviconURL(html, baseUrl)

        val allImageUrls = extractAllImages(html, baseUrl).toMutableList()
        metadata[META_IMAGE]?.let { metaImage ->
            if (!allImageUrls.contains(metaImage)) {
                allImageUrls.add(0, metaImage)
            }
        }

        if (allImageUrls.isEmpty()) {
            extractMainImageFallback(html, baseUrl)?.let { fallback ->
                allImageUrls.add(fallback)
                metadata[META_IMAGE] = fallback
            }
        } else {
            metadata[META_IMAGE] = allImageUrls.first()
        }

        val limitedImageUrls = allImageUrls.take(10)
        val imageOptions = downloadMultipleImageData(limitedImageUrls)

        return YabaLinkPreview(
            url = cleaned,
            title = metadata[META_TITLE],
            description = metadata[META_DESCRIPTION],
            host = metadata[META_DOMAIN],
            iconUrl = faviconUrl,
            imageUrl = metadata[META_IMAGE],
            videoUrl = metadata[META_VIDEO],
            iconData = downloadImageData(faviconUrl),
            imageData = metadata[META_IMAGE]?.let { imageOptions[it] },
            imageOptions = imageOptions,
            readableHtml = html,
        )
    }

    private fun normalizeURL(urlString: String): String {
        var normalized = urlString.trim()

        if ((normalized.startsWith("\"") && normalized.endsWith("\"")) ||
            (normalized.startsWith("'") && normalized.endsWith("'"))
        ) {
            normalized = normalized.drop(1).dropLast(1)
        }

        val lower = normalized.lowercase()
        normalized = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> normalized
            lower.startsWith("www.") -> "https://$normalized"
            lower.startsWith("//") -> "https:$normalized"
            lower.startsWith("ftp://") ||
                    lower.startsWith("file://") ||
                    lower.startsWith("mailto:") -> normalized

            normalized.contains(".") && !normalized.contains(" ") -> "https://$normalized"
            else -> normalized
        }

        val protocolIndex = normalized.indexOf("://")
        if (protocolIndex != -1) {
            val protocolPart = normalized.substring(0, protocolIndex + 3)
            val pathPart = normalized.substring(protocolIndex + 3)
            val cleanedPath = pathPart.replace(Regex("//+"), "/")
            normalized = protocolPart + cleanedPath
        }

        return normalized
    }

    private suspend fun loadURL(urlString: String): String? {
        return runCatching {
            val response: HttpResponse = client.get(urlString) {
                // TODO: ADD CUSTOM CLIENTS :)
                header("User-Agent", "WhatsApp/2")
                header("Referer", "https://google.com/")
            }
            if (response.status.value in 200..299) {
                response.bodyAsText()
            } else null
        }.getOrNull()
    }

    private fun extractAllImages(html: String, baseURL: Url): List<String> {
        val regex = Regex(
            "<img\\s+[^>]*src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            RegexOption.IGNORE_CASE
        )
        val matches = regex.findAll(html)

        val candidates = matches.mapNotNull { match ->
            val src = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val lower = src.lowercase()
            if (lower.isEmpty() || lower.startsWith("data:")) return@mapNotNull null
            if (shouldSkipImage(lower)) return@mapNotNull null
            val score = calculateImageScore(lower)
            val resolved = resolveUrl(baseURL, src) ?: return@mapNotNull null
            resolved to score
        }.sortedByDescending { it.second }

        return candidates.map { it.first }.toList()
    }

    private fun shouldSkipImage(src: String): Boolean {
        val skipPatterns = listOf(
            "icon",
            "sprite",
            "badge",
            "button",
            "arrow",
            "bullet",
            "tracking",
            "pixel",
            "analytics",
            "ad",
            "banner",
            "logo",
            "favicon",
            "thumbnail",
            "avatar",
            "profile",
        )
        if (skipPatterns.any { src.contains(it) }) return true

        val sizePatterns = listOf(
            "16x16",
            "32x32",
            "48x48",
            "64x64",
            "1x1",
            "2x2",
            "small",
            "tiny",
            "mini",
            "_s.",
            "_xs.",
            "_sm.",
        )
        if (sizePatterns.any { src.contains(it) }) return true

        return false
    }

    private fun calculateImageScore(src: String): Int {
        var score = 0

        if (src.contains("hero") || src.contains("main") || src.contains("header")) score += 5
        if (src.contains("feature") || src.contains("cover") || src.contains("banner")) score += 3
        if (src.contains("large") || src.contains("big") || src.contains("full")) score += 2

        when {
            src.endsWith(".jpg") || src.endsWith(".jpeg") -> score += 2
            src.endsWith(".png") -> score += 1
            src.endsWith(".webp") -> score += 1
        }

        when {
            listOf("1200", "1920", "2048").any { src.contains(it) } -> score += 3
            listOf("800", "1024").any { src.contains(it) } -> score += 2
            listOf("600", "640").any { src.contains(it) } -> score += 1
        }

        return score
    }

    private fun extractMainImageFallback(html: String, baseURL: Url): String? =
        extractAllImages(html, baseURL).firstOrNull()

    private fun extractFaviconURL(html: String, baseURL: Url): String? {
        val rels = listOf("icon", "shortcut icon", "apple-touch-icon", "mask-icon")
        val mimePriority = listOf(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/svg+xml",
            "image/x-icon"
        )

        val regex = Regex(
            "<link[^>]*?rel\\s*=\\s*[\"']([^\"']+)[\"'][^>]*?>",
            RegexOption.IGNORE_CASE
        )
        val matches = regex.findAll(html)

        val candidates = matches
            .mapNotNull { match ->
                val tag = match.value
                val rel =
                    Regex(
                        "rel\\s*=\\s*[\"']([^\"']+)[\"']",
                        RegexOption.IGNORE_CASE
                    )
                        .find(tag)
                        ?.groupValues
                        ?.getOrNull(1)
                val href =
                    Regex(
                        "href\\s*=\\s*[\"']([^\"']+)[\"']",
                        RegexOption.IGNORE_CASE
                    )
                        .find(tag)
                        ?.groupValues
                        ?.getOrNull(1)
                if (rel == null || href == null) return@mapNotNull null
                if (rels.none { rel.contains(it, ignoreCase = true) })
                    return@mapNotNull null

                val type =
                    Regex(
                        "type\\s*=\\s*[\"']([^\"']+)[\"']",
                        RegexOption.IGNORE_CASE
                    )
                        .find(tag)
                        ?.groupValues
                        ?.getOrNull(1)
                Triple(href, type, rel)
            }
            .toList()

        val sorted = candidates.sortedBy { candidate ->
            val typeIndex = mimePriority.indexOf(candidate.second?.lowercase())
            if (typeIndex == -1) mimePriority.size else typeIndex
        }

        val href = sorted.firstOrNull()?.first
        val resolved = href?.let { resolveUrl(baseURL, it) }
        return resolved ?: resolveUrl(baseURL, "/favicon.ico")
    }

    private fun extractAllMetaTags(html: String): List<String> =
        Regex("<meta\\s+[^>]*?>", RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { it.value }
            .toList()

    private fun extractMetaData(metaTags: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()

        val prioritizedKeys = listOf(
            META_TITLE to
                    listOf("title", "og:title", "twitter:title", "itemprop=\"name\""),
            META_DESCRIPTION to
                    listOf(
                        "description",
                        "og:description",
                        "twitter:description",
                        "itemprop=\"description\""
                    ),
            META_TYPE to listOf("og:type", "twitter:type"),
            META_DOMAIN to listOf("og:site_name", "twitter:domain"),
            META_VIDEO to listOf("og:video", "twitter:player", "itemprop=\"video\""),
            META_IMAGE to listOf("og:image", "twitter:image", "itemprop=\"image\""),
        )

        for ((field, keys) in prioritizedKeys) {
            for (key in keys) {
                val content = contentForMetaKey(key, metaTags)
                if (content != null) {
                    result[field] = content
                    break
                }
            }
        }

        return result
    }

    private fun contentForMetaKey(targetKey: String, metaTags: List<String>): String? {
        val regex = Regex(
            """content\s*=\s*["'](.*?)["']""",
            RegexOption.IGNORE_CASE
        )

        for (tag in metaTags) {
            val lower = tag.lowercase()
            val matchesKey = lower.contains("""property="$targetKey"""") ||
                    lower.contains("""name="$targetKey"""") ||
                    lower.contains("""itemprop=$targetKey""")
            if (!matchesKey) continue

            val match = regex.find(tag) ?: continue
            return match.groupValues.getOrNull(1)?.trim('"', '\'')
        }
        return null
    }

    private fun extractJsonLDMetadata(html: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex(
            "(?s)<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>",
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
        )

        regex.findAll(html).forEach { match ->
            val jsonString = match.groupValues.getOrNull(1) ?: return@forEach
            runCatching {
                val element = json.parseToJsonElement(jsonString)
                parseJsonLdObject(element).let { parsed ->
                    parsed[META_TITLE]?.let { result[META_TITLE] = it }
                    parsed[META_DESCRIPTION]?.let { result[META_DESCRIPTION] = it }
                    parsed[META_IMAGE]?.let { result[META_IMAGE] = it }
                    parsed[META_VIDEO]?.let { result[META_VIDEO] = it }
                }
            }
        }

        return result
    }

    private fun parseJsonLdObject(element: JsonElement): Map<String, String> {
        if (element !is JsonObject) return emptyMap()
        val result = mutableMapOf<String, String>()

        val headline = element["headline"]?.jsonPrimitive?.contentOrNull
        val name = element["name"]?.jsonPrimitive?.contentOrNull
        val description = element["description"]?.jsonPrimitive?.contentOrNull
        val image = element["image"]?.let { jsonElement ->
            when (jsonElement) {
                is JsonPrimitive -> jsonElement.content
                is JsonObject -> jsonElement["url"]?.jsonPrimitive?.contentOrNull
                else -> null
            } ?: run {
                val array = jsonElement as? kotlinx.serialization.json.JsonArray
                array?.firstOrNull()?.jsonPrimitive?.contentOrNull
            }
        }

        val video = (element["video"] as? JsonObject)
            ?.get("contentUrl")
            ?.jsonPrimitive
            ?.contentOrNull

        result[META_TITLE] = headline ?: name ?: ""
        if (description != null) result[META_DESCRIPTION] = description
        if (!image.isNullOrBlank()) result[META_IMAGE] = image
        if (!video.isNullOrBlank()) result[META_VIDEO] = video

        return result.filterValues { it.isNotEmpty() }
    }

    private fun extractAdditionalMetadata(html: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val title = html.sliceBetween("<title>", "</title>")
        if (!title.isNullOrBlank()) result[META_TITLE] = title

        if (result[META_DESCRIPTION] == null) {
            val match = Regex("<p>(.*?)</p>", RegexOption.IGNORE_CASE).find(html)
            val desc = match
                ?.groupValues
                ?.getOrNull(1)
                ?.replace(Regex("<[^>]+>"), "")
                ?.trim()
            if (!desc.isNullOrEmpty()) result[META_DESCRIPTION] = desc
        }

        return result
    }

    private suspend fun downloadImageData(urlString: String?): ByteArray? {
        if (urlString.isNullOrBlank()) return null
        return runCatching {
            val response = client.get(urlString)
            if (response.status.value in 200..299) {
                response.body<ByteArray>()
            } else null
        }.getOrNull()
    }

    private suspend fun downloadMultipleImageData(imageUrls: List<String>): Map<String, ByteArray> =
        coroutineScope {
            val tasks = imageUrls.map { imageUrl ->
                async { imageUrl to downloadImageData(imageUrl) }
            }

            tasks.mapNotNull { deferred ->
                val (url, data) = deferred.await()
                if (data != null && isHighQualityImage(data)) url to data else null
            }.toMap()
        }

    private fun isHighQualityImage(data: ByteArray): Boolean {
        if (data.size < 1024) return false
        if (data.size > 5 * 1024 * 1024) return false
        return true
    }

    private fun decodeHTMLEntities(input: String): String {
        var output = input
        val entities = mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
        )
        entities.forEach { (entity, replacement) ->
            output = output.replace(entity, replacement)
        }

        output = output.replace(Regex("&#(\\d+);")) { match ->
            val code = match.groupValues.getOrNull(1)?.toIntOrNull()
            code?.toChar()?.toString() ?: match.value
        }

        output = output.replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            val code = match.groupValues.getOrNull(1)?.toIntOrNull(16)
            code?.toChar()?.toString() ?: match.value
        }

        return output
    }

    private fun resolveUrl(base: Url, relative: String): String? =
        runCatching { URLBuilder(base).takeFrom(relative).buildString() }.getOrNull()
            ?: runCatching { URLBuilder().takeFrom(relative).buildString() }.getOrNull()

    private fun String.sliceBetween(from: String, to: String): String? {
        val start = indexOf(from)
        if (start == -1) return null
        val end = indexOf(to, start + from.length)
        if (end == -1) return null
        return substring(start + from.length, end)
    }
}
