package dev.subfly.yaba

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.core.navigation.YabaNavigator
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.ui.creation.CreationSheet
import dev.subfly.yaba.ui.creation.TagCreationContent
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.filesystem.settings.FileSystemSettings
import dev.subfly.yabacore.preferences.SettingsStores
import dev.subfly.yabacore.preferences.UserPreferences

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    val userPreferences by SettingsStores.userPreferences.preferencesFlow.collectAsState(
        UserPreferences()
    )

    var shouldShowCreationSheet by rememberSaveable { mutableStateOf(false) }
    var sheetFlowStartRoute by rememberSaveable { mutableStateOf<NavKey?>(null) }

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
    ) {
        YabaTheme {
            YabaNavigator(
                onShowSheet = { startRoute ->
                    sheetFlowStartRoute = startRoute
                    shouldShowCreationSheet = true
                }
            )
            CreationSheet(
                shouldShow = shouldShowCreationSheet,
                flowStartRoute = sheetFlowStartRoute,
                onDismiss = {
                    shouldShowCreationSheet = false
                    sheetFlowStartRoute = null
                }
            )
        }
    }
}
