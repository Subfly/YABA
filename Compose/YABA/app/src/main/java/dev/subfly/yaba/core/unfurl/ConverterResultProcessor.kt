package dev.subfly.yaba.core.unfurl

import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.images.ImageCompression
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.webview.WebConverterAsset
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

/**
 * Converts the JS converter output (document JSON with `yaba-asset://` placeholders + asset URLs)
 * into [ReadableUnfurl] by downloading assets and rewriting placeholders in the JSON string.
 */
object ConverterResultProcessor {
    /**
     * @param documentJson Rich-text document JSON with yaba-asset://N placeholders (e.g. image src attrs)
     * @param assets Remote URLs for each placeholder
     */
    suspend fun process(
        documentJson: String,
        assets: List<WebConverterAsset>,
    ): ReadableUnfurl {
        val client = UnfurlHttpClient.client
        val compressionP = SettingsStores.userPreferences.get().imageCompressionPercent.coerceIn(0, 50)
        val readables = assets.mapNotNull { asset ->
            runCatching {
                val response: HttpResponse = client.get(asset.url)
                if (response.status.value in 200..299) {
                    var bytes = response.body<ByteArray>()
                    if (bytes.size in 1024..(5 * 1024 * 1024)) {
                        val extHint = inferImageExtension(bytes, asset.url)
                        val compressed =
                            ImageCompression.compressForStorage(
                                input = bytes,
                                sourceExtension = extHint,
                                compressionPercent = compressionP,
                            )
                        if (compressed != null) {
                            bytes = compressed.bytes
                        }
                        val ext =
                            if (compressed != null) {
                                mapAssetExtensionForPath(compressed.extension)
                            } else {
                                extHint
                            }
                        val assetId = IdGenerator.newId()
                        val relativePath = "../assets/$assetId.$ext"
                        ReadableAsset(assetId = assetId, extension = ext, bytes = bytes) to
                            (asset.placeholder to relativePath)
                    } else null
                } else null
            }.getOrNull()
        }

        var resultJson = documentJson
        readables.forEach { (_, mapping) ->
            val (placeholder, replacement) = mapping
            resultJson = resultJson.replace(placeholder, replacement)
        }

        return ReadableUnfurl(
            documentJson = resultJson,
            assets = readables.map { it.first },
        )
    }

    private fun mapAssetExtensionForPath(outExt: String): String {
        val e = outExt.lowercase().removePrefix(".")
        return when (e) {
            "jpg" -> "jpeg"
            else -> e
        }
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
