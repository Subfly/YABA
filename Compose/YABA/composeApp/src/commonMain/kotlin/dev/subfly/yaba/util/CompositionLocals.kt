package dev.subfly.yaba.util

import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.core.app.AppVM
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.creation.ResultStore
import dev.subfly.yabacore.preferences.UserPreferences

val LocalUserPreferences = compositionLocalOf<UserPreferences> {
    error("No user preferences provider provided")
}

val LocalResultStore = compositionLocalOf<ResultStore> {
    error("No result store provided")
}

val LocalCreationContentNavigator = compositionLocalOf<NavBackStack<NavKey>> {
    error("No creation navigator provided")
}

val LocalContentNavigator = compositionLocalOf<NavBackStack<NavKey>> {
    error("No detail navigator provided")
}

val LocalAppStateManager = compositionLocalOf<AppVM> {
    error("No app state manager provided")
}

val LocalDeletionDialogManager = compositionLocalOf<DeletionVM> {
    error("No deletion manager provided")
}
