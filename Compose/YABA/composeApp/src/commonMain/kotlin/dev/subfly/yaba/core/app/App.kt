package dev.subfly.yaba.core.app

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.window.core.layout.WindowSizeClass
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.alert.YabaDeletionDialog
import dev.subfly.yaba.core.navigation.creation.EmptyCretionRoute
import dev.subfly.yaba.core.navigation.creation.YabaCreationDialog
import dev.subfly.yaba.core.navigation.creation.YabaCreationSheet
import dev.subfly.yaba.core.navigation.creation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.creation.rememberResultStore
import dev.subfly.yaba.core.navigation.main.HomeRoute
import dev.subfly.yaba.core.navigation.main.YabaMainNavigationView
import dev.subfly.yaba.core.navigation.main.detailNavigationConfig
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun App() {
    val userPreferences by SettingsStores.userPreferences.preferencesFlow.collectAsState(
        UserPreferences()
    )

    val appVM = viewModel { AppVM() }
    val deletionVM = viewModel { DeletionVM() }

    val navigationResultStore = rememberResultStore()
    val creationNavigator = rememberNavBackStack(
        configuration = creationNavigationConfig,
        EmptyCretionRoute
    )
    val contentNavigator = rememberNavBackStack(
        configuration = detailNavigationConfig,
        HomeRoute()
    )

    val currentWindowInfo = currentWindowAdaptiveInfo()

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
        LocalContentNavigator provides contentNavigator,
        LocalAppStateManager provides appVM,
        LocalDeletionDialogManager provides deletionVM,
    ) {
        YabaTheme {
            YabaMainNavigationView()
            if (
                currentWindowInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                ).not()
            ) {
                YabaCreationSheet()
            } else {
                YabaCreationDialog()
            }
            YabaDeletionDialog()
        }
    }
}
