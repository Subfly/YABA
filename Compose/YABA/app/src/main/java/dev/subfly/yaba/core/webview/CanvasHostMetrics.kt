package dev.subfly.yaba.core.webview

data class CanvasHostMetrics(
    val activeTool: String = "selection",
    val hasSelection: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)
