package dev.subfly.yaba.core.components.webview

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Injected as `window.YabaNativeHost` (and Android alias `window.YabaAndroidHost`) — web calls
 * [postMessage] with JSON payloads (see [YabaNativeHostMessageParser] and
 * yaba-web-components `yaba-native-host.ts`).
 */
internal class YabaAndroidHostJsBridge(
    private val onJsonMessage: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postMessage(json: String) {
        mainHandler.post { onJsonMessage(json) }
    }
}
