package dev.subfly.yaba.core.components.webview

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

private const val TAG = "YabaWebView"
private const val ASSET_LOADER_DOMAIN = "https://appassets.androidplatform.net"
private const val ASSET_LOADER_HOST = "appassets.androidplatform.net"
private const val BRIDGE_READY_POLL_MS = 50L
private const val BRIDGE_READY_TIMEOUT_MS = 5_000L

private fun toAssetLoaderUrl(rawUrl: String): String? {
    val pathInAssets = extractAndroidAssetPath(rawUrl) ?: return null
    return "$ASSET_LOADER_DOMAIN/$pathInAssets"
}

private fun extractAndroidAssetPath(rawUrl: String): String? {
    val uri = runCatching { rawUrl.toUri() }.getOrNull() ?: return null
    if (uri.scheme != "file") return null
    val path = uri.path ?: return null
    val marker = "/android_asset/"
    val markerIndex = path.indexOf(marker)
    if (markerIndex < 0) return null
    val assetPath = path.substring(markerIndex + marker.length)
    return assetPath.takeIf { it.isNotBlank() }
}

private fun isAssetLoaderRequest(uri: Uri?): Boolean = uri?.host == ASSET_LOADER_HOST

private fun escapeJsString(s: String): String =
    s.replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\"", "\\\"")

private fun decodeJsStringResult(value: String?): String {
    if (value == null) return ""
    val trimmed = value.trim()
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
        return trimmed.removeSurrounding("\"")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
    return trimmed
}

private suspend fun evaluateJs(webView: WebView, script: String): String =
    suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript(script) { value ->
            if (cont.isActive) cont.resume(value ?: "null")
        }
    }

private suspend fun waitForBridgeReady(webView: WebView, bridgeReadyCheckScript: String): Boolean {
    val attempts = (BRIDGE_READY_TIMEOUT_MS / BRIDGE_READY_POLL_MS).toInt()
    repeat(attempts) {
        val ready = evaluateJs(webView, bridgeReadyCheckScript).trim() == "true"
        if (ready) return true
        delay(BRIDGE_READY_POLL_MS)
    }
    return false
}

private fun defaultWebViewClient(
    assetLoader: WebViewAssetLoader,
    onPageFinished: () -> Unit,
    onUrlClick: ((String) -> Boolean)?,
): WebViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        onPageFinished()
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        request?.let {
            val path = it.url.path ?: ""
            if (path == "/favicon.ico" || path.endsWith("/favicon.ico")) return null
            if (!isAssetLoaderRequest(it.url)) return super.shouldInterceptRequest(view, it)
            return try {
                assetLoader.shouldInterceptRequest(it.url)
                    ?: super.shouldInterceptRequest(view, it)
            } catch (_: Exception) {
                super.shouldInterceptRequest(view, it)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val url = request?.url?.toString() ?: return false
        return onUrlClick?.invoke(url) ?: false
    }
}

