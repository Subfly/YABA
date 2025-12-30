package dev.subfly.yaba.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.ui.home.components.HomeFab
import dev.subfly.yaba.ui.home.components.HomeTopBar
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.YabaContentLayout

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeView(
    modifier: Modifier = Modifier,
    onShowSheet: (NavKey) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                scrollBehavior = scrollBehavior,
                onOptionsClicked = {
                    // TODO: NAVIGATE TO SEARCH
                }
            )
        },
        floatingActionButtonPosition = when (userPreferences.preferredFabPosition) {
            FabPosition.LEFT -> androidx.compose.material3.FabPosition.Start
            FabPosition.RIGHT -> androidx.compose.material3.FabPosition.End
            FabPosition.CENTER -> androidx.compose.material3.FabPosition.Center
        },
        floatingActionButton = { HomeFab(showSheetWithRoute = onShowSheet) }
    ) { paddings ->
        YabaContentLayout(
            appearance = userPreferences.preferredContentAppearance,
            layoutConfig = ContentLayoutConfig(
                appearance = userPreferences.preferredContentAppearance,
            ),
            contentPadding = paddings,
            content = {

            },
        )
    }
}
