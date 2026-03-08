package dev.subfly.yaba.ui.creation.bookmark.imagemark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.folder.PresentableFolderItemView
import dev.subfly.yaba.core.components.item.tag.PresentableTagItemView
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.SharedImageData
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkLabel
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationEvent
import dev.subfly.yabacore.state.creation.imagemark.ImagemarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_type_image
import yaba.composeapp.generated.resources.create_bookmark_add_tags
import yaba.composeapp.generated.resources.create_bookmark_description_placeholder
import yaba.composeapp.generated.resources.create_bookmark_edit_tags
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_description
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_title
import yaba.composeapp.generated.resources.create_bookmark_title_placeholder
import yaba.composeapp.generated.resources.folder
import yaba.composeapp.generated.resources.info
import yaba.composeapp.generated.resources.tags_title
import kotlin.uuid.ExperimentalUuidApi

// TODO: LOCALIZATIONS
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
        LinkmarkTopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
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
                        onErrorCallback = {

                        }
                    )
                )
            },
            onDismiss = {
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )
        LazyColumn {
            item {
                ImagemarkImagePickerContent(
                    state = state,
                    onPickFromGallery = { vm.onEvent(ImagemarkCreationEvent.OnPickFromGallery) },
                    onCaptureFromCamera = { vm.onEvent(ImagemarkCreationEvent.OnCaptureFromCamera) },
                    onClearImage = { vm.onEvent(ImagemarkCreationEvent.OnClearImage) },
                )
            }
            item {
                ImagemarkInfoContent(
                    state = state,
                    onChangeLabel = { vm.onEvent(ImagemarkCreationEvent.OnChangeLabel(it)) },
                    onChangeDescription = { vm.onEvent(ImagemarkCreationEvent.OnChangeDescription(it)) },
                )
            }
            item {
                ImagemarkFolderSelectionContent(
                    state = state,
                    onSelectFolder = {
                        creationNavigator.add(
                            FolderSelectionRoute(
                                mode = dev.subfly.yabacore.model.utils.FolderSelectionMode.FOLDER_SELECTION,
                                contextFolderId = null,
                                contextBookmarkIds = null,
                            )
                        )
                    }
                )
            }
            item {
                ImagemarkTagSelectionContent(
                    state = state,
                    onSelectTags = {
                        creationNavigator.add(
                            TagSelectionRoute(
                                selectedTagIds = state.selectedTags.map { it.id }
                            )
                        )
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ImagemarkImagePickerContent(
    state: ImagemarkCreationUIState,
    onPickFromGallery: () -> Unit,
    onCaptureFromCamera: () -> Unit,
    onClearImage: () -> Unit,
) {
    val color = state.selectedFolder?.color ?: YabaColor.BLUE

    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.bookmark_type_image),
        iconName = "image-03",
        extraContent = {
            if (state.imageBytes != null && state.isInEditMode.not()) {
                TextButton(
                    onClick = onClearImage,
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.textButtonColors()
                        .copy(
                            contentColor = MaterialTheme.colorScheme.error,
                        )
                ) {
                    Text("Remove", color = Color(YabaColor.RED.iconTintArgb()))
                    YabaIcon(
                        modifier = Modifier.padding(start = 8.dp),
                        name = "cancel-01",
                        color = YabaColor.RED
                    )
                }
            }
        }
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (state.imageBytes != null) {
        YabaImage(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 240.dp)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)),
            bytes = state.imageBytes,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

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

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun ImagemarkInfoContent(
    state: ImagemarkCreationUIState,
    onChangeLabel: (String) -> Unit,
    onChangeDescription: (String) -> Unit,
) {
    val color = state.selectedFolder?.color ?: YabaColor.BLUE

    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(label = stringResource(Res.string.info), iconName = "information-circle")
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = !state.isLoading,
        value = state.label,
        onValueChange = onChangeLabel,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(Res.string.create_bookmark_title_placeholder)) },
        leadingIcon = { YabaIcon(name = "text", color = color) }
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        modifier = Modifier
            .heightIn(min = 80.dp, max = 160.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = !state.isLoading,
        value = state.description,
        onValueChange = onChangeDescription,
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text(stringResource(Res.string.create_bookmark_description_placeholder)) },
        leadingIcon = { YabaIcon(name = "paragraph", color = color) }
    )
}

@Composable
private fun ImagemarkFolderSelectionContent(
    state: ImagemarkCreationUIState,
    onSelectFolder: () -> Unit,
) {
    Spacer(modifier = Modifier.height(24.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.folder),
        iconName = "folder-01"
    )
    Spacer(modifier = Modifier.height(12.dp))
    PresentableFolderItemView(
        modifier = Modifier.padding(horizontal = 12.dp),
        model = state.selectedFolder,
        nullModelPresentableColor = YabaColor.BLUE,
        cornerSize = 12.dp,
        onPressed = onSelectFolder,
    )
}

@Composable
private fun ImagemarkTagSelectionContent(
    state: ImagemarkCreationUIState,
    onSelectTags: () -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val color = state.selectedFolder?.color ?: YabaColor.BLUE

    Spacer(modifier = Modifier.height(4.dp))
    LinkmarkLabel(
        label = stringResource(Res.string.tags_title),
        iconName = "tag-01",
        extraContent = {
            TextButton(
                onClick = onSelectTags,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(color.iconTintArgb())
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    YabaIcon(
                        name = if (state.selectedTags.isEmpty()) "plus-sign" else "edit-02",
                        color = color
                    )
                    Text(
                        text = stringResource(
                            if (state.selectedTags.isEmpty()) Res.string.create_bookmark_add_tags
                            else Res.string.create_bookmark_edit_tags
                        )
                    )
                }
            }
        }
    )
    if (state.selectedTags.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            NoContentView(
                modifier = Modifier.padding(12.dp).padding(vertical = 24.dp),
                iconName = "tag-01",
                labelRes = Res.string.create_bookmark_no_tags_selected_title,
                message = { Text(text = stringResource(Res.string.create_bookmark_no_tags_selected_description)) },
            )
        }
    } else {
        state.selectedTags.fastForEachIndexed { index, tag ->
            PresentableTagItemView(
                modifier = Modifier.padding(horizontal = 12.dp),
                model = tag,
                nullModelPresentableColor = YabaColor.BLUE,
                cornerSize = 12.dp,
                onPressed = { },
                onNavigateToEdit = { creationNavigator.add(TagCreationRoute(tagId = tag.id)) },
                index = index,
                count = state.selectedTags.size,
            )
        }
    }
}
