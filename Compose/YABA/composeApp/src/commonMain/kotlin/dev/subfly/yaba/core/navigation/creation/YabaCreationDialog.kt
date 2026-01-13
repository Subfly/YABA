package dev.subfly.yaba.core.navigation.creation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YabaCreationDialog() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    val appState by appStateManager.state.collectAsState()

    LaunchedEffect(appState.showCreationContent) {
        if (appState.showCreationContent.not()) {
            // Just in case, some delay
            delay(100)
            creationNavigator.removeIf { it !is EmptyCretionRoute }
        }
    }

    if (appState.showCreationContent) {
        BasicAlertDialog(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            onDismissRequest = appStateManager::onShowCreationContent
        ){ YabaCreationNavigationView() }
    }
}
