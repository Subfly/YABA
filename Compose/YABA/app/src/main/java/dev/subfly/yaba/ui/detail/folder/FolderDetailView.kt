package dev.subfly.yaba.ui.detail.folder

import android.annotation.SuppressLint
import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.item.bookmark.BookmarkItemView
import dev.subfly.yaba.core.navigation.alert.DeletionState
import dev.subfly.yaba.core.navigation.alert.DeletionType
import dev.subfly.yaba.core.navigation.creation.BookmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.DocmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.ImagemarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.LinkmarkCreationRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkCreationRoute
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.core.components.layout.ContentLayoutConfig
import dev.subfly.yaba.core.components.layout.YabaBookmarkLayout
import dev.subfly.yaba.ui.detail.composables.CollectionDetailSearchTopBar
import dev.subfly.yaba.ui.detail.composables.collectionDetailAccentColor
import dev.subfly.yaba.ui.detail.composables.collectionDetailIconButtonColors
import dev.subfly.yaba.ui.detail.composables.collectionDetailSearchFieldTint
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalDeletionDialogManager
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.LocalUserPreferences
import dev.subfly.yaba.util.BookmarkPrivatePasswordEventEffect
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberPrivateBookmarkOpenClick
import dev.subfly.yaba.util.rememberShareHandler
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.filesystem.access.YabaFileAccessor
import dev.subfly.yaba.core.managers.LinkmarkManager
import dev.subfly.yaba.core.model.utils.BookmarkAppearance
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.CardImageSizing
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.SortOrderType
import dev.subfly.yaba.core.model.utils.SortType
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.model.utils.uiIconName
import dev.subfly.yaba.core.state.detail.folder.FolderDetailEvent
import dev.subfly.yaba.core.state.detail.folder.FolderDetailUIState
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FolderDetailView(
    modifier: Modifier = Modifier,
    folderId: String,
) {
    val userPreferences = LocalUserPreferences.current
    val navigator = LocalContentNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val shareUrl = rememberShareHandler()
    val shareScope = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()

    var searchHasFocus by remember { mutableStateOf(false) }

    val vm = viewModel { FolderDetailVM() }
    val state by vm.state.collectAsStateWithLifecycle()
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val deletionDialogManager = LocalDeletionDialogManager.current

    BookmarkPrivatePasswordEventEffect(
        resolveBookmark = { id -> state.bookmarks.find { it.id == id } },
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
                    onConfirm = { vm.onEvent(FolderDetailEvent.OnDeleteBookmark(bookmark = bookmark)) },
                ),
            )
        },
    )

    LaunchedEffect(folderId) {
        vm.onEvent(FolderDetailEvent.OnInit(folderId = folderId))
    }

    val color = remember(state.folder) { collectionDetailAccentColor(state.folder?.color) }

    Scaffold(
        modifier = modifier.motionEventSpy {
            if (searchHasFocus) {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        },
    ) { _ ->
        val isLoadingEmpty = state.isLoading && state.bookmarks.isEmpty()
        val isEmptyDefault = state.query.isEmpty() && state.bookmarks.isEmpty()
        val isEmptySearch = state.query.isNotEmpty() && state.bookmarks.isEmpty()
        val isShowingBookmarks = state.bookmarks.isNotEmpty()

        val itemModifier = Modifier.padding(
            horizontal = if (userPreferences.preferredBookmarkAppearance == BookmarkAppearance.CARD) {
                12.dp
            } else 0.dp
        )

        val splitPinned = remember(state.bookmarks) {
            state.bookmarks.any { it.isPinned }
        }
        val pinnedList = remember(state.bookmarks, splitPinned) {
            if (splitPinned) state.bookmarks.filter { it.isPinned } else emptyList()
        }
        val unpinnedList = remember(state.bookmarks, splitPinned) {
            if (splitPinned) state.bookmarks.filter { !it.isPinned } else state.bookmarks
        }

        Box(modifier = Modifier.fillMaxSize()) {
            YabaBookmarkLayout(
                modifier = Modifier.fillMaxSize(),
                bookmarks = if (isShowingBookmarks) unpinnedList else emptyList(),
                pinnedSectionHeader = if (isShowingBookmarks && splitPinned) {
                    { PinnedBookmarksSectionHeader() }
                } else null,
                pinnedBookmarks = if (isShowingBookmarks) pinnedList else emptyList(),
                layoutConfig = ContentLayoutConfig(
                    bookmarkAppearance = userPreferences.preferredBookmarkAppearance,
                    cardImageSizing = userPreferences.preferredCardImageSizing,
                    headlineSpacerSizing = 8.dp,
                    gridForceApplyPadding = true,
                ),
                onDrop = {},
                stickyHeaderContent = {
                    FolderSearchToolbar(
                        state = state,
                        color = color,
                        searchBarState = searchBarState,
                        onFocusChanged = { searchHasFocus = it },
                        onQueryChanged = { newQuery ->
                            vm.onEvent(FolderDetailEvent.OnChangeQuery(query = newQuery))
                        },
                        onSearch = { finalQuery ->
                            vm.onEvent(FolderDetailEvent.OnChangeQuery(query = finalQuery))
                        },
                        onClear = { vm.onEvent(FolderDetailEvent.OnChangeQuery("")) },
                        onEvent = vm::onEvent,
                    )
                },
                itemContent = { model, _, appearance, cardImageSizing, index, count ->
                    val openBookmark = rememberPrivateBookmarkOpenClick(model) {
                        navigator.add(
                            when (model.kind) {
                                BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = model.id)
                                BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = model.id)
                                BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = model.id)
                                BookmarkKind.FILE -> DocDetailRoute(bookmarkId = model.id)
                            },
                        )
                    }
                    BookmarkItemView(
                        modifier = itemModifier,
                        model = model,
                        appearance = appearance,
                        cardImageSizing = cardImageSizing,
                        isAddedToSelection = model.id in state.selectedBookmarkIds,
                        onClick = {
                            if (state.isSelectionMode) {
                                vm.onEvent(
                                    FolderDetailEvent.OnToggleBookmarkSelection(
                                        bookmarkId = model.id
                                    )
                                )
                            } else {
                                openBookmark()
                            }
                        },
                        onDeleteBookmark = { bookmark ->
                            vm.onEvent(FolderDetailEvent.OnDeleteBookmark(bookmark = bookmark))
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
                },
            )

            when {
                isLoadingEmpty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularWavyProgressIndicator() }
                }

                isEmptyDefault -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        NoContentView(
                            iconName = "bookmark-02",
                            labelRes = R.string.no_bookmarks_title,
                            message = { Text(text = stringResource(R.string.no_bookmarks_message)) },
                        )
                    }
                }

                isEmptySearch -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderSearchToolbar(
    state: FolderDetailUIState,
    color: YabaColor,
    searchBarState: SearchBarState,
    onFocusChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    onEvent: (FolderDetailEvent) -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current
    val navigator = LocalContentNavigator.current

    val searchFieldTint by remember {
        derivedStateOf { collectionDetailSearchFieldTint(color) }
    }
    val topBarIconButtonColors = collectionDetailIconButtonColors(color)

    CollectionDetailSearchTopBar(
        accentColor = color,
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
                colors =
                    SearchBarDefaults.inputFieldColors(
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
                placeholder = {
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(
                            R.string.search_collection,
                            state.folder?.label ?: ""
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
            FolderDetailOptionsMenu(
                iconButtonColors = topBarIconButtonColors,
                state = state,
                onEvent = onEvent,
                onNewBookmark = {
                    val folder = state.folder ?: return@FolderDetailOptionsMenu
                    resultStore.setResult(ResultStoreKeys.SELECTED_FOLDER, folder)
                    creationNavigator.add(BookmarkCreationRoute())
                    appStateManager.onShowCreationContent()
                },
                onMoveSelection = {
                    creationNavigator.add(
                        FolderSelectionRoute(
                            mode = FolderSelectionMode.BOOKMARKS_MOVE,
                            contextFolderId = state.folder?.id,
                            contextBookmarkIds = state.selectedBookmarkIds.map { it },
                        )
                    )
                    appStateManager.onShowCreationContent()
                },
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FolderDetailOptionsMenu(
    iconButtonColors: IconButtonColors,
    state: FolderDetailUIState,
    onEvent: (FolderDetailEvent) -> Unit,
    onNewBookmark: () -> Unit,
    onMoveSelection: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(
            onClick = { isMenuExpanded = !isMenuExpanded },
            colors = iconButtonColors,
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "more-horizontal-circle-02") }

        FolderDetailDropdownMenu(
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            state = state,
            onEvent = onEvent,
            onNewBookmark = onNewBookmark,
            onMoveSelection = onMoveSelection,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalUuidApi::class)
@Composable
private fun FolderDetailDropdownMenu(
    isExpanded: Boolean,
    onDismissRequest: () -> Unit,
    state: FolderDetailUIState,
    onEvent: (FolderDetailEvent) -> Unit,
    onNewBookmark: () -> Unit,
    onMoveSelection: () -> Unit,
) {
    val deletionDialogManager = LocalDeletionDialogManager.current
    val appStateManager = LocalAppStateManager.current

    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isSortingExpanded by remember { mutableStateOf(false) }
    var isSortOrderExpanded by remember { mutableStateOf(false) }

    val newBookmarkText = stringResource(R.string.new_bookmark)
    val selectText = stringResource(R.string.bookmark_selection_enable)
    val cancelSelectionText = stringResource(R.string.bookmark_selection_cancel)
    val moveSelectionText = stringResource(R.string.bookmark_selection_move)
    val deleteSelectionText = stringResource(R.string.bookmark_selection_delete)

    val isSystemFolder =
        state.folder?.let { CoreConstants.Folder.isSystemFolder(it.id) } == true

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
                            shapes = MenuDefaults.itemShape(0, 3),
                            checked = false,
                            enabled = state.selectedBookmarkIds.isNotEmpty(),
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                if (state.selectedBookmarkIds.isNotEmpty()) {
                                    onMoveSelection()
                                }
                            },
                            leadingIcon = { YabaIcon(name = "arrow-move-up-right") },
                            text = { Text(text = moveSelectionText) },
                        )
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(1, 3),
                            checked = false,
                            enabled = state.selectedBookmarkIds.isNotEmpty(),
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                if (state.selectedBookmarkIds.isNotEmpty()) {
                                    deletionDialogManager.send(
                                        DeletionState(
                                            deletionType = DeletionType.BOOKMARKS,
                                            onConfirm = { onEvent(FolderDetailEvent.OnDeleteSelected) },
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
                            shapes = MenuDefaults.itemShape(2, 3),
                            checked = false,
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                onEvent(FolderDetailEvent.OnToggleSelectionMode)
                            },
                            leadingIcon = { YabaIcon(name = "cancel-circle") },
                            text = { Text(text = cancelSelectionText) },
                        )
                    }
                } else {
                    Column {
                        if (!isSystemFolder) {
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
                        }
                        DropdownMenuItem(
                            shapes = MenuDefaults.itemShape(
                                if (isSystemFolder) 0 else 1,
                                if (isSystemFolder) 1 else 2,
                            ),
                            checked = false,
                            onCheckedChange = { _ ->
                                onDismissRequest()
                                onEvent(FolderDetailEvent.OnToggleSelectionMode)
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
                    onEvent(FolderDetailEvent.OnChangeAppearance(appearance = appearance))
                    onDismissRequest()
                },
                onCardSizingSelection = { sizing ->
                    onEvent(
                        FolderDetailEvent.OnChangeAppearance(
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
                        FolderDetailEvent.OnChangeSort(
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
                        FolderDetailEvent.OnChangeSort(
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

@Composable
private fun PinnedBookmarksSectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YabaIcon(name = "pin", color = YabaColor.YELLOW)
        Text(
            text = "Pinned Bookmarks", // TODO: LOCALIZATION
            style = MaterialTheme.typography.titleSmall,
        )
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
