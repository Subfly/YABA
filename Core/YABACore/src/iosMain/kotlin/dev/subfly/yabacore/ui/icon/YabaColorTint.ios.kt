package dev.subfly.yabacore.ui.icon

import dev.subfly.yabacore.model.utils.YabaColor

/**
 * iOS system colors from Apple Human Interface Guidelines (Default Light mode).
 * Source: https://developer.apple.com/design/human-interface-guidelines/color
 */
actual fun YabaColor.iconTintArgb(): Long =
    when (this) {
        YabaColor.NONE -> 0x00000000
        YabaColor.BLUE -> 0xFF0088FF       // RGB(0, 136, 255)
        YabaColor.BROWN -> 0xFFAC7F5E      // RGB(172, 127, 94)
        YabaColor.CYAN -> 0xFF00C0E8       // RGB(0, 192, 232)
        YabaColor.GRAY -> 0xFF8E8E93       // RGB(142, 142, 147)
        YabaColor.GREEN -> 0xFF34C759      // RGB(52, 199, 89)
        YabaColor.INDIGO -> 0xFF6155F5     // RGB(97, 85, 245)
        YabaColor.MINT -> 0xFF00C8B3       // RGB(0, 200, 179)
        YabaColor.ORANGE -> 0xFFFF8D28     // RGB(255, 141, 40)
        YabaColor.PINK -> 0xFFFF2D55       // RGB(255, 45, 85)
        YabaColor.PURPLE -> 0xFFCB30E0     // RGB(203, 48, 224)
        YabaColor.RED -> 0xFFFF383C        // RGB(255, 56, 60)
        YabaColor.TEAL -> 0xFF00C3D0       // RGB(0, 195, 208)
        YabaColor.YELLOW -> 0xFFFFCC00     // RGB(255, 204, 0)
    }
