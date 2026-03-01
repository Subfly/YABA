package dev.subfly.yabacore.model.utils

enum class ReaderTheme {
    SYSTEM,
    DARK,
    LIGHT,
    SEPIA,
}

enum class ReaderFontSize {
    SMALL,
    MEDIUM,
    LARGE,
}

enum class ReaderLineHeight {
    NORMAL,
    RELAXED,
}

data class ReaderPreferences(
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val fontSize: ReaderFontSize = ReaderFontSize.MEDIUM,
    val lineHeight: ReaderLineHeight = ReaderLineHeight.NORMAL,
)
