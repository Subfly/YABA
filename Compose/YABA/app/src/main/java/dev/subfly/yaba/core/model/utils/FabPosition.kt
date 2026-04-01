package dev.subfly.yaba.core.model.utils

import kotlinx.serialization.Serializable

@Serializable
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
