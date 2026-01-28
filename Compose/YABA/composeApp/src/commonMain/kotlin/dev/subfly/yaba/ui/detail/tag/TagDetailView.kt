package dev.subfly.yaba.ui.detail.tag

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yaba.util.yabaPointerEventSpy
import dev.subfly.yabacore.model.utils.BookmarkAppearance
import dev.subfly.yabacore.model.utils.CardImageSizing
import dev.subfly.yabacore.model.utils.SortOrderType
import dev.subfly.yabacore.model.utils.SortType
import dev.subfly.yabacore.model.utils.YabaColor
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
import yaba.composeapp.generated.resources.new_bookmark
import yaba.composeapp.generated.resources.no_bookmarks_message
import yaba.composeapp.generated.resources.no_bookmarks_title
import yaba.composeapp.generated.resources.search_collection
import yaba.composeapp.generated.resources.search_no_bookmarks_found_description
import yaba.composeapp.generated.resources.search_no_bookmarks_found_title
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
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val searchBarState = rememberSearchBarState()

    var searchHasFocus by remember { mutableStateOf(false) }

    val vm = viewModel { TagDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(tagId) {
        vm.onEvent(TagDetailEvent.OnInit(tagId = tagId))
    }

    Scaffold(
        modifier = modifier.yabaPointerEventSpy(
            onInteraction = {
                if (searchHasFocus) {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            }
        ),
        topBar = {
            AppBarWithSearch(
                modifier = Modifier.onFocusChanged { focusState ->
                    searchHasFocus = focusState.hasFocus
                },
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
                        placeholder = {
                            Text(
                                text = stringResource(
                                    Res.string.search_collection,
                                    state.tag?.label ?: ""
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
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
                        onNewBookmark = {
                            val tag = state.tag ?: return@TagDetailOptionsMenu
                            resultStore.setResult(ResultStoreKeys.SELECTED_TAGS, listOf(tag))
                            creationNavigator.add(BookmarkCreationRoute())
                            appStateManager.onShowCreationContent()
                        },
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
                    itemContent = { model, _, appearance, cardImageSizing, index, count ->
                        BookmarkItemView(
                            model = model,
                            appearance = appearance,
                            cardImageSizing = cardImageSizing,
                            isAddedToSelection = model.id in state.selectedBookmarkIds,
                            onClick = {
                                if (state.isSelectionMode) {
                                    vm.onEvent(
                                        TagDetailEvent.OnToggleBookmarkSelection(
                                            bookmarkId = model.id
                                        )
                                    )
                                } else {
                                    navigator.add(
                                        LinkDetailRoute(bookmarkId = model.id)
                                    )
                                }
                            },
                            onDeleteBookmark = { bookmark ->
                                vm.onEvent(TagDetailEvent.OnDeleteBookmark(bookmark = bookmark))
                            },
                            onShareBookmark = {
                                // TODO: IMPLEMENT SHARE
                            },
                            index = index,
                            count = count,
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
    onNewBookmark: () -> Unit,
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
            onNewBookmark = onNewBookmark,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalUuidApi::class)
@Composable
private fun TagDetailDropdownMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    state: TagDetailUIState,
    onEvent: (TagDetailEvent) -> Unit,
    onNewBookmark: () -> Unit,
) {
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isSortingExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }

    val newBookmarkText = stringResource(Res.string.new_bookmark)
    val selectText = stringResource(Res.string.bookmark_selection_enable)
    val cancelSelectionText = stringResource(Res.string.bookmark_selection_cancel)
    val deleteSelectionText = stringResource(Res.string.bookmark_selection_delete)

    DropdownMenuPopup(
        expanded = isExpanded,
        onDismissRequest = onDismissRequest,
    ) {
        // Actions group
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 0, count = 2),
        ) {
            AnimatedContent(targetState = state.isSelectionMode) { isInSelectionMode ->
                if (isInSelectionMode) {
                    Column {
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(0, 2),
                            checked = false,
                            enabled = state.selectedBookmarkIds.isNotEmpty(),
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                if (state.selectedBookmarkIds.isNotEmpty()) {
                                    deletionDialogManager.send(
                                        DeletionState(
                                            deletionType = DeletionType.BOOKMARKS,
                                            onConfirm = { onEvent(TagDetailEvent.OnDeleteSelected) },
                                        )
                                    )
                                    appStateManager.onShowDeletionDialog()
                                }
                            },
                            leadingIcon = {
                                YabaIcon(
                                    name = "delete-02",
                                    color = YabaColor.RED,
                                )
                            },
                            text = { Text(text = deleteSelectionText) },
                        )
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(1, 2),
                            checked = false,
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                onEvent(TagDetailEvent.OnToggleSelectionMode)
                            },
                            leadingIcon = { YabaIcon(name = "cancel-circle") },
                            text = { Text(text = cancelSelectionText) },
                        )
                    }
                } else {
                    Column {
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(0, 2),
                            checked = false,
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                onNewBookmark()
                            },
                            leadingIcon = { YabaIcon(name = "bookmark-add-02") },
                            text = { Text(text = newBookmarkText) },
                        )
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(1, 2),
                            checked = false,
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                onEvent(TagDetailEvent.OnToggleSelectionMode)
                            },
                            leadingIcon = { YabaIcon(name = "checkmark-circle-01") },
                            text = { Text(text = selectText) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Config group (appearance / sorting / order)
        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(index = 1, count = 2),
        ) {
            BookmarkAppearanceSection(
                isExpanded = isAppearanceExpanded,
                onPressedSection = { isAppearanceExpanded = !isAppearanceExpanded },
                onDismissSubmenu = { isAppearanceExpanded = false },
                currentAppearance = state.bookmarkAppearance,
                currentCardImageSizing = state.cardImageSizing,
                onAppearanceSelection = { appearance ->
                    onEvent(TagDetailEvent.OnChangeAppearance(appearance = appearance))
                    onDismissRequest()
                },
                onCardSizingSelection = { sizing ->
                    onEvent(
                        TagDetailEvent.OnChangeAppearance(
                            appearance = BookmarkAppearance.CARD,
                            cardImageSizing = sizing
                        )
                    )
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
    currentCardImageSizing: CardImageSizing,
    onAppearanceSelection: (BookmarkAppearance) -> Unit,
    onCardSizingSelection: (CardImageSizing) -> Unit,
) {
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
                                                        && currentCardImageSizing == sizing,
                                                onCheckedChange = { _ ->
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

