package dev.subfly.yaba.util

import androidx.compose.runtime.compositionLocalOf
import dev.subfly.yaba.core.navigation.ResultStore
import dev.subfly.yabacore.preferences.UserPreferences

val LocalUserPreferences = compositionLocalOf { UserPreferences() }
val LocalResultStore = compositionLocalOf { ResultStore() }
