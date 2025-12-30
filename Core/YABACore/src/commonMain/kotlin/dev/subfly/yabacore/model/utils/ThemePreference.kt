package dev.subfly.yabacore.model.utils

enum class ThemePreference {
    LIGHT,
    DARK,
    SYSTEM,
}

fun ThemePreference.uiIconName(): String =
    when (this) {
        ThemePreference.LIGHT -> "sun-03"
        ThemePreference.DARK -> "moon-02"
        ThemePreference.SYSTEM -> "smart-phone-02"
    }

expect fun ThemePreference.uiTitle(): String
