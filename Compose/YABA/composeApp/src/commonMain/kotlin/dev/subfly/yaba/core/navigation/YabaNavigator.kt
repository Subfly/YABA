package dev.subfly.yaba.core.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDragHandle
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.ui.home.HomeView

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun YabaNavigator(
    modifier: Modifier = Modifier,
    onShowSheet: () -> Unit,
) {
    val listDetailNavigator = rememberListDetailPaneScaffoldNavigator()
    val paneExpansionState = rememberPaneExpansionState()

    ListDetailPaneScaffold(
        modifier = modifier.fillMaxSize(),
        directive = listDetailNavigator.scaffoldDirective,
        value = listDetailNavigator.scaffoldValue,
        listPane = { HomeView(onShowSheet = onShowSheet) },
        detailPane = { Text("Detail") },
        paneExpansionDragHandle = { dragState ->
            val interactionSource = remember { MutableInteractionSource() }
            VerticalDragHandle(
                modifier = Modifier.paneExpansionDraggable(
                    dragState,
                    LocalMinimumInteractiveComponentSize.current,
                    interactionSource
                ),
                interactionSource = interactionSource
            )
        },
        paneExpansionState = paneExpansionState,
    )
}
