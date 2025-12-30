package dev.subfly.yabacore.ui.icon

import dev.subfly.yabacore.model.utils.YabaColor

actual fun YabaColor.iconTintArgb(): Long =
    when (this) {
        YabaColor.NONE -> 0x00000000
        YabaColor.BLUE -> 0xFF0000FF
        YabaColor.BROWN -> 0xFFA52A2A
        YabaColor.CYAN -> 0xFF00FFFF
        YabaColor.GRAY -> 0xFF888888
        YabaColor.GREEN -> 0xFF00FF00
        YabaColor.INDIGO -> 0xFF4B0082
        YabaColor.MINT -> 0xFF98FF98
        YabaColor.ORANGE -> 0xFFFFA500
        YabaColor.PINK -> 0xFFFFC0CB
        YabaColor.PURPLE -> 0xFF800080
        YabaColor.RED -> 0xFFFF0000
        YabaColor.TEAL -> 0xFF008080
        YabaColor.YELLOW -> 0xFFFFFF00
    }
