package dev.subfly.yaba.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.state.search.SearchEvent
import dev.subfly.yabacore.state.search.SearchUIState
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
import yaba.composeapp.generated.resources.settings_bookmark_sorting_title
import yaba.composeapp.generated.resources.settings_content_appearance_title
import yaba.composeapp.generated.resources.settings_sort_order_title

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
                            AnimatedVisibility(
                                visible = state.query.isNotEmpty(),
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut(),
                            ) {
                                IconButton(
                                    onClick = { vm.onEvent(SearchEvent.OnChangeQuery("")) }
                                ) { YabaIcon(name = "cancel-01") }
                            }
                        },
                        placeholder = { Text(text = stringResource(Res.string.search_prompt)) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator::removeLastOrNull) {
                        YabaIcon(name = "arrow-left-01")
                    }
                },
                actions = { OptionsMenu(state = state, onEvent = vm::onEvent) }
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
                        .padding(paddings)
                        .padding(
                            horizontal = if (state.bookmarkAppearance != BookmarkAppearance.LIST) {
                                12.dp
                            } else 0.dp
                        ),
                    bookmarks = state.bookmarks,
                    layoutConfig = ContentLayoutConfig(
                        bookmarkAppearance = userPreferences.preferredBookmarkAppearance,
                        cardImageSizing = userPreferences.preferredCardImageSizing,
                        headlineSpacerSizing = 8.dp,
                    ),
                    onDrop = {},
                    itemContent = { model, _, appearance, cardImageSizing ->
                        BookmarkItemView(
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
private fun OptionsMenu(
    state: SearchUIState,
    onEvent: (SearchEvent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
            YabaIcon(name = "more-horizontal-circle-02")
        }
        SearchDropdownMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            bookmarkAppearance = state.bookmarkAppearance,
            sortType = state.sortType,
            sortOrder = state.sortOrder,
            onChangeAppearance = { appearance ->
                onEvent(SearchEvent.OnChangeAppearance(appearance = appearance))
            },
            onChangeSortType = { sortType ->
                onEvent(SearchEvent.OnChangeSort(sortType = sortType, sortOrder = state.sortOrder))
            },
            onChangeSortOrder = { sortOrder ->
                onEvent(SearchEvent.OnChangeSort(sortType = state.sortType, sortOrder = sortOrder))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchDropdownMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    bookmarkAppearance: BookmarkAppearance,
    sortType: SortType,
    sortOrder: SortOrderType,
    onChangeAppearance: (BookmarkAppearance) -> Unit,
    onChangeSortType: (SortType) -> Unit,
    onChangeSortOrder: (SortOrderType) -> Unit,
) {
    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isSortingExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }

    DropdownMenuPopup(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 1),
        ) {
            BookmarkAppearanceSection(
                isExpanded = isAppearanceExpanded,
                onPressedSection = { isAppearanceExpanded = !isAppearanceExpanded },
                onDismissSubmenu = { isAppearanceExpanded = false },
                currentAppearance = bookmarkAppearance,
                onAppearanceSelection = { appearance ->
                    onChangeAppearance(appearance)
                    onDismissRequest()
                },
            )

            SortingSection(
                isExpanded = isSortingExpanded,
                onPressedSection = { isSortingExpanded = !isSortingExpanded },
                onDismissSubmenu = { isSortingExpanded = false },
                currentSortType = sortType,
                onSortingSelection = { newSortType ->
                    onChangeSortType(newSortType)
                    onDismissRequest()
                },
            )

            SortOrderSection(
                isExpanded = isSortOrderExpanded,
                onPressedSection = { isSortOrderExpanded = !isSortOrderExpanded },
                onDismissSubmenu = { isSortOrderExpanded = false },
                currentSortOrder = sortOrder,
                onSortOrderSelection = { newSortOrder ->
                    onChangeSortOrder(newSortOrder)
                    onDismissRequest()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BookmarkAppearanceSection(
    isExpanded: Boolean,
    onPressedSection: () -> Unit,
    onDismissSubmenu: () -> Unit,
    currentAppearance: BookmarkAppearance,
    onAppearanceSelection: (BookmarkAppearance) -> Unit,
) {
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(0, 3),
            checked = false,
            onCheckedChange = { _ -> onPressedSection() },
            leadingIcon = { YabaIcon(name = "change-screen-mode") },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01",
                )
            },
            text = { Text(text = stringResource(Res.string.settings_content_appearance_title)) },
        )

        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(
                    index = 0,
                    count = 1
                ),
            ) {
                BookmarkAppearance.entries.fastForEachIndexed { index, appearance ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, BookmarkAppearance.entries.size),
                        checked = currentAppearance == appearance,
                        onCheckedChange = { _ ->
                            onAppearanceSelection(appearance)
                            onDismissSubmenu()
                        },
                        leadingIcon = { YabaIcon(name = appearance.uiIconName()) },
                        text = { Text(text = appearance.uiTitle()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortingSection(
    isExpanded: Boolean,
    onPressedSection: () -> Unit,
    onDismissSubmenu: () -> Unit,
    currentSortType: SortType,
    onSortingSelection: (SortType) -> Unit,
) {
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(1, 3),
            checked = false,
            onCheckedChange = { _ -> onPressedSection() },
            leadingIcon = { YabaIcon(name = "sorting-04") },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01",
                )
            },
            text = { Text(text = stringResource(Res.string.settings_bookmark_sorting_title)) },
        )

        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
            ) {
                SortType.entries.fastForEachIndexed { index, sortType ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, SortType.entries.size),
                        checked = currentSortType == sortType,
                        onCheckedChange = { _ ->
                            onSortingSelection(sortType)
                            onDismissSubmenu()
                        },
                        leadingIcon = { YabaIcon(name = sortType.uiIconName()) },
                        text = { Text(text = sortType.uiTitle()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SortOrderSection(
    isExpanded: Boolean,
    onPressedSection: () -> Unit,
    onDismissSubmenu: () -> Unit,
    currentSortOrder: SortOrderType,
    onSortOrderSelection: (SortOrderType) -> Unit,
) {
    Box {
        DropdownMenuItem(
            shapes = MenuDefaults.itemShape(2, 3),
            checked = false,
            onCheckedChange = { _ -> onPressedSection() },
            leadingIcon = { YabaIcon(name = currentSortOrder.uiIconName()) },
            trailingIcon = {
                val expandedRotation by animateFloatAsState(
                    targetValue = if (isExpanded) 90F else 0F,
                )
                YabaIcon(
                    modifier = Modifier.rotate(expandedRotation),
                    name = "arrow-right-01",
                )
            },
            text = { Text(text = stringResource(Res.string.settings_sort_order_title)) },
        )

        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = onDismissSubmenu,
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
            ) {
                SortOrderType.entries.fastForEachIndexed { index, sortOrder ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, SortOrderType.entries.size),
                        checked = currentSortOrder == sortOrder,
                        onCheckedChange = { _ ->
                            onSortOrderSelection(sortOrder)
                            onDismissSubmenu()
                        },
                        leadingIcon = { YabaIcon(name = sortOrder.uiIconName()) },
                        text = { Text(text = sortOrder.uiTitle()) },
                    )
                }
            }
        }
    }
}
