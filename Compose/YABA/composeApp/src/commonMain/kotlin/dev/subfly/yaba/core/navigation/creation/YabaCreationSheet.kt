package dev.subfly.yaba.core.navigation.creation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.subfly.yaba.core.components.AnimatedBottomSheet
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun YabaCreationSheet(modifier: Modifier = Modifier) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appState by appStateManager.state.collectAsState()

    var wasSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(sheetState.isVisible) {
        if (sheetState.isVisible) {
            wasSheetVisible = true
            return@LaunchedEffect
        }

        // Only clean up after the sheet has actually been shown once.
        // (ShareActivity opens the sheet immediately on first composition; this avoids a race where
        // initial cleanup can remove the just-pushed route before the sheet becomes visible.)
        if (wasSheetVisible) {
            creationNavigator.removeAll { it !is EmptyCretionRoute }
            resultStore.cleanUp()
        }
    }

    AnimatedBottomSheet(
        modifier = modifier,
        isVisible = appState.showCreationContent,
        sheetState = sheetState,
        onDismissRequest = appStateManager::onHideCreationContent,
    ) { YabaCreationNavigationView() }
}
