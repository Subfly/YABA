package dev.subfly.yaba.core.components.webview

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Collections

/**
 * Hardened settings and permissions are taken from: https://github.com/GrapheneOS/PdfViewer
 * GrapheneOS PdfViewer-aligned hardening for packaged WebView content served via
 * [androidx.webkit.WebViewAssetLoader] on `https://appassets.androidplatform.net`.
 *
 * @see <a href="https://github.com/GrapheneOS/PdfViewer">GrapheneOS PdfViewer</a>
 */
internal object YabaWebAndroidSecurity {

    /**
     * Stricter than the stock PdfViewer HTML shell where possible, while still allowing
     * Vite-bundled readers (wasm workers, inline styles from component libs, blob/data media).
     *
     * EPUB.js renders spine HTML in `about:srcdoc` iframes and sets `<base href>` for relative
     * OPS paths; it also loads layout stylesheets via `blob:` URLs, and injected `@font-face`
     * rules load font files as `blob:` URLs. Those require [base-uri 'self'] (not `'none'`),
     * and `blob:` on [style-src] and [font-src].
     */
    const val CONTENT_SECURITY_POLICY: String =
        "default-src 'none'; " +
            "form-action 'none'; " +
            "connect-src 'self' blob:; " +
            "img-src 'self' blob: data:; " +
            "script-src 'self' 'wasm-unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline' blob:; " +
            "font-src 'self' data: blob:; " +
            "worker-src 'self' blob:; " +
            "media-src 'self' blob:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'"

    /** Mirrors GrapheneOS PdfViewer [Permissions-Policy] (all sensitive features denied). */
    const val PERMISSIONS_POLICY: String =
        "accelerometer=(), " +
            "ambient-light-sensor=(), " +
            "autoplay=(), " +
            "battery=(), " +
            "camera=(), " +
            "clipboard-read=(), " +
            "clipboard-write=(), " +
            "display-capture=(), " +
            "document-domain=(), " +
            "encrypted-media=(), " +
            "fullscreen=(), " +
            "gamepad=(), " +
            "geolocation=(), " +
            "gyroscope=(), " +
            "hid=(), " +
            "idle-detection=(), " +
            "interest-cohort=(), " +
            "magnetometer=(), " +
            "microphone=(), " +
            "midi=(), " +
            "payment=(), " +
            "picture-in-picture=(), " +
            "publickey-credentials-get=(), " +
            "screen-wake-lock=(), " +
            "serial=(), " +
            "speaker-selection=(), " +
            "sync-xhr=(), " +
            "usb=(), " +
            "xr-spatial-tracking=()"

    private val emptyHeaders: Map<String, String> = Collections.emptyMap()

    internal fun blockedResponse(statusCode: Int, reasonPhrase: String): WebResourceResponse {
        val body = ByteArrayInputStream(ByteArray(0))
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            statusCode,
            reasonPhrase,
            emptyHeaders,
            body,
        )
    }

    /**
     * Subresource's that must keep WebView default handling (not served via asset loader).
     */
    internal fun isHandledByWebViewInternally(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return true
        return when (scheme) {
            "about", "data", "blob", "javascript" -> true
            else -> false
        }
    }

    internal fun applySecurityHeadersIfHtml(response: WebResourceResponse): WebResourceResponse {
        val mime = response.mimeType ?: return response
        val baseMime = mime.split(";", limit = 2).firstOrNull()?.trim()?.lowercase() ?: return response
        if (baseMime != "text/html") return response

        val merged = LinkedHashMap<String, String>()
        response.responseHeaders?.entries?.forEach { entry ->
            merged[entry.key] = entry.value
        }
        merged["Content-Security-Policy"] = CONTENT_SECURITY_POLICY
        merged["Permissions-Policy"] = PERMISSIONS_POLICY
        merged["X-Content-Type-Options"] = "nosniff"
        response.responseHeaders = merged
        return response
    }
}