private fun denyPermissionsChromeClient(): WebChromeClient = object : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.deny()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?,
    ) {
        callback?.invoke(origin, false, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (consoleMessage != null) {
            Log.d(
                TAG,
                "JS ${consoleMessage.messageLevel()} ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}"
            )
        }
        return super.onConsoleMessage(consoleMessage)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YabaWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    onUrlClick: (String) -> Boolean,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    val myWebView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            webChromeClient = denyPermissionsChromeClient()
        }
    }
    webViewRef.value = myWebView
    myWebView.webViewClient = defaultWebViewClient(
        assetLoader = assetLoader,
        onPageFinished = { isPageReady = true; onReady() },
        onUrlClick = onUrlClick,
    )

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (myWebView.parent != null) {
                    (myWebView.parent as ViewGroup).removeView(myWebView)
                }
                addView(myWebView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            }
        },
        update = {
            val lastLoadedUrl = myWebView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                myWebView.loadUrl(loadUrl)
                myWebView.tag = loadUrl
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose { webViewRef.value = null }
    }

    LaunchedEffect(isPageReady, markdown, assetsBaseUrl) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady()); } catch(e){ return false; } })();",
        )
        if (!ready) {
            Log.w(TAG, "Viewer bridge not ready before timeout")
            return@LaunchedEffect
        }

        val markdownEscaped = escapeJsString(markdown)
        val opts = if (assetsBaseUrl != null) {
            "{ assetsBaseUrl: '${escapeJsString(assetsBaseUrl)}' }"
        } else {
            "undefined"
        }
        val script = """
            (function() {
                window.YabaEditorBridge.setMarkdown('$markdownEscaped', $opts);
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YabaWebViewEditorInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    onUrlClick: (String) -> Boolean,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    var pendingPermissionRequest by remember { mutableStateOf<Pair<PermissionRequest, Array<String>>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val pending = pendingPermissionRequest
        pendingPermissionRequest = null
        if (pending != null) {
            val (request, permissions) = pending
            val allGranted = permissions.all { permission -> result[permission] == true }
            if (allGranted) request.grant(request.resources) else request.deny()
        }
    }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        if (request == null) return
                        val permissions = mutableListOf<String>()
                        request.resources.forEach { resource ->
                            when (resource) {
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                                    permissions.add(Manifest.permission.CAMERA)
                                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                                    permissions.add(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        val uniquePermissions = permissions.distinct()
                        if (uniquePermissions.isEmpty()) {
                            request.deny()
                            return
                        }
                        pendingPermissionRequest = request to uniquePermissions.toTypedArray()
                        permissionLauncher.launch(uniquePermissions.toTypedArray())
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?,
                    ) {
                        callback?.invoke(origin, false, false)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        if (consoleMessage != null) {
                            Log.d(
                                TAG,
                                "JS ${consoleMessage.messageLevel()} ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}"
                            )
                        }
                        return super.onConsoleMessage(consoleMessage)
                    }
                }
                webViewClient = defaultWebViewClient(
                    assetLoader = assetLoader,
                    onPageFinished = {
                        isPageReady = true
                        onReady()
                    },
                    onUrlClick = onUrlClick,
                )
                webViewRef.value = this
            }
            FrameLayout(ctx).apply {
                addView(webView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            }
        },
        update = {
            val webView = webViewRef.value ?: return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose { webViewRef.value = null }
    }

    LaunchedEffect(isPageReady, markdown, assetsBaseUrl) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady()); } catch(e){ return false; } })();",
        )
        if (!ready) {
            Log.w(TAG, "Editor bridge not ready before timeout")
            return@LaunchedEffect
        }

        val markdownEscaped = escapeJsString(markdown)
        val opts = if (assetsBaseUrl != null) {
            "{ assetsBaseUrl: '${escapeJsString(assetsBaseUrl)}' }"
        } else {
            "undefined"
        }
        val script = """
            (function() {
                window.YabaEditorBridge.setMarkdown('$markdownEscaped', $opts);
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YabaWebViewConverterInternal(
    modifier: Modifier,
    baseUrl: String,
    input: ConverterInput?,
    onConverterResult: (ConverterResult) -> Unit,
    onConverterError: (Throwable) -> Unit,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                webChromeClient = denyPermissionsChromeClient()
                webViewClient = defaultWebViewClient(
                    assetLoader = assetLoader,
                    onPageFinished = {
                        isPageReady = true
                        onReady()
                    },
                    onUrlClick = null,
                )
                webViewRef.value = this
            }
            FrameLayout(ctx).apply {
                addView(webView, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ))
            }
        },
        update = {
            val webView = webViewRef.value ?: return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose { webViewRef.value = null }
    }

    LaunchedEffect(isPageReady, input?.html, input?.baseUrl) {
        if (!isPageReady) return@LaunchedEffect
        val request = input ?: return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect

        val bridgeReady = waitForBridgeReady(
            webView,
            "(function(){ try { return typeof window.YabaConverterBridge !== 'undefined'; } catch(e){ return false; } })();",
        )
        if (!bridgeReady) {
            onConverterError(IllegalStateException("Converter bridge not ready"))
            return@LaunchedEffect
        }

        val htmlEscaped = escapeJsString(request.html)
        val baseUrlLiteral = request.baseUrl?.let { "'${escapeJsString(it)}'" } ?: "null"
        val convertScript = """
            (function() {
                try {
                    var result = window.YabaConverterBridge.sanitizeAndConvertHtmlToMarkdown({
                        html: '$htmlEscaped',
                        baseUrl: $baseUrlLiteral
                    });
                    return JSON.stringify(result);
                } catch (e) {
                    return JSON.stringify({ error: e.message });
                }
            })();
        """.trimIndent()

        val rawResult = evaluateJs(webView, convertScript)
        val jsonStr = decodeJsStringResult(rawResult)
        try {
            val json = JSONObject(jsonStr)
            if (json.has("error")) {
                onConverterError(IllegalStateException(json.optString("error")))
                return@LaunchedEffect
            }
            val markdownResult = json.optString("markdown", "")
            val assetsArray = json.optJSONArray("assets") ?: JSONArray()
            val assets = mutableListOf<ConverterAsset>()
            for (i in 0 until assetsArray.length()) {
                val item = assetsArray.optJSONObject(i) ?: continue
                assets.add(
                    ConverterAsset(
                        placeholder = item.optString("placeholder", ""),
                        url = item.optString("url", ""),
                        alt = item.optString("alt").takeIf { it.isNotEmpty() },
                    ),
                )
            }
            onConverterResult(ConverterResult(markdown = markdownResult, assets = assets))
        } catch (e: Exception) {
            onConverterError(e)
        }
    }
}
