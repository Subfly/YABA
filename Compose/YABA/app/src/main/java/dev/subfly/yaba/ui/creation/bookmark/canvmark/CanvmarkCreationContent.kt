package dev.subfly.yaba.ui.creation.bookmark.canvmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.R
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.ui.TagUiModel
import dev.subfly.yaba.core.model.utils.FolderSelectionMode
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.core.navigation.main.CanvasDetailRoute
import dev.subfly.yaba.core.state.creation.canvmark.CanvmarkCreationEvent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkCreationTopBar
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkFolderSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkInfoContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewAppearanceSwitcher
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewCard
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkTagSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.model.BookmarkPreviewData
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberPrivateBookmarkCreationToggle

@Composable
fun CanvmarkCreationContent(bookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val contentNavigator = LocalContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { CanvmarkCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    val onPrivateToggle = rememberPrivateBookmarkCreationToggle {
        vm.onEvent(CanvmarkCreationEvent.OnTogglePrivate)
    }

    LaunchedEffect(bookmarkId) {
        vm.onEvent(
            CanvmarkCreationEvent.OnInit(
                canvmarkIdString = bookmarkId,
            ),
        )
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_FOLDER)) {
        resultStore.getResult<FolderUiModel>(ResultStoreKeys.SELECTED_FOLDER)?.let { folder ->
            vm.onEvent(CanvmarkCreationEvent.OnSelectFolder(folder))
            resultStore.removeResult(ResultStoreKeys.SELECTED_FOLDER)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_TAGS)) {
        resultStore.getResult<List<TagUiModel>>(ResultStoreKeys.SELECTED_TAGS)?.let { tags ->
            vm.onEvent(CanvmarkCreationEvent.OnSelectTags(tags))
            resultStore.removeResult(ResultStoreKeys.SELECTED_TAGS)
        }
    }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .fillMaxHeight(0.9f)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        BookmarkCreationTopBar(
            canPerformDone = state.canSave,
            isEditing = state.isInEditMode,
            isSaving = state.isSaving,
            onDone = {
                vm.onEvent(
                    CanvmarkCreationEvent.OnSave(
                        onSavedCallback = { id ->
                            if (creationNavigator.size == 2) {
                                appStateManager.onHideCreationContent()
                            }
                            creationNavigator.removeLastOrNull()
                            contentNavigator.add(CanvasDetailRoute(bookmarkId = id))
                        },
                        onErrorCallback = {},
                    ),
                )
            },
        )

        LazyColumn {
            item {
                BookmarkPreviewContent(
                    label = stringResource(R.string.preview),
                    iconName = "pen-tool-02",
                    extraContent = {
                        BookmarkPreviewAppearanceSwitcher(
                            bookmarkAppearance = state.bookmarkAppearance,
                            cardImageSizing = state.cardImageSizing,
                            color = state.selectedFolder?.color ?: YabaColor.PURPLE,
                            onClick = {
                                vm.onEvent(CanvmarkCreationEvent.OnCyclePreviewAppearance)
                            },
                        )
                    },
                    content = {
                        BookmarkPreviewCard(
                            data =
                                BookmarkPreviewData(
                                    imageData = null,
                                    domainImageData = null,
                                    label = state.label,
                                    description = state.description,
                                    selectedFolder = state.selectedFolder,
                                    selectedTags = state.selectedTags,
                                    isLoading = false,
                                    emptyImageIconName = "pen-tool-02",
                                ),
                            bookmarkAppearance = state.bookmarkAppearance,
                            cardImageSizing = state.cardImageSizing,
                            onClick = {},
                        )
                    },
                )
            }
            item {
                BookmarkInfoContent(
                    label = state.label,
                    description = state.description,
                    onChangeLabel = { vm.onEvent(CanvmarkCreationEvent.OnChangeLabel(it)) },
                    onChangeDescription = {
                        vm.onEvent(CanvmarkCreationEvent.OnChangeDescription(it))
                    },
                    selectedFolder = state.selectedFolder,
                    isPrivate = state.isPrivate,
                    isPinned = state.isPinned,
                    onPrivateToggle = onPrivateToggle,
                    onPinToggle = { vm.onEvent(CanvmarkCreationEvent.OnTogglePinned) },
                    enabled = true,
                    labelPlaceholder = R.string.create_bookmark_title_placeholder,
                    nullModelPresentableColor = YabaColor.PURPLE,
                )
            }
            item {
                BookmarkFolderSelectionContent(
                    selectedFolder = state.selectedFolder,
                    onSelectFolder = {
                        creationNavigator.add(
                            FolderSelectionRoute(
                                mode = FolderSelectionMode.FOLDER_SELECTION,
                                contextFolderId = null,
                                contextBookmarkIds = null,
                            ),
                        )
                    },
                    nullModelPresentableColor = YabaColor.PURPLE,
                )
            }
            item {
                BookmarkTagSelectionContent(
                    selectedFolder = state.selectedFolder,
                    selectedTags = state.selectedTags,
                    onSelectTags = {
                        creationNavigator.add(
                            TagSelectionRoute(
                                selectedTagIds =
                                    state.selectedTags.map { tag -> tag.id },
                            ),
                        )
                    },
                    onNavigateToEdit = { tag ->
                        creationNavigator.add(TagCreationRoute(tagId = tag.id))
                    },
                    nullModelPresentableColor = YabaColor.PURPLE,
                )
            }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}
