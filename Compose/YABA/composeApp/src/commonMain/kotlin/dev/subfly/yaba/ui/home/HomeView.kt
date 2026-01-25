package dev.subfly.yaba.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.RecentBookmarksGridSection
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.SearchRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.ui.home.components.HomeFab
import dev.subfly.yaba.ui.home.components.HomeTitleContent
import dev.subfly.yaba.ui.home.components.HomeTopBar
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.FabPosition
import dev.subfly.yabacore.state.home.HomeEvent
import dev.subfly.yabacore.ui.layout.YabaContentLayout
import org.jetbrains.compose.resources.stringResource
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
    val navigator = LocalContentNavigator.current

    val vm = viewModel { HomeVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) { vm.onEvent(HomeEvent.OnInit) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = if (Platform == YabaPlatform.ANDROID) {
                Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else Modifier,
            topBar = {
                HomeTopBar(
                    scrollBehavior = scrollBehavior,
                    onSortingChanged = { newSortType ->
                        vm.onEvent(HomeEvent.OnChangeCollectionSorting(newSortType))
                    },
                    onSortOrderChanged = { newSortOrder ->
                        vm.onEvent(HomeEvent.OnChangeSortOrder(newSortOrder))
                    },
                    onSearchClicked = {
                        // If not in search route
                        if (navigator.last() !is SearchRoute) {
                            // Remove all the occurrences of Search Route
                            navigator.removeAll { it is SearchRoute }
                            // Then navigate
                            navigator.add(SearchRoute())
                            // So that we will be %100 sure that there
                            // will only be one Search Route in the backstack
                        }
                    },
                )
            },
            floatingActionButtonPosition = when (userPreferences.preferredFabPosition) {
                FabPosition.LEFT -> androidx.compose.material3.FabPosition.Start
                FabPosition.RIGHT -> androidx.compose.material3.FabPosition.End
                FabPosition.CENTER -> androidx.compose.material3.FabPosition.Center
            },
            floatingActionButton = { HomeFab() }
        ) { paddings ->
            val listState = rememberLazyListState()

            YabaContentLayout(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                contentPadding = paddings,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = {
                    if (Platform == YabaPlatform.ANDROID && userPreferences.showRecents) {
                        // Recent Bookmarks Section
                        header(key = "RECENTS_HEADER") {
                            HomeTitleContent(
                                title = Res.string.home_recents_label,
                                iconName = "clock-01"
                            )
                        }
                        when {
                            state.isLoading -> {
                                item(key = "RECENTS_LOADING") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(300.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { CircularWavyProgressIndicator() }
                                }
                            }

                            state.recentBookmarks.isEmpty() -> {
                                // TODO: LOCALIZATION
                                item(key = "NO_RECENTS") {
                                    NoContentView(
                                        iconName = "bookmark-02",
                                        labelRes = Res.string.no_folders_message,
                                        message = { Text(text = stringResource(Res.string.no_folders_message)) },
                                    )
                                }
                            }

                            else -> {
                                // For GRID appearance, render all bookmarks in a single grid item
                                if (state.bookmarkAppearance == BookmarkAppearance.GRID) {
                                    item(key = "RECENTS_GRID") {
                                        RecentBookmarksGridSection(
                                            bookmarks = state.recentBookmarks,
                                            appearance = state.bookmarkAppearance,
                                            cardImageSizing = state.cardImageSizing,
                                            onClickBookmark = { bookmark ->
                                                navigator.add(
                                                    LinkDetailRoute(bookmarkId = bookmark.id)
                                                )
                                            },
                                            onDeleteBookmark = { bookmark ->
                                                vm.onEvent(HomeEvent.OnDeleteBookmark(bookmark))
                                            },
                                            onShareBookmark = { bookmark ->
                                                // TODO: Implement share functionality
                                            },
                                        )
                                    }
                                } else {
                                    // For LIST/CARD appearance, render each bookmark as a list item
                                    items(
                                        items = state.recentBookmarks,
                                        key = { it.id },
                                    ) { bookmarkModel ->
                                        BookmarkItemView(
                                            modifier = Modifier
                                                .padding(
                                                    horizontal = if (state.bookmarkAppearance == BookmarkAppearance.CARD) {
                                                        12.dp
                                                    } else 0.dp
                                                ),
                                            model = bookmarkModel,
                                            appearance = state.bookmarkAppearance,
                                            cardImageSizing = state.cardImageSizing,
                                            onClick = {
                                                navigator.add(
                                                    LinkDetailRoute(bookmarkId = bookmarkModel.id)
                                                )
                                            },
                                            onDeleteBookmark = { bookmark ->
                                                vm.onEvent(HomeEvent.OnDeleteBookmark(bookmark))
                                            },
                                            onShareBookmark = { bookmark ->
                                                // TODO: Implement share functionality
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Folders Section
                    header(key = "FOLDERS_HEADER") {
                        HomeTitleContent(
                            title = Res.string.folders_title,
                            iconName = "folder-01"
                        )
                    }

                    when {
                        state.isLoading -> {
                            item(key = "FOLDERS_LOADING") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(300.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularWavyProgressIndicator() }
                            }
                        }

                        state.folderRows.isEmpty() -> {
                            item(key = "NO_FOLDERS") {
                                NoContentView(
                                    iconName = "folder-01",
                                    labelRes = Res.string.no_folders_message,
                                    message = { Text(text = stringResource(Res.string.no_folders_message)) },
                                )
                            }
                        }

                        else -> {
                            items(
                                items = state.folderRows,
                                key = { it.folder.id },
                            ) { row ->
                                FolderItemView(
                                    modifier = Modifier,
                                    model = row.folder,
                                    parentColors = row.parentColors,
                                    hasChildren = row.hasChildren,
                                    isExpanded = row.isExpanded,
                                    onToggleExpanded = {
                                        vm.onEvent(HomeEvent.OnToggleFolderExpanded(row.folder.id))
                                    },
                                    onDeleteFolder = { folderToBeDeleted ->
                                        vm.onEvent(HomeEvent.OnDeleteFolder(folderToBeDeleted))
                                    },
                                    onClick = onClick@{ clickedFolder ->
                                        val clickedId = clickedFolder.id
                                        val lastRoute = navigator.last()

                                        if (lastRoute is FolderDetailRoute
                                            && lastRoute.folderId == clickedId
                                        ) return@onClick

                                        navigator.add(FolderDetailRoute(folderId = clickedId))
                                    },
                                )
                            }
                        }
                    }

                    // Tags Section
                    header(key = "TAGS_HEADER") {
                        HomeTitleContent(
                            modifier = Modifier.padding(top = 6.dp),
                            title = Res.string.tags_title,
                            iconName = "tag-01"
                        )
                    }

                    when {
                        state.isLoading -> {
                            item(key = "TAGS_LOADING") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(300.dp),
                                    contentAlignment = Alignment.Center,
                                ) { CircularWavyProgressIndicator() }
                            }
                        }

                        state.tags.isEmpty() -> {
                            item(key = "NO_TAGS") {
                                NoContentView(
                                    iconName = "folder-01",
                                    labelRes = Res.string.no_tags_message,
                                    message = { Text(text = stringResource(Res.string.no_tags_message)) },
                                )
                            }
                        }

                        else -> {
                            items(
                                items = state.tags,
                                key = { it.id },
                            ) { tagModel ->
                                TagItemView(
                                    model = tagModel,
                                    onDeleteTag = { tagToBeDeleted ->
                                        vm.onEvent(HomeEvent.OnDeleteTag(tagToBeDeleted))
                                    },
                                    onClick = onClick@{ clickedTag ->
                                        val clickedId = clickedTag.id
                                        val lastRoute = navigator.last()
                                        if (lastRoute is TagDetailRoute
                                            && lastRoute.tagId == clickedId
                                        ) return@onClick

                                        navigator.add(
                                            TagDetailRoute(tagId = clickedId)
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Spacer for FAB
                    item(key = "EMPTY_SPACER_FOR_FAB") {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            )
        }
    }
}
