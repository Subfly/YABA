package dev.subfly.yaba.ui.selection.bookmark

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.state.selection.bookmark.BookmarkSelectionEvent
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.no_bookmarks_message
import yaba.composeapp.generated.resources.no_bookmarks_title
import yaba.composeapp.generated.resources.search_no_bookmarks_found_description
import yaba.composeapp.generated.resources.search_no_bookmarks_found_title

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BookmarkSelectionContent(selectedBookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { BookmarkSelectionVM() }
    val state by vm.state.collectAsStateWithLifecycle()

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
                ) { Text(text = stringResource(Res.string.done)) }
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
                        labelRes = Res.string.no_bookmarks_title,
                        message = {
                            Text(text = stringResource(Res.string.no_bookmarks_message))
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
                        labelRes = Res.string.search_no_bookmarks_found_title,
                        message = {
                            Text(
                                text = stringResource(
                                    Res.string.search_no_bookmarks_found_description,
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
                        BookmarkItemView(
                            modifier = Modifier.padding(bottom = 8.dp).animateItem(),
                            model = bookmark,
                            appearance = BookmarkAppearance.LIST,
                            isAddedToSelection = bookmark.id == state.selectedBookmarkId,
                            onClick = { vm.onEvent(BookmarkSelectionEvent.OnSelectBookmark(bookmark.id)) },
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
