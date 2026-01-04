package dev.subfly.yaba.core.app

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import dev.subfly.yaba.core.navigation.creation.YabaCreationSheet
import dev.subfly.yaba.core.navigation.creation.EmptyRoute
import dev.subfly.yaba.core.navigation.YabaNavigator
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.alert.YabaDeletionDialog
import dev.subfly.yaba.core.navigation.creation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.creation.rememberResultStore
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    val userPreferences by SettingsStores.userPreferences.preferencesFlow.collectAsState(
        UserPreferences()
    )

    val appVM = viewModel<AppVM>()
    val deletionVM = viewModel<DeletionVM>()

    val navigationResultStore = rememberResultStore()
    val creationNavigator = rememberNavBackStack(
        configuration = creationNavigationConfig,
        EmptyRoute
    )

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
        LocalAppStateManager provides appVM,
        LocalDeletionDialogManager provides deletionVM,
    ) {
        YabaTheme {
            YabaNavigator()
            YabaCreationSheet()
            YabaDeletionDialog()
        }
    }
}
