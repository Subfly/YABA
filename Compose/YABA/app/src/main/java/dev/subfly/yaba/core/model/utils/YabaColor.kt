package dev.subfly.yaba.core.model.utils

/**
 * Mirrors Darwin's YabaColor mapping for cross-platform consistency. Persisted as an Int in
 * entities.
 */
enum class YabaColor(val code: Int) {
    NONE(0),
    BLUE(1),
    BROWN(2),
    CYAN(3),
    GRAY(4),
    GREEN(5),
    INDIGO(6),
    MINT(7),
    ORANGE(8),
    PINK(9),
    PURPLE(10),
    RED(11),
    TEAL(12),
    YELLOW(13);

    fun iconTintArgb(): Long =
        when (this) {
            NONE -> 0x00000000L
            BLUE -> 0xFF0088FF
            BROWN -> 0xFFAC7F5E
            CYAN -> 0xFF00C0E8
            GRAY -> 0xFF8E8E93
            GREEN -> 0xFF34C759
            INDIGO -> 0xFF6155F5
            MINT -> 0xFF00C8B3
            ORANGE -> 0xFFFF8D28
            PINK -> 0xFFFF2D55
            PURPLE -> 0xFFCB30E0
            RED -> 0xFFFF383C
            TEAL -> 0xFF00C3D0
            YELLOW -> 0xFFFFCC00
        }

    companion object {
        fun fromCode(code: Int): YabaColor = entries.firstOrNull { it.code == code } ?: NONE

        fun fromRoleString(raw: String?): YabaColor {
            val normalized = raw?.trim()?.lowercase().orEmpty()
            if (normalized.isBlank()) return NONE
            if (normalized == "default") return NONE
            return entries.firstOrNull { it.name.lowercase() == normalized } ?: NONE
        }
    }
}
