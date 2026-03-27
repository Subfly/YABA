package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yabacore.webview.WebConverterAsset
import dev.subfly.yabacore.webview.WebConverterResult
import dev.subfly.yabacore.webview.WebEpubConverterResult
import dev.subfly.yabacore.webview.WebLinkMetadata
import dev.subfly.yabacore.webview.WebPdfConverterResult
import dev.subfly.yabacore.webview.WebPdfTextSection
import dev.subfly.yabacore.webview.YabaConverterBridgeScripts
import dev.subfly.yabacore.webview.YabaWebBridgeScripts
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

internal suspend fun runHtmlConversion(
        webView: WebView,
        html: String,
        baseUrl: String?,
): Result<WebConverterResult> {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CONVERTER_BRIDGE_DEFINED)) {
        return Result.failure(IllegalStateException("Converter bridge not ready"))
    }
    val rawResult =
            evaluateJs(
                    webView,
                    YabaConverterBridgeScripts.sanitizeAndConvertHtmlToReaderHtmlScript(
                            html,
                            baseUrl
                    ),
            )
    val jsonStr = decodeJsStringResult(rawResult)
    return runCatching {
        val json = JSONObject(jsonStr)
        if (json.has("error")) {
            error(json.optString("error"))
        }
        val documentJson = json.optString("documentJson", "")
        val assetsArray = json.optJSONArray("assets") ?: JSONArray()
        val assets = mutableListOf<WebConverterAsset>()
        for (i in 0 until assetsArray.length()) {
            val item = assetsArray.optJSONObject(i) ?: continue
            assets.add(
                    WebConverterAsset(
                            placeholder = item.optString("placeholder", ""),
                            url = item.optString("url", ""),
                            alt = item.optString("alt").takeIf { it.isNotEmpty() },
                    ),
            )
        }
        val linkMetaJson = json.optJSONObject("linkMetadata") ?: JSONObject()
        val linkMetadata =
                WebLinkMetadata(
                        cleanedUrl = linkMetaJson.optString("cleanedUrl", ""),
                        title = linkMetaJson.optString("title").takeIf { it.isNotEmpty() },
                        description =
                                linkMetaJson.optString("description").takeIf { it.isNotEmpty() },
                        author = linkMetaJson.optString("author").takeIf { it.isNotEmpty() },
                        date = linkMetaJson.optString("date").takeIf { it.isNotEmpty() },
                        audio = linkMetaJson.optString("audio").takeIf { it.isNotEmpty() },
                        video = linkMetaJson.optString("video").takeIf { it.isNotEmpty() },
                        image = linkMetaJson.optString("image").takeIf { it.isNotEmpty() },
                        logo = linkMetaJson.optString("logo").takeIf { it.isNotEmpty() },
                )
        WebConverterResult(
                documentJson = documentJson,
                assets = assets,
                linkMetadata = linkMetadata
        )
    }
}

internal suspend fun runPdfExtraction(
        webView: WebView,
        context: android.content.Context,
        pdfUrl: String,
        renderScale: Float,
): Result<WebPdfConverterResult> {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CONVERTER_BRIDGE_DEFINED)) {
        return Result.failure(IllegalStateException("Converter bridge not ready"))
    }
    val resolvedPdfUrl = toInternalStorageAssetLoaderFileUrl(context, pdfUrl) ?: pdfUrl
    val rawJobId =
            evaluateJs(
                    webView,
                    YabaConverterBridgeScripts.startPdfExtractionScript(
                            resolvedPdfUrl,
                            renderScale
                    ),
            )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start PDF extraction job"))
    }
    var attempts = 0
    while (attempts < 300) {
        attempts += 1
        val rawStatus =
                evaluateJs(
                        webView,
                        YabaConverterBridgeScripts.getPdfExtractionJobScript(jobId),
                )
        val statusStr = decodeJsStringResult(rawStatus)
        if (statusStr.isBlank() || statusStr == "null") {
            delay(100)
            continue
        }
        try {
            val state = JSONObject(statusStr)
            val status = state.optString("status")
            if (status == "pending") {
                delay(100)
                continue
            }
            evaluateJs(webView, YabaConverterBridgeScripts.deletePdfExtractionJobScript(jobId))
            if (status == "error") {
                return Result.failure(
                        IllegalStateException(state.optString("error", "PDF extraction failed"))
                )
            }
            val output = state.optJSONObject("output") ?: JSONObject()
            val sectionsJson = output.optJSONArray("sections") ?: JSONArray()
            val sections = buildList {
                for (index in 0 until sectionsJson.length()) {
                    val section = sectionsJson.optJSONObject(index) ?: continue
                    add(
                            WebPdfTextSection(
                                    sectionKey = section.optString("sectionKey", ""),
                                    text = section.optString("text", ""),
                            ),
                    )
                }
            }
            return Result.success(
                    WebPdfConverterResult(
                            title = output.optString("title").takeIf { it.isNotBlank() },
                            author = output.optString("author").takeIf { it.isNotBlank() },
                            subject = output.optString("subject").takeIf { it.isNotBlank() },
                            creationDate =
                                    output.optString("creationDate").takeIf { it.isNotBlank() },
                            pageCount = output.optInt("pageCount", 0),
                            firstPagePngDataUrl =
                                    output.optString("firstPagePngDataUrl").takeIf {
                                        it.isNotBlank()
                                    },
                            sections = sections,
                    ),
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    return Result.failure(IllegalStateException("PDF extraction timed out"))
}

internal suspend fun runEpubExtraction(
        webView: WebView,
        context: android.content.Context,
        epubUrl: String,
): Result<WebEpubConverterResult> {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CONVERTER_BRIDGE_DEFINED)) {
        return Result.failure(IllegalStateException("Converter bridge not ready"))
    }
    val resolvedUrl = toInternalStorageAssetLoaderFileUrl(context, epubUrl) ?: epubUrl
    val rawJobId =
            evaluateJs(
                    webView,
                    YabaConverterBridgeScripts.startEpubExtractionScript(resolvedUrl),
            )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start EPUB extraction job"))
    }
    var attempts = 0
    while (attempts < 300) {
        attempts += 1
        val rawStatus =
                evaluateJs(
                        webView,
                        YabaConverterBridgeScripts.getEpubExtractionJobScript(jobId),
                )
        val statusStr = decodeJsStringResult(rawStatus)
        if (statusStr.isBlank() || statusStr == "null") {
            delay(100)
            continue
        }
        try {
            val state = JSONObject(statusStr)
            val status = state.optString("status")
            if (status == "pending") {
                delay(100)
                continue
            }
            evaluateJs(webView, YabaConverterBridgeScripts.deleteEpubExtractionJobScript(jobId))
            if (status == "error") {
                return Result.failure(
                        IllegalStateException(state.optString("error", "EPUB extraction failed"))
                )
            }
            val output = state.optJSONObject("output") ?: JSONObject()
            return Result.success(
                    WebEpubConverterResult(
                            coverPngDataUrl =
                                    output.optString("coverPngDataUrl").takeIf { it.isNotBlank() },
                            title = output.optString("title").takeIf { it.isNotBlank() },
                            author = output.optString("author").takeIf { it.isNotBlank() },
                            description =
                                    output.optString("description").takeIf { it.isNotBlank() },
                            pubdate = output.optString("pubdate").takeIf { it.isNotBlank() },
                            identifier = output.optString("identifier").takeIf { it.isNotBlank() },
                    ),
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    return Result.failure(IllegalStateException("EPUB extraction timed out"))
}
