package dev.subfly.yabacore.model

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

    companion object {
        fun fromCode(code: Int): YabaColor = entries.firstOrNull { it.code == code } ?: NONE
    }
}
