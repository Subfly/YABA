package dev.subfly.yaba.ui.detail.tag

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
import dev.subfly.yabacore.state.detail.tag.TagDetailEvent
import dev.subfly.yabacore.state.detail.tag.TagDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.layout.ContentLayoutConfig
import dev.subfly.yabacore.ui.layout.YabaBookmarkLayout
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_selection_cancel
import yaba.composeapp.generated.resources.bookmark_selection_delete
import yaba.composeapp.generated.resources.bookmark_selection_enable
import yaba.composeapp.generated.resources.no_bookmarks_message
import yaba.composeapp.generated.resources.no_bookmarks_title
import yaba.composeapp.generated.resources.search_no_bookmarks_found_description
import yaba.composeapp.generated.resources.search_no_bookmarks_found_title
import yaba.composeapp.generated.resources.search_prompt
import yaba.composeapp.generated.resources.settings_bookmark_sorting_title
import yaba.composeapp.generated.resources.settings_content_appearance_title
import yaba.composeapp.generated.resources.settings_sort_order_title
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@Composable
fun TagDetailView(
    modifier: Modifier = Modifier,
    tagId: String,
) {
    val userPreferences = LocalUserPreferences.current
    val navigator = LocalContentNavigator.current

    val searchBarState = rememberSearchBarState()

    val vm = viewModel { TagDetailVM() }
    val state by vm.state

    LaunchedEffect(tagId) {
        vm.onEvent(TagDetailEvent.OnInit(tagId = tagId))
    }

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
                            vm.onEvent(TagDetailEvent.OnChangeQuery(query = newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(TagDetailEvent.OnChangeQuery(query = finalQuery))
                        },
                        leadingIcon = { YabaIcon(name = "search-01") },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = state.query.isNotEmpty(),
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut(),
                            ) {
                                IconButton(
                                    onClick = { vm.onEvent(TagDetailEvent.OnChangeQuery("")) }
                                ) { YabaIcon(name = "cancel-01") }
                            }
                        },
                        placeholder = { Text(text = stringResource(Res.string.search_prompt)) },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator::removeLastOrNull) {
                        YabaIcon(name = "arrow-left-01")
                    }
                },
                actions = {
                    TagDetailOptionsMenu(
                        state = state,
                        onEvent = vm::onEvent,
                    )
                },
            )
        },
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
                        message = { Text(text = stringResource(Res.string.no_bookmarks_message)) },
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
                        },
                    )
                }
            }

            else -> {
                YabaBookmarkLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddings)
                        .padding(
                            horizontal = if (userPreferences.preferredBookmarkAppearance != BookmarkAppearance.LIST) {
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
                                if (state.isSelectionMode) {
                                    vm.onEvent(
                                        TagDetailEvent.OnToggleBookmarkSelection(
                                            bookmarkId = model.id
                                        )
                                    )
                                } else {
                                    // TODO: OPEN BOOKMARK DETAIL
                                }
                            },
                            onDeleteBookmark = { bookmark ->
                                vm.onEvent(TagDetailEvent.OnDeleteBookmark(bookmark = bookmark))
                            },
                            onShareBookmark = {
                                // TODO: IMPLEMENT SHARE
                            },
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TagDetailOptionsMenu(
    state: TagDetailUIState,
    onEvent: (TagDetailEvent) -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
            YabaIcon(name = "more-horizontal-circle-02")
        }

        TagDetailDropdownMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            state = state,
            onEvent = onEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TagDetailDropdownMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    state: TagDetailUIState,
    onEvent: (TagDetailEvent) -> Unit,
) {
    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isSortingExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }

    val selectText = stringResource(Res.string.bookmark_selection_enable)
    val cancelSelectionText = stringResource(Res.string.bookmark_selection_cancel)
    val deleteSelectionText = stringResource(Res.string.bookmark_selection_delete)
    val groupCount = if (state.isSelectionMode) 3 else 2

    DropdownMenuPopup(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        // Actions group
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = groupCount),
        ) {
            DropdownMenuItem(
                shapes = MenuDefaults.itemShape(0, 1),
                checked = false,
                onCheckedChange = { _ ->
                    onDismissRequest()
                    onEvent(TagDetailEvent.OnToggleSelectionMode)
                },
                leadingIcon = {
                    YabaIcon(
                        name = if (state.isSelectionMode) "cancel-circle" else "checkmark-circle-01"
                    )
                },
                text = {
                    Text(text = if (state.isSelectionMode) cancelSelectionText else selectText)
                },
            )
        }

        if (state.isSelectionMode) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 1, count = groupCount),
            ) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 1),
                    checked = false,
                    onCheckedChange = { _ ->
                        onDismissRequest()
                        onEvent(TagDetailEvent.OnDeleteSelected)
                    },
                    leadingIcon = { YabaIcon(name = "delete-02") },
                    text = { Text(text = deleteSelectionText) },
                )
            }
        }

        // Config group (appearance / sorting / order)
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = groupCount - 1, count = groupCount),
        ) {
            BookmarkAppearanceSection(
                isExpanded = isAppearanceExpanded,
                onPressedSection = { isAppearanceExpanded = !isAppearanceExpanded },
                onDismissSubmenu = { isAppearanceExpanded = false },
                currentAppearance = state.bookmarkAppearance,
                onAppearanceSelection = { appearance ->
                    onEvent(TagDetailEvent.OnChangeAppearance(appearance = appearance))
                    onDismissRequest()
                },
            )

            SortingSection(
                isExpanded = isSortingExpanded,
                onPressedSection = { isSortingExpanded = !isSortingExpanded },
                onDismissSubmenu = { isSortingExpanded = false },
                currentSortType = state.sortType,
                onSortingSelection = { sortType ->
                    onEvent(
                        TagDetailEvent.OnChangeSort(
                            sortType = sortType,
                            sortOrder = state.sortOrder
                        )
                    )
                    onDismissRequest()
                },
            )

            SortOrderSection(
                isExpanded = isSortOrderExpanded,
                onPressedSection = { isSortOrderExpanded = !isSortOrderExpanded },
                onDismissSubmenu = { isSortOrderExpanded = false },
                currentSortOrder = state.sortOrder,
                onSortOrderSelection = { sortOrder ->
                    onEvent(
                        TagDetailEvent.OnChangeSort(
                            sortType = state.sortType,
                            sortOrder = sortOrder
                        )
                    )
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
                    count = BookmarkAppearance.entries.size
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
                shapes = MenuDefaults.groupShape(index = 0, count = SortType.entries.size),
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
                shapes = MenuDefaults.groupShape(index = 0, count = SortOrderType.entries.size),
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

