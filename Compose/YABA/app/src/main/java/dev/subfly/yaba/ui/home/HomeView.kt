package dev.subfly.yaba.ui.home

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yaba.core.components.item.tag.TagItemView
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.FolderDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.core.navigation.main.SearchRoute
import dev.subfly.yaba.core.navigation.main.TagDetailRoute
import dev.subfly.yaba.core.components.layout.YabaContentLayout
import dev.subfly.yaba.ui.home.components.HomeFab
import dev.subfly.yaba.ui.home.components.HomeTitleContent
import dev.subfly.yaba.ui.home.components.HomeTopBar
import dev.subfly.yaba.util.BookmarkPrivatePasswordEventEffect
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.rememberPrivateBookmarkOpenClick
import dev.subfly.yaba.util.Platform
import dev.subfly.yaba.util.YabaPlatform
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.FabPosition
import dev.subfly.yaba.core.state.home.HomeEvent
import kotlinx.coroutines.launch
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
    val shareUrl = rememberShareHandler()
    val shareScope = rememberCoroutineScope()

    val vm = viewModel { HomeVM() }
    val state by vm.state.collectAsStateWithLifecycle()
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    BookmarkPrivatePasswordEventEffect(
        resolveBookmark = { id -> state.recentBookmarks.find { it.id == id } },
        onOpenBookmark = { model ->
            navigator.add(
                when (model.kind) {
                    BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = model.id)
                    BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = model.id)
                    BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = model.id)
                    BookmarkKind.FILE -> DocDetailRoute(bookmarkId = model.id)
                },
            )
        },
        onEditBookmark = { model ->
            when (model.kind) {
                BookmarkKind.LINK -> creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.NOTE -> creationNavigator.add(NotemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.IMAGE -> creationNavigator.add(ImagemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.FILE -> creationNavigator.add(DocmarkCreationRoute(bookmarkId = model.id))
            }
            appStateManager.onShowCreationContent()
        },
        onShareBookmark = { bookmark ->
            when (bookmark.kind) {
                BookmarkKind.LINK -> shareScope.launch {
                    LinkmarkManager.getBookmarkUrl(bookmark.id)?.let(shareUrl)
                }
                BookmarkKind.IMAGE -> shareScope.launch {
                    YabaFileAccessor.shareImageBookmark(bookmark.id)
                }
                else -> {}
            }
        },
        onDeleteBookmark = { bookmark ->
            deletionDialogManager.send(
                DeletionState(
                    deletionType = DeletionType.BOOKMARK,
                    bookmarkToBeDeleted = bookmark,
                    onConfirm = { vm.onEvent(HomeEvent.OnDeleteBookmark(bookmark)) },
                ),
            )
        },
    )

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
                                title = R.string.home_recents_label,
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainer,
                                                shape = RoundedCornerShape(12.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        NoContentView(
                                            modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                                            iconName = "bookmark-02",
                                            labelRes = R.string.no_folders_message,
                                            message = { Text(text = stringResource(R.string.no_folders_message)) },
                                        )
                                    }
                                }
                            }

                            else -> {
                                itemsIndexed(
                                    items = state.recentBookmarks,
                                    key = { _, it -> it.id },
                                ) { index, bookmarkModel ->
                                    val openBookmark = rememberPrivateBookmarkOpenClick(bookmarkModel) {
                                        navigator.add(
                                            when (bookmarkModel.kind) {
                                                BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = bookmarkModel.id)
                                                BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = bookmarkModel.id)
                                                BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = bookmarkModel.id)
                                                BookmarkKind.FILE -> DocDetailRoute(bookmarkId = bookmarkModel.id)
                                            },
                                        )
                                    }
                                    BookmarkItemView(
                                        model = bookmarkModel,
                                        appearance = BookmarkAppearance.LIST,
                                        cardImageSizing = state.cardImageSizing,
                                        onClick = { openBookmark() },
                                        onDeleteBookmark = { bookmark ->
                                            vm.onEvent(HomeEvent.OnDeleteBookmark(bookmark))
                                        },
                                        onShareBookmark = { bookmark ->
                                            when (bookmark.kind) {
                                                BookmarkKind.LINK -> shareScope.launch {
                                                    LinkmarkManager.getBookmarkUrl(bookmark.id)?.let(shareUrl)
                                                }
                                                BookmarkKind.IMAGE -> shareScope.launch {
                                                    YabaFileAccessor.shareImageBookmark(bookmark.id)
                                                }
                                                else -> {}
                                            }
                                        },
                                        index = index,
                                        count = state.recentBookmarks.size,
                                    )
                                }
                            }
                        }
                    }

                    // Folders Section
                    header(key = "FOLDERS_HEADER") {
                        HomeTitleContent(
                            title = R.string.folders_title,
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            shape = RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    NoContentView(
                                        modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                                        iconName = "folder-01",
                                        labelRes = R.string.no_folders_message,
                                        message = { Text(text = stringResource(R.string.no_folders_message)) },
                                    )
                                }
                            }
                        }

                        else -> {
                            itemsIndexed(
                                items = state.folderRows,
                                key = { _, it -> it.folder.id },
                            ) { index, row ->
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
                                    index = index,
                                    count = state.folderRows.size,
                                )
                            }
                        }
                    }

                    // Tags Section
                    header(key = "TAGS_HEADER") {
                        HomeTitleContent(
                            modifier = Modifier.padding(top = 6.dp),
                            title = R.string.tags_title,
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
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            shape = RoundedCornerShape(12.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    NoContentView(
                                        modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                                        iconName = "tag-01",
                                        labelRes = R.string.no_tags_message,
                                        message = { Text(text = stringResource(R.string.no_tags_message)) },
                                    )
                                }
                            }
                        }

                        else -> {
                            itemsIndexed(
                                items = state.tags,
                                key = { _, it -> it.id },
                            ) { index, tagModel ->
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
                                    index = index,
                                    count = state.tags.size,
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
