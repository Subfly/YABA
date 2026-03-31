package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yaba.core.webview.WebConverterResult
import dev.subfly.yaba.core.webview.WebEpubConverterResult
import dev.subfly.yaba.core.webview.WebPdfConverterResult
import dev.subfly.yaba.core.webview.YabaConverterBridgeScripts
import dev.subfly.yaba.core.webview.YabaWebBridgeScripts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

private const val CONVERTER_JOB_TIMEOUT_MS = 120_000L

internal suspend fun runHtmlConversion(
    webView: WebView,
    html: String,
    baseUrl: String?,
): Result<WebConverterResult> {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CONVERTER_BRIDGE_DEFINED)) {
        return Result.failure(IllegalStateException("Converter bridge not ready"))
    }
    val rawJobId = evaluateJs(
        webView,
        YabaConverterBridgeScripts.sanitizeAndConvertHtmlToReaderHtmlScript(
            html,
            baseUrl,
        ),
    )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start HTML conversion job"))
    }
    val deferred = CompletableDeferred<Result<WebConverterResult>>()
    YabaConverterJobBridge.registerHtmlJob(jobId, deferred)
    return try {
        withTimeout(CONVERTER_JOB_TIMEOUT_MS) { deferred.await() }
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        YabaConverterJobBridge.removeHtmlJob(jobId)
        evaluateJs(webView, YabaConverterBridgeScripts.deleteHtmlConversionJobScript(jobId))
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
    val rawJobId = evaluateJs(
        webView,
        YabaConverterBridgeScripts.startPdfExtractionScript(
            resolvedPdfUrl,
            renderScale,
        ),
    )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start PDF extraction job"))
    }
    val deferred = CompletableDeferred<Result<WebPdfConverterResult>>()
    YabaConverterJobBridge.registerPdfJob(jobId, deferred)
    return try {
        withTimeout(CONVERTER_JOB_TIMEOUT_MS) { deferred.await() }
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        YabaConverterJobBridge.removePdfJob(jobId)
        evaluateJs(webView, YabaConverterBridgeScripts.deletePdfExtractionJobScript(jobId))
    }
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
    val rawJobId = evaluateJs(
        webView,
        YabaConverterBridgeScripts.startEpubExtractionScript(resolvedUrl),
    )
    val jobId = decodeJsStringResult(rawJobId)
    if (jobId.isBlank()) {
        return Result.failure(IllegalStateException("Failed to start EPUB extraction job"))
    }
    val deferred = CompletableDeferred<Result<WebEpubConverterResult>>()
    YabaConverterJobBridge.registerEpubJob(jobId, deferred)
    return try {
        withTimeout(CONVERTER_JOB_TIMEOUT_MS) { deferred.await() }
    } catch (e: Exception) {
        Result.failure(e)
    } finally {
        YabaConverterJobBridge.removeEpubJob(jobId)
        evaluateJs(webView, YabaConverterBridgeScripts.deleteEpubExtractionJobScript(jobId))
    }
}
