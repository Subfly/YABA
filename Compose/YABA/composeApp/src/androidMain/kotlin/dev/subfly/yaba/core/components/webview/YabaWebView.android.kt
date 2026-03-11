package dev.subfly.yaba.core.components.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.highlight.HighlightQuoteSnapshot
import dev.subfly.yabacore.model.highlight.HighlightSourceContext
import dev.subfly.yabacore.model.highlight.ReadableAnchor
import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.model.utils.ReaderTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.math.abs

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

private fun normalizeTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

private fun toInternalStorageAssetLoaderBaseUrl(
    context: Context,
    rawBaseUrl: String?,
): String? {
    if (rawBaseUrl.isNullOrBlank()) return null
    val uri = runCatching { rawBaseUrl.toUri() }.getOrNull() ?: return null
    if (uri.scheme != "file") return null
    val rawPath = uri.path ?: return null
    val filesRoot = context.filesDir.absolutePath.trimEnd('/')
    if (!rawPath.startsWith(filesRoot)) return null

    val relativePath = rawPath.removePrefix(filesRoot).trimStart('/')
    if (relativePath.isBlank()) return null

    return normalizeTrailingSlash("$ASSET_LOADER_DOMAIN/local/$relativePath")
}

private fun toInternalStorageAssetLoaderFileUrl(
    context: Context,
    rawFileUrl: String?,
): String? {
    if (rawFileUrl.isNullOrBlank()) return null
    // BookmarkFileManager.getAbsolutePath returns a raw path (e.g. /data/.../files/YABA/...),
    // not a file:// URI. Handle both: file:// URLs and raw absolute paths.
    val rawPath = when {
        rawFileUrl.startsWith("file://") -> runCatching { rawFileUrl.toUri() }.getOrNull()?.path
        rawFileUrl.startsWith("/") -> rawFileUrl
        else -> runCatching { rawFileUrl.toUri() }.getOrNull()?.takeIf { it.scheme == "file" }?.path
    } ?: return null
    val filesRoot = context.filesDir.absolutePath.trimEnd('/')
    if (!rawPath.startsWith(filesRoot)) return null

    val relativePath = rawPath.removePrefix(filesRoot).trimStart('/')
    if (relativePath.isBlank()) return null
    return "$ASSET_LOADER_DOMAIN/local/$relativePath"
}

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

private fun ReaderTheme.toJsValue(): String =
    when (this) {
        ReaderTheme.SYSTEM -> "system"
        ReaderTheme.DARK -> "dark"
        ReaderTheme.LIGHT -> "light"
        ReaderTheme.SEPIA -> "sepia"
    }

private fun ReaderFontSize.toJsValue(): String =
    when (this) {
        ReaderFontSize.SMALL -> "small"
        ReaderFontSize.MEDIUM -> "medium"
        ReaderFontSize.LARGE -> "large"
    }

private fun ReaderLineHeight.toJsValue(): String =
    when (this) {
        ReaderLineHeight.NORMAL -> "normal"
        ReaderLineHeight.RELAXED -> "relaxed"
    }

private fun YabaWebPlatform.toJsValue(): String =
    when (this) {
        YabaWebPlatform.Compose -> "compose"
        YabaWebPlatform.Darwin -> "darwin"
    }

