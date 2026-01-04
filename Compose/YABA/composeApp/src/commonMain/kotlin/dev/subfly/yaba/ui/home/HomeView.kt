package dev.subfly.yaba.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yaba.ui.home.components.HomeFab
import dev.subfly.yaba.ui.home.components.HomeTopBar
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.state.home.HomeEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.TagLayoutStyle
import dev.subfly.yabacore.ui.layout.YabaContentLayout
import dev.subfly.yabacore.ui.layout.YabaTagLayout
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.folders_title
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalUuidApi::class
)
@Composable
fun HomeView(modifier: Modifier = Modifier) {
    val userPreferences = LocalUserPreferences.current
    val vm = viewModel<HomeVM>()
    val state by vm.state

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        vm.onEvent(HomeEvent.OnInit)
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                HomeTopBar(
                    scrollBehavior = scrollBehavior,
                    onAppearanceChanged = { newAppearance ->
                        vm.onEvent(HomeEvent.OnChangeContentAppearance(newAppearance))
                    },
                    onSortingChanged = { newSortType ->
                        vm.onEvent(HomeEvent.OnChangeCollectionSorting(newSortType))
                    },
                    onSizingChanged = { newSizing ->
                        // TODO: FIX ON VM
                        // vm.onEvent(HomeEvent.OnChan)
                    },
                    onSearchClicked = {
                        // TODO: NAVIGATE TO SEARCH
                    }
                )
                if (state.isLoading) {
                    LinearProgressIndicator()
                }
            }
        },
        floatingActionButtonPosition = when (userPreferences.preferredFabPosition) {
            FabPosition.LEFT -> androidx.compose.material3.FabPosition.Start
            FabPosition.RIGHT -> androidx.compose.material3.FabPosition.End
            FabPosition.CENTER -> androidx.compose.material3.FabPosition.Center
        },
        floatingActionButton = { HomeFab() }
    ) { paddings ->
        YabaContentLayout(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddings,
            layoutConfig = ContentLayoutConfig(
                appearance = userPreferences.preferredContentAppearance,
                cardImageSizing = userPreferences.preferredCardImageSizing,
            ),
            content = {
                item(key = "TAGS") {
                    YabaTagLayout(
                        modifier = Modifier.padding(top = 12.dp),
                        tags = state.tags,
                        layoutStyle = TagLayoutStyle.Horizontal,
                        onDrop = { _ -> }
                    ) { tagModel, isDragging ->
                        TagItemView(
                            modifier = Modifier.padding(
                                start = if (tagModel.id == state.tags.first().id) 12.dp else 0.dp,
                                end = if (tagModel.id == state.tags.last().id) 12.dp else 0.dp,
                            ),
                            model = tagModel,
                            onDeleteTag = { tagToBeDeleted ->
                                vm.onEvent(HomeEvent.OnDeleteTag(tagToBeDeleted))
                            }
                        )
                    }
                }

                item(key = "FOLDERS_HEADER") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .padding(top = 12.dp, start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YabaIcon(
                            name = "folder-01",
                        )
                        Text(
                            text = stringResource(Res.string.folders_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                if (state.folders.isEmpty()) {

                } else {
                    items(
                        items = state.folders,
                        key = { it.id },
                    ) { folderModel, appearance ->
                        FolderItemView(
                            model = folderModel,
                            appearance = appearance,
                            onDeleteFolder = { folderToBeDeleted ->
                                vm.onEvent(HomeEvent.OnDeleteFolder(folderToBeDeleted))
                            }
                        )
                    }
                }
            },
        )
    }
}
