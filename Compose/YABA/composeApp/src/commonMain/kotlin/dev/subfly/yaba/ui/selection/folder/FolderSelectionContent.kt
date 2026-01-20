package dev.subfly.yaba.ui.selection.folder

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.ButtonDefaults
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
import dev.subfly.yaba.core.components.item.folder.MoveToRootFolderItemView
import dev.subfly.yaba.core.components.item.folder.PresentableFolderItemView
import dev.subfly.yaba.core.navigation.creation.FolderCreationRoute
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.selection.FolderSelectionEvent
import dev.subfly.yabacore.state.selection.FolderSelectionUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.folder_search_prompt
import yaba.composeapp.generated.resources.select_folder_no_folder_found_in_search_description
import yaba.composeapp.generated.resources.select_folder_no_folder_found_in_search_title
import yaba.composeapp.generated.resources.select_folder_no_folders_available_description
import yaba.composeapp.generated.resources.select_folder_no_folders_available_title
import yaba.composeapp.generated.resources.select_folder_title
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun FolderSelectionContent(
    mode: FolderSelectionMode,
    contextFolderId: String?,
    contextBookmarkIds: List<String>?,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current
    val appStateManager = LocalAppStateManager.current

    val vm = viewModel { FolderSelectionVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.onEvent(
            FolderSelectionEvent.OnInit(
                mode = mode,
                contextFolderId = contextFolderId,
                contextBookmarkIds = contextBookmarkIds,
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9F)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(mode = mode)
        SearchBarContent(
            currentQuery = state.searchQuery,
            onQueryChange = { newQuery ->
                vm.onEvent(
                    event = FolderSelectionEvent.OnSearchQueryChanged(query = newQuery)
                )
            },
            onSearch = { finalQuery ->
                vm.onEvent(
                    event = FolderSelectionEvent.OnSearchQueryChanged(query = finalQuery)
                )
            },
            onClear = {
                vm.onEvent(
                    event = FolderSelectionEvent.OnSearchQueryChanged(query = "")
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SelectionContent(
            state = state,
            onMoveToRootFolder = {
                vm.onEvent(event = FolderSelectionEvent.OnMoveFolderToSelected(targetFolderId = null))
                appStateManager.onHideCreationContent()
                creationNavigator.removeLastOrNull()
            },
            onFolderSelected = { selectedFolder ->
                when (mode) {
                    FolderSelectionMode.FOLDER_SELECTION, FolderSelectionMode.PARENT_SELECTION -> {
                        resultStore.setResult(
                            key = ResultStoreKeys.SELECTED_FOLDER,
                            value = selectedFolder,
                        )
                        creationNavigator.removeLastOrNull()
                    }

                    FolderSelectionMode.BOOKMARKS_MOVE -> {
                        vm.onEvent(
                            event = FolderSelectionEvent.OnMoveBookmarksToSelected(
                                targetFolderId = selectedFolder.id.toString()
                            )
                        )
                        appStateManager.onHideCreationContent()
                        creationNavigator.removeLastOrNull()
                    }

                    FolderSelectionMode.FOLDER_MOVE -> {
                        vm.onEvent(
                            event = FolderSelectionEvent.OnMoveFolderToSelected(
                                targetFolderId = selectedFolder.id.toString()
                            )
                        )
                        appStateManager.onHideCreationContent()
                        creationNavigator.removeLastOrNull()
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    mode: FolderSelectionMode,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(
            containerColor = Color.Transparent,
        ),
        title = { Text(text = stringResource(Res.string.select_folder_title)) },
        navigationIcon = {
            when (mode) {
                FolderSelectionMode.FOLDER_SELECTION, FolderSelectionMode.PARENT_SELECTION -> {
                    IconButton(onClick = creationNavigator::removeLastOrNull) {
                        YabaIcon(name = "arrow-left-01")
                    }
                }

                FolderSelectionMode.BOOKMARKS_MOVE, FolderSelectionMode.FOLDER_MOVE -> {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            appStateManager.onHideCreationContent()
                            creationNavigator.removeLastOrNull()
                        },
                        colors =
                            ButtonDefaults.textButtonColors()
                                .copy(
                                    contentColor = MaterialTheme.colorScheme.error,
                                )
                    ) { Text(text = stringResource(Res.string.cancel)) }
                }
            }
        },
        actions = {
            IconButton(onClick = { creationNavigator.add(FolderCreationRoute(folderId = null)) }) {
                YabaIcon(name = "folder-add")
            }
        }
    )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        state = searchbarState,
        inputField = {
            SearchBarDefaults.InputField(
                query = currentQuery,
                expanded = false,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onExpandedChange = {},
                placeholder = { Text(text = stringResource(Res.string.folder_search_prompt)) },
                leadingIcon = { YabaIcon(name = "search-01") },
                trailingIcon = { IconButton(onClick = onClear) { YabaIcon(name = "cancel-01") } }
            )
        }
    )
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalUuidApi::class
)
@Composable
private fun SelectionContent(
    state: FolderSelectionUIState,
    onFolderSelected: (FolderUiModel) -> Unit,
    onMoveToRootFolder: () -> Unit,
) {
    AnimatedContent(
        targetState = state.isLoading,
    ) { isLoading ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularWavyProgressIndicator() }
        } else {
            if (state.folders.isEmpty()) {
                if (state.searchQuery.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        NoContentView(
                            iconName = "folder-01",
                            labelRes = Res.string.select_folder_no_folders_available_title,
                            message = { Text(text = stringResource(Res.string.select_folder_no_folders_available_description)) },
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        NoContentView(
                            modifier = Modifier,
                            iconName = "search-01",
                            labelRes = Res.string.select_folder_no_folder_found_in_search_title,
                            message = { Text(text = stringResource(Res.string.select_folder_no_folder_found_in_search_description, state.searchQuery)) },
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.canMoveToRoot) {
                        item {
                            MoveToRootFolderItemView(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(bottom = 8.dp),
                                onClick = onMoveToRootFolder,
                            )
                        }
                    }
                    items(
                        items = state.folders,
                        key = { it.id.toString() },
                    ) { model ->
                        PresentableFolderItemView(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            model = model,
                            nullModelPresentableColor = YabaColor.BLUE,
                            onPressed = { onFolderSelected(model) },
                            cornerSize = 12.dp,
                        )
                    }
                }
            }
        }
    }
}
