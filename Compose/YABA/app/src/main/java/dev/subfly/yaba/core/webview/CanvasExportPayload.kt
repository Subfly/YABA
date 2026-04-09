package dev.subfly.yaba.core.webview

import org.json.JSONObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Decodes the JSON returned from [WebViewCanvasBridge.exportCanvasImage] after the Excalidraw
 * export runs in the WebView (`{ "ok": true, "base64": "..." }`).
 */
object CanvasExportPayload {
    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBytesOrNull(json: String): ByteArray? {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "null") return null
        val o = runCatching { JSONObject(trimmed) }.getOrNull() ?: return null
        if (!o.optBoolean("ok")) return null
        val b64 = o.optString("base64", "").trim()
        if (b64.isBlank()) return null
        return runCatching { Base64.decode(b64) }.getOrNull()
    }
}
