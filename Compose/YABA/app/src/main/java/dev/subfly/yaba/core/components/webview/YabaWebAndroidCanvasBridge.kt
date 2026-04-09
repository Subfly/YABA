package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yaba.core.webview.WebViewCanvasBridge
import dev.subfly.yaba.core.webview.YabaCanvasBridgeScripts
import dev.subfly.yaba.core.webview.YabaWebBridgeScripts

@Suppress("FunctionName")
internal fun CanvasWebViewBridge(
    webView: WebView,
): WebViewCanvasBridge = object : WebViewCanvasBridge {
    override suspend fun getSceneJson(): String {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY)) return ""
        val raw = evaluateJs(webView, YabaCanvasBridgeScripts.GET_SCENE_JSON_SCRIPT)
        return decodeJsStringResult(raw)
    }

    override suspend fun setSceneJson(sceneJson: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.setSceneJsonScript(sceneJson))
    }

    override suspend fun setActiveTool(tool: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.setActiveToolScript(tool))
    }

    override suspend fun undo() {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.UNDO_SCRIPT)
    }

    override suspend fun redo() {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.REDO_SCRIPT)
    }

    override suspend fun deleteSelected() {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.DELETE_SELECTED_SCRIPT)
    }

    override suspend fun insertImageFromDataUrl(dataUrl: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.insertImageFromDataUrlScript(dataUrl))
    }

    override suspend fun applySelectionStyle(json: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.applySelectionStyleScript(json))
    }

    override suspend fun canvasLayer(action: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.CANVAS_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaCanvasBridgeScripts.canvasLayerScript(action))
    }
}
