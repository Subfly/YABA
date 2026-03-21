package dev.subfly.yabacore.unfurl

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.webview.WebConverterAsset
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

/**
 * Converts the JS converter output (HTML with `yaba-asset://` placeholders + asset URLs)
 * into [ReadableUnfurl] by downloading assets and rewriting placeholders in HTML.
 */
object ConverterResultProcessor {
    /**
     * @param html HTML with yaba-asset://N placeholders in img src (and similar)
     * @param assets Map of placeholder -> remote URL
     */
    suspend fun process(
        html: String,
        assets: List<WebConverterAsset>,
    ): ReadableUnfurl {
        val client = UnfurlHttpClient.client
        val readables = assets.mapNotNull { asset ->
            runCatching {
                val response: HttpResponse = client.get(asset.url)
                if (response.status.value in 200..299) {
                    val bytes = response.body<ByteArray>()
                    if (bytes.size in 1024..(5 * 1024 * 1024)) {
                        val ext = inferImageExtension(bytes, asset.url)
                        val assetId = IdGenerator.newId()
                        val relativePath = "../assets/$assetId.$ext"
                        ReadableAsset(assetId = assetId, extension = ext, bytes = bytes) to
                            (asset.placeholder to relativePath)
                    } else null
                } else null
            }.getOrNull()
        }

        var resultHtml = html
        readables.forEach { (_, mapping) ->
            val (placeholder, replacement) = mapping
            resultHtml = resultHtml.replace(placeholder, replacement)
        }

        return ReadableUnfurl(
            html = resultHtml,
            assets = readables.map { it.first },
        )
    }

    private fun inferImageExtension(bytes: ByteArray, url: String): String {
        if (bytes.size >= 3) {
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
                return "jpg"
            }
            if (bytes.size >= 4 &&
                bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            ) {
                return "png"
            }
            if (bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte()) {
                return "gif"
            }
            if (bytes.size >= 12 &&
                bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
                bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
                bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
                bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
            ) {
                return "webp"
            }
        }
        val urlLower = url.lowercase()
        return when {
            urlLower.contains(".png") -> "png"
            urlLower.contains(".gif") -> "gif"
            urlLower.contains(".webp") -> "webp"
            urlLower.contains(".jpeg") || urlLower.contains(".jpg") -> "jpg"
            else -> "jpg"
        }
    }
}
