package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yaba.core.webview.WebConverterAsset
import dev.subfly.yaba.core.webview.WebConverterResult
import dev.subfly.yaba.core.webview.WebEpubConverterResult
import dev.subfly.yaba.core.webview.WebLinkMetadata
import dev.subfly.yaba.core.webview.WebPdfConverterResult
import dev.subfly.yaba.core.webview.WebPdfTextSection
import dev.subfly.yaba.core.webview.YabaConverterBridgeScripts
import dev.subfly.yaba.core.webview.YabaWebBridgeScripts
import dev.subfly.yaba.core.webview.normalizeBridgeOptionalString
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
    val rawJobId =
            evaluateJs(
                    webView,
                    YabaConverterBridgeScripts.sanitizeAndConvertHtmlToReaderHtmlScript(
                            html,
                            baseUrl
                    ),
            )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start HTML conversion job"))
    }
    var attempts = 0
    while (attempts < 300) {
        attempts += 1
        val rawStatus =
                evaluateJs(
                        webView,
                        YabaConverterBridgeScripts.getHtmlConversionJobScript(jobId),
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
            evaluateJs(webView, YabaConverterBridgeScripts.deleteHtmlConversionJobScript(jobId))
            if (status == "error") {
                return Result.failure(
                        IllegalStateException(state.optString("error", "HTML conversion failed"))
                )
            }
            val json = state.optJSONObject("output") ?: JSONObject()
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
                            cleanedUrl =
                                    linkMetaJson.optString("cleanedUrl", "").normalizeBridgeOptionalString()
                                            ?: "",
                            title = linkMetaJson.optString("title").normalizeBridgeOptionalString(),
                            description =
                                    linkMetaJson.optString("description").normalizeBridgeOptionalString(),
                            author = linkMetaJson.optString("author").normalizeBridgeOptionalString(),
                            date = linkMetaJson.optString("date").normalizeBridgeOptionalString(),
                            audio = linkMetaJson.optString("audio").normalizeBridgeOptionalString(),
                            video = linkMetaJson.optString("video").normalizeBridgeOptionalString(),
                            image = linkMetaJson.optString("image").normalizeBridgeOptionalString(),
                            logo = linkMetaJson.optString("logo").normalizeBridgeOptionalString(),
                    )
            return Result.success(
                    WebConverterResult(
                            documentJson = documentJson,
                            assets = assets,
                            linkMetadata = linkMetadata
                    )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    return Result.failure(IllegalStateException("HTML conversion timed out"))
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
                            title = output.optString("title").normalizeBridgeOptionalString(),
                            author = output.optString("author").normalizeBridgeOptionalString(),
                            subject = output.optString("subject").normalizeBridgeOptionalString(),
                            creationDate =
                                    output.optString("creationDate").normalizeBridgeOptionalString(),
                            pageCount = output.optInt("pageCount", 0),
                            firstPagePngDataUrl =
                                    output.optString("firstPagePngDataUrl")
                                            .normalizeBridgeOptionalString(),
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
                                    output.optString("coverPngDataUrl").normalizeBridgeOptionalString(),
                            title = output.optString("title").normalizeBridgeOptionalString(),
                            author = output.optString("author").normalizeBridgeOptionalString(),
                            description =
                                    output.optString("description").normalizeBridgeOptionalString(),
                            pubdate = output.optString("pubdate").normalizeBridgeOptionalString(),
                    ),
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    return Result.failure(IllegalStateException("EPUB extraction timed out"))
}
