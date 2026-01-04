package dev.subfly.yaba.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yaba.ui.home.components.HomeFab
import dev.subfly.yaba.ui.home.components.HomeTitleContent
import dev.subfly.yaba.ui.home.components.HomeTopBar
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.state.home.HomeEvent
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.GridLayoutConfig
import dev.subfly.yabacore.ui.layout.YabaContentLayout
import dev.subfly.yabacore.ui.layout.YabaContentSpan
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.folders_title
import yaba.composeapp.generated.resources.home_recents_label
import yaba.composeapp.generated.resources.no_folders_message
import yaba.composeapp.generated.resources.no_tags_message
import yaba.composeapp.generated.resources.tags_title
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

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
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
                    grid = GridLayoutConfig(
                        minCellWidth = 180.dp,
                        outerPadding = PaddingValues(horizontal = 12.dp),
                    )
                ),
                content = {
                    if (state.recentBookmarks.isNotEmpty()) {
                        item(
                            key = "RECENTS_HEADER",
                            span = YabaContentSpan.FullLine,
                        ) {
                            HomeTitleContent(
                                title = Res.string.home_recents_label,
                                iconName = "clock-01"
                            )
                        }

                        items(
                            items = state.recentBookmarks,
                            key = { it.id },
                        ) { bookmarkModel, appearance ->

                        }
                    }

                    item(
                        key = "FOLDERS_HEADER",
                        span = YabaContentSpan.FullLine,
                    ) {
                        HomeTitleContent(
                            title = Res.string.folders_title,
                            iconName = "folder-01"
                        )
                    }

                    when {
                        state.isLoading -> {
                            item(
                                key = "FOLDERS_LOADING",
                                span = YabaContentSpan.FullLine,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularWavyProgressIndicator() }
                            }
                        }

                        state.folders.isEmpty() -> {
                            item(
                                key = "NO_FOLDERS",
                                span = YabaContentSpan.FullLine,
                            ) {
                                NoContentView(
                                    iconName = "folder-01",
                                    // TODO: SEE WHY TITLE IS NOT TRANSLATED IN XML
                                    labelRes = Res.string.no_folders_message,
                                    messageRes = Res.string.no_folders_message,
                                )
                            }
                        }

                        else -> {
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
                    }

                    item(
                        key = "TAGS_HEADER",
                        span = YabaContentSpan.FullLine,
                    ) {
                        HomeTitleContent(
                            modifier = Modifier.padding(top = 6.dp),
                            title = Res.string.tags_title,
                            iconName = "tag-01"
                        )
                    }

                    when {
                        state.isLoading -> {
                            item(
                                key = "TAGS_LOADING",
                                span = YabaContentSpan.FullLine,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularWavyProgressIndicator() }
                            }
                        }

                        state.tags.isEmpty() -> {
                            item(
                                key = "NO_TAGS",
                                span = YabaContentSpan.FullLine,
                            ) {
                                NoContentView(
                                    iconName = "folder-01",
                                    // TODO: SEE WHY TITLE IS NOT TRANSLATED IN XML
                                    labelRes = Res.string.no_tags_message,
                                    messageRes = Res.string.no_tags_message,
                                )
                            }
                        }

                        else -> {
                            items(
                                items = state.tags,
                                key = { it.id },
                            ) { tagModel, appearance ->
                                TagItemView(
                                    model = tagModel,
                                    appearance = appearance,
                                    onDeleteTag = { tagToBeDeleted ->
                                        vm.onEvent(HomeEvent.OnDeleteTag(tagToBeDeleted))
                                    }
                                )
                            }
                        }
                    }

                    item(
                        key = "EMPTY_SPACER_FOR_FAB",
                        span = YabaContentSpan.FullLine,
                    ) {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                },
            )
        }
    }
}
