package dev.subfly.yaba.core.components.webview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONTokener
import kotlin.coroutines.resume
import dev.subfly.yabacore.webview.MathTapEvent
import dev.subfly.yabacore.webview.YabaWebBridgeScripts

/**
 * Hardened settings and permissions are taken from:
 * https://github.com/GrapheneOS/PdfViewer
 */

internal const val YABA_WEBVIEW_LOG_TAG = "YabaWebView"

private const val ASSET_LOADER_DOMAIN = "https://appassets.androidplatform.net"
private const val ASSET_LOADER_HOST = "appassets.androidplatform.net"
internal const val BRIDGE_READY_POLL_MS = 50L
internal const val BRIDGE_READY_TIMEOUT_MS = 5_000L

internal fun toAssetLoaderUrl(rawUrl: String): String? {
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

internal fun isAssetLoaderRequest(uri: Uri?): Boolean = uri?.host == ASSET_LOADER_HOST

private fun normalizeTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"

internal fun toInternalStorageAssetLoaderBaseUrl(
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

internal fun toInternalStorageAssetLoaderFileUrl(
    context: Context,
    rawFileUrl: String?,
): String? {
    if (rawFileUrl.isNullOrBlank()) return null
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

/**
 * Decodes the value returned by [WebView.evaluateJavascript], which is JSON-encoded.
 *
 * The previous implementation only unescaped `\"` and `\\\\`, not JSON escapes like `\n`.
 * That left literal backslash-n in JSON strings returned from [WebView.evaluateJavascript],
 * which could corrupt persisted document JSON if not fully decoded.
 */
internal fun decodeJsStringResult(value: String?): String {
    if (value == null) return ""
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed == "null" || trimmed == "undefined") return ""
    return try {
        when (val parsed = JSONTokener(trimmed).nextValue()) {
            is String -> parsed
            else -> trimmed
        }
    } catch (_: Exception) {
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
        }
        return trimmed
    }
}

internal suspend fun evaluateJs(webView: WebView, script: String): String =
    suspendCancellableCoroutine { cont ->
        webView.evaluateJavascript(script) { value ->
            if (cont.isActive) cont.resume(value ?: "null")
        }
    }

internal suspend fun waitForBridgeReady(webView: WebView, bridgeReadyCheckScript: String): Boolean {
    val attempts = (BRIDGE_READY_TIMEOUT_MS / BRIDGE_READY_POLL_MS).toInt()
    repeat(attempts) {
        val ready = evaluateJs(webView, bridgeReadyCheckScript).trim() == "true"
        if (ready) return true
        delay(BRIDGE_READY_POLL_MS)
    }
    return false
}

internal fun rememberAssetLoader(context: Context, includeLocalStorage: Boolean): WebViewAssetLoader {
    val builder = WebViewAssetLoader.Builder()
    if (includeLocalStorage) {
        builder.addPathHandler(
            "/local/",
            WebViewAssetLoader.InternalStoragePathHandler(context, context.filesDir),
        )
    }
    return builder.addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context)).build()
}

@SuppressLint("SetJavaScriptEnabled")
internal fun applyHardenedWebSettings(
    webView: WebView,
    allowZoom: Boolean = false,
) {
    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.blockNetworkLoads = true
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.setSupportZoom(allowZoom)
    settings.builtInZoomControls = allowZoom
    settings.displayZoomControls = false
    CookieManager.getInstance().setAcceptCookie(false)
}

internal fun yabaWebViewClient(
    assetLoader: WebViewAssetLoader,
    onPageStarted: () -> Unit,
    onPageFinished: () -> Unit,
    onRenderProcessGone: () -> Boolean,
    onUrlClick: ((String) -> Boolean)?,
    onAnnotationTap: ((String) -> Unit)?,
    onMathTap: ((MathTapEvent) -> Unit)?,
): WebViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
        onPageStarted()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        onPageFinished()
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        // Null request: defer to default handling. Do not call super with null — that overload is
        // ambiguous with the deprecated (WebView, String) shouldInterceptRequest on the JVM.
        val req = request ?: return null
        val url = req.url

        // blob:/data:/about:/javascript: must use WebView defaults (PdfViewer-style isolation still holds
        // for the document origin; these schemes are not remote HTTP).
        if (YabaWebAndroidSecurity.isHandledByWebViewInternally(url)) {
            return super.shouldInterceptRequest(view, req)
        }

        val method = req.method?.uppercase() ?: "GET"
        if (method != "GET") {
            return if (isAssetLoaderRequest(url)) {
                YabaWebAndroidSecurity.blockedResponse(405, "Method Not Allowed")
            } else {
                super.shouldInterceptRequest(view, req)
            }
        }

        // Deny any non-packaged HTTP(S) load explicitly (defense in depth vs [blockNetworkLoads]).
        if (!isAssetLoaderRequest(url)) {
            return YabaWebAndroidSecurity.blockedResponse(404, "Not Found")
        }

        val path = url.path ?: ""
        if (path == "/favicon.ico" || path.endsWith("/favicon.ico")) {
            return null
        }

        val loaded = try {
            assetLoader.shouldInterceptRequest(url)
        } catch (_: Exception) {
            null
        }
        if (loaded == null) {
            // No fallback to default loader for our synthetic origin — matches strict allowlisting.
            return YabaWebAndroidSecurity.blockedResponse(404, "Not Found")
        }
        return YabaWebAndroidSecurity.applySecurityHeadersIfHtml(loaded)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val uri = request?.url ?: return true
        val url = uri.toString()
        if (url.startsWith(YabaWebBridgeScripts.ANNOTATION_TAP_SCHEME_PREFIX)) {
            val id = try {
                url.toUri().getQueryParameter("id") ?: ""
            } catch (_: Exception) {
                ""
            }
            if (id.isNotBlank()) onAnnotationTap?.invoke(id)
            return true
        }
        if (url.startsWith(YabaWebBridgeScripts.MATH_TAP_SCHEME_PREFIX)) {
            val u = try {
                url.toUri()
            } catch (_: Exception) {
                return true
            }
            val kind = u.getQueryParameter("kind").orEmpty()
            val pos = u.getQueryParameter("pos")?.toIntOrNull()
            val latex = u.getQueryParameter("latex").orEmpty()
            if (pos != null) {
                onMathTap?.invoke(
                    MathTapEvent(
                        isBlock = kind == "block",
                        documentPos = pos,
                        latex = latex,
                    ),
                )
            }
            return true
        }
        // In-package navigations only; never load arbitrary external URLs inside the WebView (PdfViewer
        // always returns true — we allow same-host asset URLs for multipage / SPA edge cases).
        if (isAssetLoaderRequest(uri)) {
            return false
        }
        onUrlClick?.invoke(url)
        return true
    }

    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
        if (detail?.didCrash() == true) {
            return onRenderProcessGone()
        }
        return false
    }
}

internal fun denyPermissionsWebChromeClient(
    onProgressChanged: (Int) -> Unit,
    onConsoleMessage: (String) -> Unit = {},
): WebChromeClient = object : WebChromeClient() {
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        onProgressChanged(newProgress)
    }

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
            onConsoleMessage(consoleMessage.message())
            Log.d(
                YABA_WEBVIEW_LOG_TAG,
                "JS ${consoleMessage.messageLevel()} ${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}",
            )
        }
        return super.onConsoleMessage(consoleMessage)
    }
}
