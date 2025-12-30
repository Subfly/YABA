package dev.subfly.yabacore.model.utils

enum class CardImageSizing {
    BIG,
    SMALL,
}

fun CardImageSizing.uiIconName(): String =
    when (this) {
        CardImageSizing.BIG -> "image-composition-oval"
        CardImageSizing.SMALL -> "image-composition"
    }

expect fun CardImageSizing.uiTitle(): String
