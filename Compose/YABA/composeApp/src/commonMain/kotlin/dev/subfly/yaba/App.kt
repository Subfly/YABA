package dev.subfly.yaba

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.rememberNavBackStack
import dev.subfly.yaba.core.navigation.CreationSheet
import dev.subfly.yaba.core.navigation.YabaNavigator
import dev.subfly.yaba.core.navigation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.rememberResultStore
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalCreationContentNavigator
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
    val navigationResultStore = rememberResultStore()
    val creationNavigator = rememberNavBackStack(
        configuration = creationNavigationConfig
    )

    var shouldShowCreationSheet by rememberSaveable { mutableStateOf(false) }

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
    ) {
        YabaTheme {
            YabaNavigator(
                onShowSheet = { shouldShowCreationSheet = true }
            )
            CreationSheet(
                shouldShow = shouldShowCreationSheet,
                onDismiss = { shouldShowCreationSheet = false }
            )
        }
    }
}