private fun YabaWebAppearance.toJsValue(): String =
    when (this) {
        YabaWebAppearance.Auto -> "auto"
        YabaWebAppearance.Light -> "light"
        YabaWebAppearance.Dark -> "dark"
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
    onHighlightTap: ((String) -> Unit)? = null,
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
        if (url.startsWith("yaba://highlight-tap?")) {
            val id = try {
                url.toUri().getQueryParameter("id") ?: ""
            } catch (_: Exception) { "" }
            if (id.isNotBlank()) onHighlightTap?.invoke(id)
            return true
        }
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

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
actual fun YabaWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    markdown: String,
    assetsBaseUrl: String?,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
    readerPreferences: ReaderPreferences,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReady: () -> Unit,
    onBridgeReady: (WebViewReaderBridge) -> Unit,
    onHighlightTap: (String) -> Unit,
    highlights: List<HighlightUiModel>,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    val onScrollDirectionChangedState = rememberUpdatedState(onScrollDirectionChanged)
    val gestureStartYRef = remember { floatArrayOf(0f) }
    val lastGestureDirectionRef = remember { intArrayOf(0) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler(
                "/local/",
                WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir),
            )
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    val myWebView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            webChromeClient = denyPermissionsChromeClient()
            settings.setSupportZoom(false)
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureStartYRef[0] = motionEvent.y
                        lastGestureDirectionRef[0] = 0
                        false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - gestureStartYRef[0]
                        val threshold = touchSlop * 2
                        if (abs(deltaY) < threshold) return@setOnTouchListener false

                        val gestureDirection = if (deltaY < 0f) 1 else -1
                        if (gestureDirection == lastGestureDirectionRef[0]) return@setOnTouchListener false

                        if (gestureDirection > 0) {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Down)
                        } else {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Up)
                        }
                        lastGestureDirectionRef[0] = gestureDirection
                        false
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        lastGestureDirectionRef[0] = 0
                        false
                    }
                    else -> false
                }
            }
        }
    }
    webViewRef.value = myWebView
    myWebView.webViewClient = defaultWebViewClient(
        assetLoader = assetLoader,
        onPageFinished = { isPageReady = true; onReady() },
        onUrlClick = onUrlClick,
        onHighlightTap = onHighlightTap,
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
        val resolvedAssetsBaseUrl = toInternalStorageAssetLoaderBaseUrl(context, assetsBaseUrl) ?: assetsBaseUrl
        val opts = if (resolvedAssetsBaseUrl != null) {
            "{ assetsBaseUrl: '${escapeJsString(resolvedAssetsBaseUrl)}' }"
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

    LaunchedEffect(isPageReady, readerPreferences, platform, appearance) {
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

        val readerTheme = readerPreferences.theme.toJsValue()
        val readerFontSize = readerPreferences.fontSize.toJsValue()
        val readerLineHeight = readerPreferences.lineHeight.toJsValue()
        val themePlatform = platform.toJsValue()
        val themeAppearance = appearance.toJsValue()
        val script = """
            (function() {
                if (!window.YabaEditorBridge) return;
                if (typeof window.YabaEditorBridge.setPlatform === "function") {
                    window.YabaEditorBridge.setPlatform('$themePlatform');
                }
                if (typeof window.YabaEditorBridge.setAppearance === "function") {
                    window.YabaEditorBridge.setAppearance('$themeAppearance');
                }
                if (typeof window.YabaEditorBridge.setReaderPreferences === "function") {
                    window.YabaEditorBridge.setReaderPreferences({
                        theme: '$readerTheme',
                        fontSize: '$readerFontSize',
                        lineHeight: '$readerLineHeight'
                    });
                }
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }

    val readerBridge = remember(webViewRef) {
        object : WebViewReaderBridge {
            override suspend fun getSelectionSnapshot(
                bookmarkId: String,
                readableVersionId: String,
            ): ReadableSelectionDraft? {
                val webView = webViewRef.value ?: return null
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady()); } catch(e){ return false; } })();",
                )
                if (!ready) return null
                val script = """
                    (function() {
                        try {
                            var snap = window.YabaEditorBridge.getSelectionSnapshot();
                            if (!snap) return null;
                            return JSON.stringify(snap);
                        } catch(e) { return null; }
                    })();
                """.trimIndent()
                val raw = evaluateJs(webView, script)
                val jsonStr = decodeJsStringResult(raw)
                if (jsonStr == "null" || jsonStr.isBlank()) return null
                return runCatching {
                    val json = JSONObject(jsonStr)
                    val startSectionKey = json.optString("startSectionKey", "")
                    val startOffsetInSection = json.optInt("startOffsetInSection", 0)
                    val endSectionKey = json.optString("endSectionKey", "")
                    val endOffsetInSection = json.optInt("endOffsetInSection", 0)
                    val selectedText = json.optString("selectedText", "")
                    val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
                    val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }

                    if (startSectionKey.isBlank() || endSectionKey.isBlank() || selectedText.isBlank()) return@runCatching null

                    ReadableSelectionDraft(
                        sourceContext = HighlightSourceContext.readable(bookmarkId, readableVersionId),
                        anchor = ReadableAnchor(
                            readableVersionId = readableVersionId,
                            startSectionKey = startSectionKey,
                            startOffsetInSection = startOffsetInSection,
                            endSectionKey = endSectionKey,
                            endOffsetInSection = endOffsetInSection,
                        ),
                        quote = HighlightQuoteSnapshot(
                            selectedText = selectedText,
                            prefixText = prefixText,
                            suffixText = suffixText,
                        ),
                    )
                }.getOrNull()
            }

            override suspend fun getCanCreateHighlight(): Boolean {
                val webView = webViewRef.value ?: return false
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return false
                val script = """
                    (function() {
                        try {
                            return !!(window.YabaEditorBridge && window.YabaEditorBridge.getCanCreateHighlight && window.YabaEditorBridge.getCanCreateHighlight());
                        } catch(e) { return false; }
                    })();
                """.trimIndent()
                return evaluateJs(webView, script).trim() == "true"
            }

            override suspend fun setHighlights(highlights: List<HighlightUiModel>) {
                val webView = webViewRef.value ?: return
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return
                val arr = JSONArray()
                for (h in highlights) {
                    val obj = JSONObject().apply {
                        put("id", h.id)
                        put("startSectionKey", h.startSectionKey)
                        put("startOffsetInSection", h.startOffsetInSection)
                        put("endSectionKey", h.endSectionKey)
                        put("endOffsetInSection", h.endOffsetInSection)
                        put("colorRole", h.colorRole.name)
                    }
                    arr.put(obj)
                }
                val jsonStr = arr.toString()
                val escaped = escapeJsString(jsonStr)
                val script = """
                    (function() {
                        try {
                            var json = JSON.parse('$escaped');
                            if (window.YabaEditorBridge && window.YabaEditorBridge.setHighlights) {
                                window.YabaEditorBridge.setHighlights(JSON.stringify(json));
                            }
                        } catch(e) {}
                    })();
                """.trimIndent()
                evaluateJs(webView, script)
            }

            override suspend fun scrollToHighlight(highlightId: String) {
                val webView = webViewRef.value ?: return
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return
                val escaped = escapeJsString(highlightId)
                val script = """
                    (function() {
                        try {
                            if (window.YabaEditorBridge && window.YabaEditorBridge.scrollToHighlight) {
                                window.YabaEditorBridge.scrollToHighlight('$escaped');
                            }
                        } catch(e) {}
                    })();
                """.trimIndent()
                evaluateJs(webView, script)
            }
        }
    }

    LaunchedEffect(isPageReady, readerBridge) {
        if (isPageReady) onBridgeReady(readerBridge)
    }

    LaunchedEffect(isPageReady) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();",
        )
        if (!ready) return@LaunchedEffect
        val script = """
            (function() {
                if (window.YabaEditorBridge) {
                    window.YabaEditorBridge.onHighlightTap = function(id) {
                        if (id) window.location = "yaba://highlight-tap?id=" + encodeURIComponent(id);
                    };
                }
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }

    LaunchedEffect(isPageReady, highlights) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();",
        )
        if (!ready) return@LaunchedEffect
        val arr = JSONArray()
        for (h in highlights) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("startSectionKey", h.startSectionKey)
                put("startOffsetInSection", h.startOffsetInSection)
                put("endSectionKey", h.endSectionKey)
                put("endOffsetInSection", h.endOffsetInSection)
                put("colorRole", h.colorRole.name)
            }
            arr.put(obj)
        }
        val jsonStr = arr.toString()
        val escaped = escapeJsString(jsonStr)
        val script = """
            (function() {
                try {
                    var json = JSON.parse('$escaped');
                    if (window.YabaEditorBridge && window.YabaEditorBridge.setHighlights) {
                        window.YabaEditorBridge.setHighlights(JSON.stringify(json));
                    }
                } catch(e) {}
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
            .addPathHandler(
                "/local/",
                WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir),
            )
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
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
                settings.setSupportZoom(false)
                setBackgroundColor(Color.TRANSPARENT)
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
        val resolvedAssetsBaseUrl = toInternalStorageAssetLoaderBaseUrl(context, assetsBaseUrl) ?: assetsBaseUrl
        val opts = if (resolvedAssetsBaseUrl != null) {
            "{ assetsBaseUrl: '${escapeJsString(resolvedAssetsBaseUrl)}' }"
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
                settings.allowFileAccess = true
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun YabaWebViewPdfConverterInternal(
    modifier: Modifier,
    baseUrl: String,
    input: PdfConverterInput?,
    onPdfConverterResult: (PdfConverterResult) -> Unit,
    onPdfConverterError: (Throwable) -> Unit,
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler(
                "/local/",
                WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir),
            )
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val webView = WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
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
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
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

    LaunchedEffect(isPageReady, input?.pdfUrl, input?.renderScale) {
        if (!isPageReady) return@LaunchedEffect
        val request = input ?: return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect

        val bridgeReady = waitForBridgeReady(
            webView,
            "(function(){ try { return typeof window.YabaConverterBridge !== 'undefined'; } catch(e){ return false; } })();",
        )
        if (!bridgeReady) {
            onPdfConverterError(IllegalStateException("Converter bridge not ready"))
            return@LaunchedEffect
        }

        val resolvedPdfUrl = toInternalStorageAssetLoaderFileUrl(context, request.pdfUrl) ?: request.pdfUrl
        val pdfUrlEscaped = escapeJsString(resolvedPdfUrl)
        val renderScale = request.renderScale
        val startScript = """
            (function() {
                try {
                    return window.YabaConverterBridge.startPdfExtraction({
                        pdfUrl: '$pdfUrlEscaped',
                        renderScale: $renderScale
                    });
                } catch (e) {
                    return "";
                }
            })();
        """.trimIndent()
        val rawJobId = evaluateJs(webView, startScript)
        val jobId = decodeJsStringResult(rawJobId)
        if (jobId.isBlank()) {
            onPdfConverterError(IllegalStateException("Failed to start PDF extraction job"))
            return@LaunchedEffect
        }

        var attempts = 0
        while (attempts < 300) {
            attempts += 1
            val statusScript = """
                (function() {
                    try {
                        var state = window.YabaConverterBridge.getPdfExtractionJob('$jobId');
                        return JSON.stringify(state);
                    } catch (e) {
                        return JSON.stringify({ status: "error", error: e.message });
                    }
                })();
            """.trimIndent()
            val rawStatus = evaluateJs(webView, statusScript)
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

                evaluateJs(
                    webView,
                    "(function(){ try { window.YabaConverterBridge.deletePdfExtractionJob('$jobId'); } catch(e){} })();",
                )

                if (status == "error") {
                    val errorMessage = state.optString("error", "PDF extraction failed")
                    onPdfConverterError(IllegalStateException(errorMessage))
                    return@LaunchedEffect
                }

                val output = state.optJSONObject("output") ?: JSONObject()
                val sectionsJson = output.optJSONArray("sections") ?: JSONArray()
                val sections = buildList {
                    for (index in 0 until sectionsJson.length()) {
                        val section = sectionsJson.optJSONObject(index) ?: continue
                        add(
                            PdfTextSection(
                                sectionKey = section.optString("sectionKey", ""),
                                text = section.optString("text", ""),
                            ),
                        )
                    }
                }
                onPdfConverterResult(
                    PdfConverterResult(
                        title = output.optString("title").takeIf { it.isNotBlank() },
                        pageCount = output.optInt("pageCount", 0),
                        firstPagePngDataUrl = output.optString("firstPagePngDataUrl").takeIf { it.isNotBlank() },
                        sections = sections,
                    ),
                )
                return@LaunchedEffect
            } catch (error: Exception) {
                onPdfConverterError(error)
                return@LaunchedEffect
            }
        }

        onPdfConverterError(IllegalStateException("PDF extraction timed out"))
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
actual fun YabaPdfWebViewViewerInternal(
    modifier: Modifier,
    baseUrl: String,
    pdfUrl: String,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReady: () -> Unit,
    onBridgeReady: (WebViewReaderBridge) -> Unit,
    onHighlightTap: (String) -> Unit,
    highlights: List<HighlightUiModel>,
) {
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var isPageReady by remember { mutableStateOf(false) }
    val onScrollDirectionChangedState = rememberUpdatedState(onScrollDirectionChanged)
    val gestureStartYRef = remember { floatArrayOf(0f) }
    val lastGestureDirectionRef = remember { intArrayOf(0) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) {
        WebViewAssetLoader.Builder()
            .addPathHandler(
                "/local/",
                WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir),
            )
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
    }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    val myWebView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            webChromeClient = denyPermissionsChromeClient()
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureStartYRef[0] = motionEvent.y
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - gestureStartYRef[0]
                        val threshold = touchSlop * 2
                        if (abs(deltaY) < threshold) return@setOnTouchListener false

                        val gestureDirection = if (deltaY < 0f) 1 else -1
                        if (gestureDirection == lastGestureDirectionRef[0]) return@setOnTouchListener false

                        if (gestureDirection > 0) {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Down)
                        } else {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Up)
                        }
                        lastGestureDirectionRef[0] = gestureDirection
                        false
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    else -> false
                }
            }
        }
    }
    webViewRef.value = myWebView
    myWebView.webViewClient = defaultWebViewClient(
        assetLoader = assetLoader,
        onPageFinished = { isPageReady = true; onReady() },
        onUrlClick = null,
        onHighlightTap = onHighlightTap,
    )

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (myWebView.parent != null) {
                    (myWebView.parent as ViewGroup).removeView(myWebView)
                }
                addView(
                    myWebView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
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

    LaunchedEffect(isPageReady, pdfUrl) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady && window.YabaPdfBridge.isReady()); } catch(e){ return false; } })();",
        )
        if (!ready) {
            Log.w(TAG, "PDF bridge not ready before timeout")
            return@LaunchedEffect
        }

        val resolvedPdfUrl = toInternalStorageAssetLoaderFileUrl(context, pdfUrl) ?: pdfUrl
        val escapedPdfUrl = escapeJsString(resolvedPdfUrl)
        val script = """
            (function() {
                try {
                    if (window.YabaPdfBridge && window.YabaPdfBridge.setPdfUrl) {
                        window.YabaPdfBridge.setPdfUrl('$escapedPdfUrl');
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }

    LaunchedEffect(isPageReady, platform, appearance) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady && window.YabaPdfBridge.isReady()); } catch(e){ return false; } })();",
        )
        if (!ready) return@LaunchedEffect
        val script = """
            (function() {
                try {
                    if (window.YabaPdfBridge && window.YabaPdfBridge.setPlatform) {
                        window.YabaPdfBridge.setPlatform('${platform.toJsValue()}');
                    }
                    if (window.YabaPdfBridge && window.YabaPdfBridge.setAppearance) {
                        window.YabaPdfBridge.setAppearance('${appearance.toJsValue()}');
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }

    val readerBridge = remember(webViewRef) {
        object : WebViewReaderBridge {
            override suspend fun getSelectionSnapshot(
                bookmarkId: String,
                readableVersionId: String,
            ): ReadableSelectionDraft? {
                val webView = webViewRef.value ?: return null
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady && window.YabaPdfBridge.isReady()); } catch(e){ return false; } })();",
                )
                if (!ready) return null
                val script = """
                    (function() {
                        try {
                            var snap = window.YabaPdfBridge.getSelectionSnapshot();
                            if (!snap) return null;
                            return JSON.stringify(snap);
                        } catch(e) { return null; }
                    })();
                """.trimIndent()
                val raw = evaluateJs(webView, script)
                val jsonStr = decodeJsStringResult(raw)
                if (jsonStr == "null" || jsonStr.isBlank()) return null
                return runCatching {
                    val json = JSONObject(jsonStr)
                    val startSectionKey = json.optString("startSectionKey", "")
                    val startOffsetInSection = json.optInt("startOffsetInSection", 0)
                    val endSectionKey = json.optString("endSectionKey", "")
                    val endOffsetInSection = json.optInt("endOffsetInSection", 0)
                    val selectedText = json.optString("selectedText", "")
                    val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
                    val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }
                    if (startSectionKey.isBlank() || endSectionKey.isBlank() || selectedText.isBlank()) return@runCatching null
                    ReadableSelectionDraft(
                        sourceContext = HighlightSourceContext.readable(bookmarkId, readableVersionId),
                        anchor = ReadableAnchor(
                            readableVersionId = readableVersionId,
                            startSectionKey = startSectionKey,
                            startOffsetInSection = startOffsetInSection,
                            endSectionKey = endSectionKey,
                            endOffsetInSection = endOffsetInSection,
                        ),
                        quote = HighlightQuoteSnapshot(
                            selectedText = selectedText,
                            prefixText = prefixText,
                            suffixText = suffixText,
                        ),
                    )
                }.getOrNull()
            }

            override suspend fun getCanCreateHighlight(): Boolean {
                val webView = webViewRef.value ?: return false
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return false
                val script = """
                    (function() {
                        try {
                            return !!(window.YabaPdfBridge && window.YabaPdfBridge.getCanCreateHighlight && window.YabaPdfBridge.getCanCreateHighlight());
                        } catch(e) { return false; }
                    })();
                """.trimIndent()
                return evaluateJs(webView, script).trim() == "true"
            }

            override suspend fun setHighlights(highlights: List<HighlightUiModel>) {
                val webView = webViewRef.value ?: return
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return
                val arr = JSONArray()
                highlights.forEach { highlight ->
                    arr.put(
                        JSONObject().apply {
                            put("id", highlight.id)
                            put("startSectionKey", highlight.startSectionKey)
                            put("startOffsetInSection", highlight.startOffsetInSection)
                            put("endSectionKey", highlight.endSectionKey)
                            put("endOffsetInSection", highlight.endOffsetInSection)
                            put("colorRole", highlight.colorRole.name)
                        },
                    )
                }
                val escaped = escapeJsString(arr.toString())
                val script = """
                    (function() {
                        try {
                            if (window.YabaPdfBridge && window.YabaPdfBridge.setHighlights) {
                                window.YabaPdfBridge.setHighlights('$escaped');
                            }
                        } catch(e) {}
                    })();
                """.trimIndent()
                evaluateJs(webView, script)
            }

            override suspend fun scrollToHighlight(highlightId: String) {
                val webView = webViewRef.value ?: return
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return
                val escaped = escapeJsString(highlightId)
                val script = """
                    (function() {
                        try {
                            if (window.YabaPdfBridge && window.YabaPdfBridge.scrollToHighlight) {
                                window.YabaPdfBridge.scrollToHighlight('$escaped');
                            }
                        } catch(e) {}
                    })();
                """.trimIndent()
                evaluateJs(webView, script)
            }

            override suspend fun getPageCount(): Int {
                val webView = webViewRef.value ?: return 0
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return 0
                val script = "(function(){ try { return window.YabaPdfBridge?.getPageCount?.() ?? 0; } catch(e){ return 0; } })();"
                return try {
                    evaluateJs(webView, script).trim().toIntOrNull() ?: 0
                } catch (_: Exception) { 0 }
            }

            override suspend fun getCurrentPageNumber(): Int {
                val webView = webViewRef.value ?: return 1
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return 1
                val script = "(function(){ try { return window.YabaPdfBridge?.getCurrentPageNumber?.() ?? 1; } catch(e){ return 1; } })();"
                return try {
                    evaluateJs(webView, script).trim().toIntOrNull() ?: 1
                } catch (_: Exception) { 1 }
            }

            override suspend fun nextPage(): Boolean {
                val webView = webViewRef.value ?: return false
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return false
                val script = "(function(){ try { return window.YabaPdfBridge?.nextPage?.() ?? false; } catch(e){ return false; } })();"
                return evaluateJs(webView, script).trim() == "true"
            }

            override suspend fun prevPage(): Boolean {
                val webView = webViewRef.value ?: return false
                val ready = waitForBridgeReady(
                    webView,
                    "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
                )
                if (!ready) return false
                val script = "(function(){ try { return window.YabaPdfBridge?.prevPage?.() ?? false; } catch(e){ return false; } })();"
                return evaluateJs(webView, script).trim() == "true"
            }
        }
    }

    LaunchedEffect(isPageReady, readerBridge) {
        if (isPageReady) onBridgeReady(readerBridge)
    }

    LaunchedEffect(isPageReady) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
        )
        if (!ready) return@LaunchedEffect
        val script = """
            (function() {
                if (window.YabaPdfBridge) {
                    window.YabaPdfBridge.onHighlightTap = function(id) {
                        if (id) window.location = "yaba://highlight-tap?id=" + encodeURIComponent(id);
                    };
                }
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }

    LaunchedEffect(isPageReady, highlights) {
        if (!isPageReady) return@LaunchedEffect
        val webView = webViewRef.value ?: return@LaunchedEffect
        val ready = waitForBridgeReady(
            webView,
            "(function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();",
        )
        if (!ready) return@LaunchedEffect
        val arr = JSONArray()
        highlights.forEach { highlight ->
            arr.put(
                JSONObject().apply {
                    put("id", highlight.id)
                    put("startSectionKey", highlight.startSectionKey)
                    put("startOffsetInSection", highlight.startOffsetInSection)
                    put("endSectionKey", highlight.endSectionKey)
                    put("endOffsetInSection", highlight.endOffsetInSection)
                    put("colorRole", highlight.colorRole.name)
                },
            )
        }
        val escaped = escapeJsString(arr.toString())
        val script = """
            (function() {
                try {
                    if (window.YabaPdfBridge && window.YabaPdfBridge.setHighlights) {
                        window.YabaPdfBridge.setHighlights('$escaped');
                    }
                } catch(e) {}
            })();
        """.trimIndent()
        evaluateJs(webView, script)
    }
}
