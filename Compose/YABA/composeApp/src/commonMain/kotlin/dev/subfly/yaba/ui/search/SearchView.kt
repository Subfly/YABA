package dev.subfly.yaba.ui.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.state.search.SearchEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.YabaBookmarkLayout
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.no_bookmarks_message
import yaba.composeapp.generated.resources.no_bookmarks_title
import yaba.composeapp.generated.resources.search_no_bookmarks_found_description
import yaba.composeapp.generated.resources.search_no_bookmarks_found_title
import yaba.composeapp.generated.resources.search_prompt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchView(modifier: Modifier = Modifier) {
    val userPreferences = LocalUserPreferences.current
    val navigator = LocalContentNavigator.current

    val searchBarState = rememberSearchBarState()

    val vm = viewModel { SearchVM() }
    val state by vm.state

    LaunchedEffect(Unit) { vm.onEvent(event = SearchEvent.OnInit) }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBarWithSearch(
                scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.query,
                        expanded = false,
                        onExpandedChange = {},
                        onQueryChange = { newQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(finalQuery))
                        },
                        leadingIcon = { YabaIcon(name = "search-01") },
                        trailingIcon = {
                            IconButton(
                                onClick = { vm.onEvent(SearchEvent.OnChangeQuery("")) }
                            ) { YabaIcon(name = "cancel-01") }
                        },
                        placeholder = { Text(text = stringResource(Res.string.search_prompt)) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator::removeLastOrNull) {
                        YabaIcon(name = "arrow-left-01")
                    }
                },
                actions = { OptionsMenu() }
            )
        }
    ) { paddings ->
        when {
            state.isLoading && state.bookmarks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddings),
                    contentAlignment = Alignment.Center,
                ) { CircularWavyProgressIndicator() }
            }

            state.query.isEmpty() && state.bookmarks.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(paddings),
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
                    Modifier
                        .fillMaxSize()
                        .padding(paddings),
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
                YabaBookmarkLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddings),
                    bookmarks = state.bookmarks,
                    layoutConfig = ContentLayoutConfig(
                        bookmarkAppearance = userPreferences.preferredBookmarkAppearance,
                        cardImageSizing = userPreferences.preferredCardImageSizing,
                    ),
                    onDrop = {},
                    itemContent = { model, _, appearance, cardImageSizing ->
                        BookmarkItemView(
                            modifier = Modifier
                                .padding(
                                    horizontal = if (state.bookmarkAppearance == BookmarkAppearance.CARD) {
                                        12.dp
                                    } else 0.dp
                                ),
                            model = model,
                            appearance = appearance,
                            cardImageSizing = cardImageSizing,
                            onClick = {
                                // TODO: OPEN BOOKMARK DETAIL
                            },
                            onDeleteBookmark = { bookmark ->
                                vm.onEvent(SearchEvent.OnDeleteBookmark(bookmark = bookmark))
                            },
                            onShareBookmark = {
                                // TODO: IMPLEMENT SHARE
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionsMenu() {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
            YabaIcon(name = "more-horizontal-circle-02")
        }
        // TODO: ADD DROPDOWN
    }
}
