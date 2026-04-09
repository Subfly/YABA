package dev.subfly.yaba.core.webview

/**
 * Option section IDs emitted by the canvas web bridge — must match `OPTION_GROUP` in `canvas-bridge.ts`.
 */
object CanvasOptionGroups {
    const val STROKE = "stroke"
    const val BACKGROUND = "background"
    /** Fill pattern (hachure, etc.) — show in native UI only when a non-transparent fill is active. */
    const val FILL_TYPE = "fillType"
    const val STROKE_WIDTH = "strokeWidth"
    const val STROKE_STYLE = "strokeStyle"
    const val SLOPPINESS = "sloppiness"
    const val EDGES = "edges"
    const val FONT_SIZE = "fontSize"
    const val OPACITY = "opacity"
    const val LAYERS = "layers"
    const val DELETE = "delete"
    const val ARROW_TYPE = "arrowType"
    const val START_ARROWHEAD = "startArrowhead"
    const val END_ARROWHEAD = "endArrowhead"
}

/** Default picker order — aligned with `EXCALI_ARROWHEAD_PICKER_ORDER` in `canvas-bridge.ts`. */
val DefaultExcalidrawArrowheadPickerOrder: List<String> =
    listOf(
        "none",
        "arrow",
        "triangle",
        "triangle_outline",
        "dot",
        "circle",
        "circle_outline",
        "diamond",
        "diamond_outline",
        "bar",
        "crowfoot_one",
        "crowfoot_many",
        "crowfoot_one_or_many",
    )
