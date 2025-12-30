package dev.subfly.yaba

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.subfly.yaba.core.navigation.YabaNavigator
import dev.subfly.yaba.core.theme.YabaTheme
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

    val fileSettingsStore = FileSystemSettings.store

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
    ) {
        YabaTheme {
            YabaNavigator()
        }
    }
}
