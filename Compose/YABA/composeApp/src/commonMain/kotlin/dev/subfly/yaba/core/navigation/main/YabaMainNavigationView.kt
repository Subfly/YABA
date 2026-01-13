package dev.subfly.yaba.core.navigation.main

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.ui.detail.EmptyDetailView
import dev.subfly.yaba.ui.home.HomeView
import dev.subfly.yaba.ui.search.SearchView
import dev.subfly.yaba.util.LocalContentNavigator

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun YabaMainNavigationView(
    modifier: Modifier = Modifier,
) {
    val navigator = LocalContentNavigator.current

    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    NavDisplay(
        modifier = modifier,
        backStack = navigator,
        sceneStrategy = listDetailStrategy,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        onBack = navigator::removeLastOrNull,
        entryProvider = entryProvider {
            entry<HomeRoute>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = { EmptyDetailView() }
                ),
            ) {
                HomeView()
            }
            entry<SearchRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchView()
            }
        }
    )
}
