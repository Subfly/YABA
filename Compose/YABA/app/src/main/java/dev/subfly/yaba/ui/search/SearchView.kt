package dev.subfly.yaba.ui.search

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.layout.ContentLayoutConfig
import dev.subfly.yaba.layout.YabaBookmarkLayout
import dev.subfly.yaba.ui.detail.composables.SearchScreenChromeTopBar
import dev.subfly.yaba.ui.detail.composables.searchScreenIconButtonColors
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yaba.util.yabaPointerEventSpy
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.uiIconName
import dev.subfly.yaba.core.state.search.SearchEvent
import dev.subfly.yaba.core.state.search.SearchUIState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchView(modifier: Modifier = Modifier) {
    val userPreferences = LocalUserPreferences.current
    val navigator = LocalContentNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val shareUrl = rememberShareHandler()
    val shareScope = rememberCoroutineScope()

    val searchBarState = rememberSearchBarState()

    var searchHasFocus by remember { mutableStateOf(false) }

    val vm = viewModel { SearchVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.onEvent(event = SearchEvent.OnInit) }

    Scaffold(
        modifier = modifier.yabaPointerEventSpy(
            onInteraction = {
                if (searchHasFocus) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            }
        ),
    ) { paddings ->
        when {
            state.isLoading && state.bookmarks.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(paddings)) {
                    SearchToolbar(
                        state = state,
                        searchBarState = searchBarState,
                        onFocusChanged = { searchHasFocus = it },
                        onQueryChanged = { newQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(finalQuery))
                        },
                        onClear = { vm.onEvent(SearchEvent.OnChangeQuery("")) },
                        onEvent = vm::onEvent,
                    )
                    Box(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularWavyProgressIndicator() }
                }
            }

            state.query.isEmpty() && state.bookmarks.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(paddings)) {
                    SearchToolbar(
                        state = state,
                        searchBarState = searchBarState,
                        onFocusChanged = { searchHasFocus = it },
                        onQueryChanged = { newQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(finalQuery))
                        },
                        onClear = { vm.onEvent(SearchEvent.OnChangeQuery("")) },
                        onEvent = vm::onEvent,
                    )
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
            }

            state.query.isNotEmpty() && state.bookmarks.isEmpty() -> {
                Column(Modifier.fillMaxSize().padding(paddings)) {
                    SearchToolbar(
                        state = state,
                        searchBarState = searchBarState,
                        onFocusChanged = { searchHasFocus = it },
                        onQueryChanged = { newQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(SearchEvent.OnChangeQuery(finalQuery))
                        },
                        onClear = { vm.onEvent(SearchEvent.OnChangeQuery("")) },
                        onEvent = vm::onEvent,
                    )
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
            }

            else -> {
                val itemModifier = Modifier.padding(
                    horizontal = if (state.bookmarkAppearance == BookmarkAppearance.CARD) {
                        12.dp
                    } else {
                        0.dp
                    }
                )

                YabaBookmarkLayout(
                    modifier = Modifier.fillMaxSize(),
                    bookmarks = state.bookmarks,
                    layoutConfig = ContentLayoutConfig(
                        bookmarkAppearance = userPreferences.preferredBookmarkAppearance,
                        cardImageSizing = userPreferences.preferredCardImageSizing,
                        headlineSpacerSizing = 8.dp,
                        gridForceApplyPadding = true,
                    ),
                    onDrop = {},
                    stickyHeaderContent = {
                        SearchToolbar(
                            state = state,
                            searchBarState = searchBarState,
                            onFocusChanged = { searchHasFocus = it },
                            onQueryChanged = { newQuery ->
                                vm.onEvent(SearchEvent.OnChangeQuery(newQuery))
                            },
                            onSearch = { finalQuery ->
                                vm.onEvent(SearchEvent.OnChangeQuery(finalQuery))
                            },
                            onClear = { vm.onEvent(SearchEvent.OnChangeQuery("")) },
                            onEvent = vm::onEvent,
                        )
                    },
                    itemContent = { model, _, appearance, cardImageSizing, index, count ->
                        BookmarkItemView(
                            modifier = itemModifier,
                            model = model,
                            appearance = appearance,
                            cardImageSizing = cardImageSizing,
                            onClick = {
                                navigator.add(
                                    when (model.kind) {
                                        BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = model.id)
                                        BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = model.id)
                                        BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = model.id)
                                        BookmarkKind.FILE -> DocDetailRoute(bookmarkId = model.id)
                                    }
                                )
                            },
                            onDeleteBookmark = { bookmark ->
                                vm.onEvent(SearchEvent.OnDeleteBookmark(bookmark = bookmark))
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
                            count = count,
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchToolbar(
    state: SearchUIState,
    searchBarState: SearchBarState,
    onFocusChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onEvent: (SearchEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current
    val searchFieldTint = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
    val topBarIconButtonColors = searchScreenIconButtonColors()

    SearchScreenChromeTopBar(
        searchBarState = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.onFocusChanged { focusState ->
                    onFocusChanged(focusState.hasFocus)
                },
                query = state.query,
                expanded = false,
                onExpandedChange = {},
                onQueryChange = onQueryChanged,
                onSearch = onSearch,
                colors = SearchBarDefaults.inputFieldColors(
                    focusedContainerColor = searchFieldTint,
                    unfocusedContainerColor = searchFieldTint,
                    disabledContainerColor = searchFieldTint,
                ),
                leadingIcon = { YabaIcon(name = "search-01") },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = state.query.isNotEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        IconButton(
                            onClick = onClear,
                            colors = topBarIconButtonColors,
                            shapes = IconButtonDefaults.shapes(),
                        ) { YabaIcon(name = "cancel-01") }
                    }
                },
                placeholder = { Text(text = stringResource(R.string.search_prompt)) },
            )
        },
        navigationIcon = {
            IconButton(
                onClick = navigator::removeLastOrNull,
                colors = topBarIconButtonColors,
                shapes = IconButtonDefaults.shapes(),
            ) { YabaIcon(name = "arrow-left-01") }
        },
        actions = {
            OptionsMenu(
                iconButtonColors = topBarIconButtonColors,
                state = state,
                onEvent = onEvent,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OptionsMenu(
    iconButtonColors: IconButtonColors,
    state: SearchUIState,
    onEvent: (SearchEvent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(
            onClick = { isMenuExpanded = !isMenuExpanded },
            colors = iconButtonColors,
            shapes = IconButtonDefaults.shapes(),
        ) {
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
            onChangeCardImageSizing = { sizing ->
                onEvent(
                    SearchEvent.OnChangeAppearance(
                        appearance = BookmarkAppearance.CARD,
                        cardImageSizing = sizing
                    )
                )
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
    onChangeCardImageSizing: (CardImageSizing) -> Unit,
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
                onCardSizingSelection = { sizing ->
                    onChangeCardImageSizing(sizing)
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
    onCardSizingSelection: (CardImageSizing) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current
    var isCardImageSizingExpanded by remember { mutableStateOf(false) }

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
            text = { Text(text = stringResource(R.string.settings_content_appearance_title)) },
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
                    when (appearance) {
                        BookmarkAppearance.LIST, BookmarkAppearance.GRID -> {
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(
                                    index,
                                    BookmarkAppearance.entries.size
                                ),
                                checked = currentAppearance == appearance,
                                onCheckedChange = { _ ->
                                    onAppearanceSelection(appearance)
                                    onDismissSubmenu()
                                },
                                leadingIcon = { YabaIcon(name = appearance.uiIconName()) },
                                text = { Text(text = appearance.uiTitle()) },
                            )
                        }

                        BookmarkAppearance.CARD -> {
                            Box {
                                DropdownMenuItem(
                                    shapes = MenuDefaults.itemShape(
                                        index,
                                        BookmarkAppearance.entries.size
                                    ),
                                    checked = currentAppearance == appearance,
                                    onCheckedChange = { _ -> isCardImageSizingExpanded = true },
                                    leadingIcon = { YabaIcon(name = appearance.uiIconName()) },
                                    text = { Text(text = appearance.uiTitle()) },
                                    trailingIcon = {
                                        val sizingExpandedRotation by animateFloatAsState(
                                            targetValue = if (isCardImageSizingExpanded) 90F else 0F,
                                        )

                                        YabaIcon(
                                            modifier = Modifier.rotate(sizingExpandedRotation),
                                            name = "arrow-right-01"
                                        )
                                    }
                                )

                                DropdownMenuPopup(
                                    expanded = isCardImageSizingExpanded,
                                    onDismissRequest = { isCardImageSizingExpanded = false },
                                ) {
                                    DropdownMenuGroup(
                                        shapes = MenuDefaults.groupShape(
                                            index = 0,
                                            count = 1
                                        )
                                    ) {
                                        CardImageSizing.entries.fastForEachIndexed { i, sizing ->
                                            DropdownMenuItem(
                                                shapes = MenuDefaults.itemShape(
                                                    i,
                                                    CardImageSizing.entries.size
                                                ),
                                                checked = currentAppearance == BookmarkAppearance.CARD
                                                        && userPreferences.preferredCardImageSizing == sizing,
                                                onCheckedChange = { _ ->
                                                    onAppearanceSelection(appearance)
                                                    onCardSizingSelection(sizing)
                                                    isCardImageSizingExpanded = false
                                                    onDismissSubmenu()
                                                },
                                                leadingIcon = { YabaIcon(name = sizing.uiIconName()) },
                                                text = { Text(text = sizing.uiTitle()) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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
            text = { Text(text = stringResource(R.string.settings_bookmark_sorting_title)) },
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
            text = { Text(text = stringResource(R.string.settings_sort_order_title)) },
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
