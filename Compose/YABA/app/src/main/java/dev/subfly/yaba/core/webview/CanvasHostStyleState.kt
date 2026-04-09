package dev.subfly.yaba.core.webview

/**
 * Selection-driven Excalidraw style snapshot from the canvas WebView (`canvasStyleState` host payload).
 */
data class CanvasHostStyleState(
    val hasSelection: Boolean = false,
    val selectionCount: Int = 0,
    val strokeYabaCode: Int = 0,
    val backgroundYabaCode: Int = 0,
    val strokeWidthKey: String = "thin",
    val strokeStyle: String = "solid",
    val roughnessKey: String = "architect",
    val edgeKey: String = "sharp",
    val fontSizeKey: String = "M",
    /** 0–10 → Excalidraw opacity 0–100 in steps of 10 */
    val opacityStep: Int = 10,
    val mixedStroke: Boolean = false,
    val mixedBackground: Boolean = false,
    val mixedStrokeWidth: Boolean = false,
    val mixedStrokeStyle: Boolean = false,
    val mixedRoughness: Boolean = false,
    val mixedEdge: Boolean = false,
    val mixedFontSize: Boolean = false,
    val mixedOpacity: Boolean = false,
)
