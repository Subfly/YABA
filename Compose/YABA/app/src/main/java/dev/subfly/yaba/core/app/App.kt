package dev.subfly.yaba.core.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.window.core.layout.WindowSizeClass
import dev.subfly.yaba.core.components.toast.YabaToastHost
import dev.subfly.yaba.core.deeplink.DeepLinkManager
import dev.subfly.yaba.core.deeplink.DeepLinkTarget
import dev.subfly.yaba.core.navigation.alert.DeletionVM
import dev.subfly.yaba.core.navigation.alert.YabaDeletionDialog
import dev.subfly.yaba.core.navigation.creation.EmptyCreationRoute
import dev.subfly.yaba.core.navigation.creation.YabaCreationDialog
import dev.subfly.yaba.core.navigation.creation.YabaCreationSheet
import dev.subfly.yaba.core.navigation.creation.creationNavigationConfig
import dev.subfly.yaba.core.navigation.creation.rememberResultStore
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.CanvasDetailRoute
import dev.subfly.yaba.core.navigation.main.HomeRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.core.navigation.main.YabaMainNavigationView
import dev.subfly.yaba.core.navigation.main.detailNavigationConfig
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalPaneInfo
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.rememberPaneLayoutInfo
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.preferences.SettingsStores
import dev.subfly.yaba.core.preferences.UserPreferences

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3AdaptiveApi::class
)
@Composable
fun App() {
    val userPreferences by SettingsStores.userPreferences.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )

    val appVM = viewModel { AppVM() }
    val deletionVM = viewModel { DeletionVM() }

    val navigationResultStore = rememberResultStore()
    val creationNavigator = rememberNavBackStack(
        configuration = creationNavigationConfig,
        EmptyCreationRoute
    )
    val contentNavigator = rememberNavBackStack(
        configuration = detailNavigationConfig,
        HomeRoute()
    )

    val currentWindowInfo = currentWindowAdaptiveInfo()
    val paneInfo = rememberPaneLayoutInfo()

    val pendingTarget by DeepLinkManager.pendingTarget.collectAsState()
    LaunchedEffect(pendingTarget) {
        when (val target = pendingTarget) {
            is DeepLinkTarget.BookmarkDetail -> {
                val route = when (BookmarkKind.fromCode(target.bookmarkKindCode)) {
                    BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = target.bookmarkId)
                    BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = target.bookmarkId)
                    BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = target.bookmarkId)
                    BookmarkKind.FILE -> DocDetailRoute(bookmarkId = target.bookmarkId)
                    BookmarkKind.CANVAS -> CanvasDetailRoute(bookmarkId = target.bookmarkId)
                }
                contentNavigator.add(route)
                DeepLinkManager.consume()
            }
            null -> {}
        }
    }

    CompositionLocalProvider(
        LocalUserPreferences provides userPreferences,
        LocalResultStore provides navigationResultStore,
        LocalCreationContentNavigator provides creationNavigator,
        LocalContentNavigator provides contentNavigator,
        LocalAppStateManager provides appVM,
        LocalDeletionDialogManager provides deletionVM,
        LocalPaneInfo provides paneInfo,
    ) {
        YabaTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                YabaMainNavigationView()
                if (
                    currentWindowInfo.windowSizeClass.isWidthAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    ).not()
                ) {
                    YabaCreationSheet()
                } else {
                    YabaCreationDialog()
                }
                YabaDeletionDialog()
                YabaToastHost()
            }
        }
    }
}
