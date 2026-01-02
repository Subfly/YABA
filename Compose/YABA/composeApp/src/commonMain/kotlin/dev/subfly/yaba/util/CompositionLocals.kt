package dev.subfly.yaba.util

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.core.navigation.ResultStore
import dev.subfly.yabacore.preferences.UserPreferences

val LocalUserPreferences = compositionLocalOf { UserPreferences() }
val LocalResultStore = compositionLocalOf { ResultStore() }
val LocalCreationContentNavigator = compositionLocalOf { NavBackStack<NavKey>() }
