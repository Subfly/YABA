package dev.subfly.yaba.ui.creation.bookmark.imagemark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkCreationTopBar
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkFolderSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkInfoContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewAppearanceSwitcher
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewCard
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkTagSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.model.BookmarkPreviewData
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.SharedImageData
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationEvent
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationUIState
import dev.subfly.yabacore.util.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_title_placeholder
import yaba.composeapp.generated.resources.preview
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ImagemarkCreationContent(bookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { ImagemarkCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(bookmarkId) {
        vm.onEvent(
            ImagemarkCreationEvent.OnInit(
                imagemarkIdString = bookmarkId,
            )
        )
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_FOLDER)) {
        resultStore.getResult<FolderUiModel>(ResultStoreKeys.SELECTED_FOLDER)?.let { newFolder ->
            vm.onEvent(ImagemarkCreationEvent.OnSelectFolder(folder = newFolder))
            resultStore.removeResult(ResultStoreKeys.SELECTED_FOLDER)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_TAGS)) {
        resultStore.getResult<List<TagUiModel>>(ResultStoreKeys.SELECTED_TAGS)?.let { newTags ->
            vm.onEvent(ImagemarkCreationEvent.OnSelectTags(tags = newTags))
            resultStore.removeResult(ResultStoreKeys.SELECTED_TAGS)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SHARED_IMAGE_DATA)) {
        resultStore.getResult<SharedImageData>(
            ResultStoreKeys.SHARED_IMAGE_DATA
        )?.let { imageData ->
            vm.onEvent(
                ImagemarkCreationEvent.OnImageFromShare(
                    bytes = imageData.bytes,
                    extension = imageData.extension,
                )
            )
            resultStore.removeResult(ResultStoreKeys.SHARED_IMAGE_DATA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        BookmarkCreationTopBar(
            canPerformDone = state.canSave,
            isEditing = state.editingImagemark != null,
            isSaving = state.isSaving,
            onDone = {
                vm.onEvent(
                    ImagemarkCreationEvent.OnSave(
                        onSavedCallback = {
                            if (creationNavigator.size == 2) {
                                appStateManager.onHideCreationContent()
                            }
                            creationNavigator.removeLastOrNull()
                        },
                        onErrorCallback = {}
                    )
                )
            },
        )
        LazyColumn {
            item {
                ImagemarkPreviewContent(
                    state = state,
                    onChangePreviewType = { vm.onEvent(ImagemarkCreationEvent.OnCyclePreviewAppearance) },
                    onPickFromGallery = { vm.onEvent(ImagemarkCreationEvent.OnPickFromGallery) },
                    onCaptureFromCamera = { vm.onEvent(ImagemarkCreationEvent.OnCaptureFromCamera) },
                )
            }
            item {
                BookmarkInfoContent(
                    label = state.label,
                    description = state.description,
                    onChangeLabel = { vm.onEvent(ImagemarkCreationEvent.OnChangeLabel(it)) },
                    onChangeDescription = { vm.onEvent(ImagemarkCreationEvent.OnChangeDescription(it)) },
                    selectedFolder = state.selectedFolder,
                    enabled = state.isLoading.not(),
                    labelPlaceholder = Res.string.create_bookmark_title_placeholder,
                )
            }
            item {
                BookmarkFolderSelectionContent(
                    selectedFolder = state.selectedFolder,
                    onSelectFolder = {
                        creationNavigator.add(
                            FolderSelectionRoute(
                                mode = dev.subfly.yabacore.model.utils.FolderSelectionMode.FOLDER_SELECTION,
                                contextFolderId = null,
                                contextBookmarkIds = null,
                            )
                        )
                    },
                    nullModelPresentableColor = YabaColor.BLUE,
                )
            }
            item {
                BookmarkTagSelectionContent(
                    selectedFolder = state.selectedFolder,
                    selectedTags = state.selectedTags,
                    onSelectTags = {
                        creationNavigator.add(
                            TagSelectionRoute(
                                selectedTagIds = state.selectedTags.map { it.id }
                            )
                        )
                    },
                    onNavigateToEdit = { tag ->
                        creationNavigator.add(TagCreationRoute(tagId = tag.id))
                    },
                    nullModelPresentableColor = YabaColor.BLUE,
                )
            }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImagemarkPreviewContent(
    state: ImagemarkCreationUIState,
    onChangePreviewType: () -> Unit,
    onPickFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
) {
    val color = state.selectedFolder?.color ?: YabaColor.BLUE

    BookmarkPreviewContent(
        label = stringResource(Res.string.preview),
        iconName = "image-03",
        extraContent = {
            BookmarkPreviewAppearanceSwitcher(
                bookmarkAppearance = state.bookmarkAppearance,
                cardImageSizing = state.cardImageSizing,
                color = color,
                onClick = onChangePreviewType,
            )
        },
        content = {
            BookmarkPreviewCard(
                data = BookmarkPreviewData(
                    imageData = state.imageBytes,
                    label = state.label,
                    description = state.description,
                    selectedFolder = state.selectedFolder,
                    selectedTags = state.selectedTags,
                    isLoading = state.isLoading,
                    emptyImageIconName = "image-03",
                ),
                bookmarkAppearance = state.bookmarkAppearance,
                cardImageSizing = state.cardImageSizing,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                onClick = onPickFromGallery,
                shapes = ButtonDefaults.shapes(),
                enabled = state.isLoading.not() && state.isInEditMode.not(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(color.iconTintArgb()),
                ),
            ) {
                YabaIcon(
                    modifier = Modifier.padding(end = 8.dp),
                    name = "add-circle",
                    color = Color.White,
                )
                Text(
                    text = "Pick From Gallery",
                    color = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                onClick = onCaptureFromCamera,
                shapes = ButtonDefaults.shapes(),
                enabled = state.isLoading.not() && state.isInEditMode.not(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(color.iconTintArgb()),
                ),
            ) {
                YabaIcon(
                    modifier = Modifier.padding(end = 8.dp),
                    name = "camera-01",
                    color = Color.White,
                )
                Text(
                    text = "Take Photo",
                    color = Color.White,
                )
            }
        },
    )
}
