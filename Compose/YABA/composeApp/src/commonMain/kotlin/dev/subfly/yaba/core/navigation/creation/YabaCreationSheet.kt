package dev.subfly.yaba.core.navigation.creation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun YabaCreationSheet(modifier: Modifier = Modifier) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val sheetState = rememberModalBottomSheetState()
    val appState by appStateManager.state.collectAsState()

    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible.not()) {
            creationNavigator.removeIf { it !is EmptyRoute }
        }
    }

    AnimatedBottomSheet(
        modifier = modifier,
        isVisible = appState.showCreationSheet,
        sheetState = sheetState,
        showDragHandle = true,
        onDismissRequest = appStateManager::onHideCreationContent,
    ) { YabaCreationNavigationView() }
}
