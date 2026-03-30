package dev.subfly.yabacore.unfurl

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Url

/**
 * Fetches remote HTML for link bookmarks. Parsing and metadata extraction run in
 * [Extensions/yaba-web-components] (web-meta-scraper + tidy-url) inside the WebView bridge.
 */
object Unfurler {
    private val client = UnfurlHttpClient.client

    suspend fun unfurl(urlString: String): YabaLinkPreview? {
        val normalized = normalizeURL(urlString)
        val baseUrl = runCatching { Url(normalized) }.getOrElse {
            throw UnfurlError.CannotCreateUrl(normalized)
        }

        val html = loadURL(normalized) ?: throw UnfurlError.UnableToUnfurl

        val host = baseUrl.host

        return YabaLinkPreview(
            url = normalized,
            host = host,
            rawHtml = html,
            readable = null,
        )
    }

    /**
     * Downloads image bytes for bookmark preview (main image or domain icon).
     */
    suspend fun downloadPreviewImageBytes(urlString: String?): ByteArray? {
        if (urlString.isNullOrBlank()) return null
        return runCatching {
            val response: HttpResponse = client.get(urlString)
            if (response.status.value in 200..299) {
                response.body<ByteArray>()
            } else {
                null
            }
        }.getOrNull()
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
                header(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
                header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            }
            if (response.status.value in 200..299) {
                response.bodyAsText()
            } else {
                null
            }
        }.getOrNull()
    }
}
