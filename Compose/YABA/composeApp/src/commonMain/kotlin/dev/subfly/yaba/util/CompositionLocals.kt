package dev.subfly.yaba.util

import androidx.compose.runtime.compositionLocalOf
import dev.subfly.yabacore.preferences.UserPreferences

val LocalUserPreferences = compositionLocalOf { UserPreferences() }