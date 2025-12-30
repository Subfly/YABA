package dev.subfly.yabacore.model.utils

enum class FabPosition {
    LEFT,
    RIGHT,
    CENTER,
}

fun FabPosition.uiIconName(): String =
    when (this) {
        FabPosition.LEFT -> "circle-arrow-left-03"
        FabPosition.RIGHT -> "circle-arrow-right-03"
        FabPosition.CENTER -> "circle-arrow-down-03"
    }

expect fun FabPosition.uiTitle(): String
