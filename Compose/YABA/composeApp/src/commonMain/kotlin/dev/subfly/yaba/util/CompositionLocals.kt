package dev.subfly.yaba.util

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.core.app.AppVM
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.creation.ResultStore
import dev.subfly.yabacore.preferences.UserPreferences

val LocalUserPreferences = compositionLocalOf { UserPreferences() }
val LocalResultStore = compositionLocalOf { ResultStore() }
val LocalCreationContentNavigator = compositionLocalOf { NavBackStack<NavKey>() }

val LocalAppStateManager = compositionLocalOf { AppVM() }

val LocalDeletionDialogManager = compositionLocalOf { DeletionVM() }
