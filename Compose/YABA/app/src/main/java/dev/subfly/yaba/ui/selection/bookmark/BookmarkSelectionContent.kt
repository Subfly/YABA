package dev.subfly.yaba.ui.selection.bookmark

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.CanvmarkCreationRoute
import dev.subfly.yaba.util.BookmarkPrivatePasswordEventEffect
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.rememberPrivateBookmarkOpenClick
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.state.selection.bookmark.BookmarkSelectionEvent
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BookmarkSelectionContent(selectedBookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current
    val shareUrl = rememberShareHandler()
    val shareScope = rememberCoroutineScope()

    val vm = viewModel { BookmarkSelectionVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    BookmarkPrivatePasswordEventEffect(
        resolveBookmark = { id -> state.bookmarks.find { it.id == id } },
        onOpenBookmark = { model ->
            vm.onEvent(BookmarkSelectionEvent.OnSelectBookmark(model.id))
        },
        onEditBookmark = { model ->
            when (model.kind) {
                BookmarkKind.LINK -> creationNavigator.add(LinkmarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.NOTE -> creationNavigator.add(NotemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.IMAGE -> creationNavigator.add(ImagemarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.FILE -> creationNavigator.add(DocmarkCreationRoute(bookmarkId = model.id))
                BookmarkKind.CANVAS -> creationNavigator.add(CanvmarkCreationRoute(bookmarkId = model.id))
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
        onDeleteBookmark = {},
    )

    LaunchedEffect(selectedBookmarkId) {
        vm.onEvent(BookmarkSelectionEvent.OnInit(selectedBookmarkId = selectedBookmarkId))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
            title = { Text(text = "Select Bookmark") }, // TODO: localize
            navigationIcon = {
                IconButton(onClick = creationNavigator::removeLastOrNull) {
                    YabaIcon(name = "arrow-left-01")
                }
            },
            actions = {
                TextButton(
                    enabled = state.selectedBookmark != null,
                    onClick = {
                        val bookmark = state.selectedBookmark ?: return@TextButton
                        resultStore.setResult(
                            ResultStoreKeys.SELECTED_BOOKMARK,
                            bookmark.id,
                        )
                        creationNavigator.removeLastOrNull()
                    },
                ) { Text(text = stringResource(R.string.done)) }
            },
        )
        SearchBarContent(
            currentQuery = state.query,
            onQueryChange = { vm.onEvent(BookmarkSelectionEvent.OnChangeQuery(it)) },
            onSearch = { vm.onEvent(BookmarkSelectionEvent.OnChangeQuery(it)) },
            onClear = { vm.onEvent(BookmarkSelectionEvent.OnChangeQuery("")) },
        )
        when {
            state.query.isEmpty() && state.bookmarks.isEmpty() && state.isLoading -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularWavyProgressIndicator() }
            }

            state.query.isEmpty() && state.bookmarks.isEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    NoContentView(
                        iconName = "bookmark-02",
                        labelRes = R.string.no_bookmarks_title,
                        message = {
                            Text(text = stringResource(R.string.no_bookmarks_message))
                        }
                    )
                }
            }

            state.query.isNotEmpty() && state.bookmarks.isEmpty() -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    NoContentView(
                        iconName = "bookmark-02",
                        labelRes = R.string.search_no_bookmarks_found_title,
                        message = {
                            Text(
                                text = stringResource(
                                    R.string.search_no_bookmarks_found_description,
                                    state.query,
                                )
                            )
                        }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(state.bookmarks, key = { it.id }) { bookmark ->
                        val openSelect = rememberPrivateBookmarkOpenClick(bookmark) {
                            vm.onEvent(BookmarkSelectionEvent.OnSelectBookmark(bookmark.id))
                        }
                        BookmarkItemView(
                            modifier = Modifier.padding(bottom = 8.dp).animateItem(),
                            model = bookmark,
                            appearance = BookmarkAppearance.LIST,
                            isAddedToSelection = bookmark.id == state.selectedBookmarkId,
                            onClick = { openSelect() },
                            onDeleteBookmark = {},
                            onShareBookmark = {},
                        )
                    }
                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarContent(
    currentQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
) {
    val searchbarState = rememberSearchBarState()
    SearchBar(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        state = searchbarState,
        inputField = {
            SearchBarDefaults.InputField(
                query = currentQuery,
                expanded = false,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onExpandedChange = {},
                placeholder = { Text(text = "Search bookmarks") }, // TODO: localize
                leadingIcon = { YabaIcon(name = "search-01") },
                trailingIcon = { IconButton(onClick = onClear) { YabaIcon(name = "cancel-01") } },
            )
        },
    )
}
