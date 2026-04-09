package dev.subfly.yaba.core.webview

import org.json.JSONObject

/**
 * Partial style update sent to `window.YabaCanvasBridge.applySelectionStyle` (JSON), aligned with
 * `ApplySelectionStylePayload` in `canvas-bridge.ts`.
 */
data class CanvasSelectionStylePatch(
    val strokeYabaCode: Int? = null,
    val backgroundYabaCode: Int? = null,
    val strokeWidthKey: String? = null,
    val strokeStyle: String? = null,
    val roughnessKey: String? = null,
    val edgeKey: String? = null,
    val fontSizeKey: String? = null,
    val opacityStep: Int? = null,
) {
    fun toJsonString(): String {
        val o = JSONObject()
        strokeYabaCode?.let { if (it > 0) o.put("strokeYabaCode", it) }
        backgroundYabaCode?.let { o.put("backgroundYabaCode", it) }
        strokeWidthKey?.let { o.put("strokeWidthKey", it) }
        strokeStyle?.let { o.put("strokeStyle", it) }
        roughnessKey?.let { o.put("roughnessKey", it) }
        edgeKey?.let { o.put("edgeKey", it) }
        fontSizeKey?.let { o.put("fontSizeKey", it) }
        opacityStep?.let { o.put("opacityStep", it) }
        return o.toString()
    }
}
