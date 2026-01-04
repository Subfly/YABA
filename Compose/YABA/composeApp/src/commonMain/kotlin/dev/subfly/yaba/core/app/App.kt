package dev.subfly.yaba.core.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.window.core.layout.WindowSizeClass
import dev.subfly.yaba.core.navigation.creation.YabaCreationSheet
import dev.subfly.yaba.core.navigation.creation.EmptyRoute
import dev.subfly.yaba.core.navigation.YabaNavigator
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.alert.YabaDeletionDialog
import dev.subfly.yaba.core.navigation.creation.YabaCreationDialog
import dev.subfly.yaba.core.navigation.creation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.creation.rememberResultStore
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        EmptyRoute
    )

    val currentWindowInfo = currentWindowAdaptiveInfo()

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
        LocalAppStateManager provides appVM,
        LocalDeletionDialogManager provides deletionVM,
    ) {
        YabaTheme {
            Box(
                modifier = Modifier.clip(
                    shape = when (Platform) {
                        YabaPlatform.JVM -> RoundedCornerShape(16.dp)
                        YabaPlatform.ANDROID -> RoundedCornerShape(0.dp)
                    }
                )
            ) {
                YabaNavigator()
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
}
