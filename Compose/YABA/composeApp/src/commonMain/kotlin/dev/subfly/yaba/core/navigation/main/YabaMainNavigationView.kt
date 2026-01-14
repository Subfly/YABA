package dev.subfly.yaba.core.navigation.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.subfly.yaba.ui.detail.EmptyDetailView
import dev.subfly.yaba.ui.detail.folder.FolderDetailView
import dev.subfly.yaba.ui.detail.tag.TagDetailView
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
        transitionSpec = {
            val spec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
            slideInHorizontally(
                animationSpec = spec,
                initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                    slideOutHorizontally(
                        animationSpec = spec,
                        targetOffsetX = { fullWidth -> -(fullWidth / 3) })
        },
        popTransitionSpec = {
            val spec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
            slideInHorizontally(
                animationSpec = spec,
                initialOffsetX = { fullWidth -> -(fullWidth / 3) }) togetherWith
                    slideOutHorizontally(
                        animationSpec = spec,
                        targetOffsetX = { fullWidth -> fullWidth })
        },
        predictivePopTransitionSpec = {
            val spec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
            slideInHorizontally(
                animationSpec = spec,
                initialOffsetX = { fullWidth -> -(fullWidth / 3) }) togetherWith
                    slideOutHorizontally(
                        animationSpec = spec,
                        targetOffsetX = { fullWidth -> fullWidth })
        },
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
            entry<FolderDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                FolderDetailView(folderId = key.folderId)
            }
            entry<TagDetailRoute>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                TagDetailView(tagId = key.tagId)
            }
        }
    )
}
