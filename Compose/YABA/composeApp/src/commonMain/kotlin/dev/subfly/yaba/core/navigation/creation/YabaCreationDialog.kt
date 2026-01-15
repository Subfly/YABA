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
import dev.subfly.yaba.util.LocalResultStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YabaCreationDialog() {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val appState by appStateManager.state.collectAsState()

    LaunchedEffect(appState.showCreationContent) {
        if (appState.showCreationContent.not()) {
            // Just in case, some delay
            delay(100)
            creationNavigator.removeAll { it !is EmptyCretionRoute }
            resultStore.cleanUp()
        }
    }

    if (appState.showCreationContent) {
        BasicAlertDialog(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            onDismissRequest = appStateManager::onShowCreationContent
        ){ YabaCreationNavigationView() }
    }
}
