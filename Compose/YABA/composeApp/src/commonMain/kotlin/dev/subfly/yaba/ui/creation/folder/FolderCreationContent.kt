package dev.subfly.yaba.ui.creation.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.item.folder.PresentableParentFolderItem
import dev.subfly.yaba.core.navigation.creation.ColorSelectionRoute
import dev.subfly.yaba.core.navigation.creation.IconCategorySelectionRoute
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.folder.FolderCreationEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.create_folder_placeholder
import yaba.composeapp.generated.resources.create_folder_title
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.edit_folder_title

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun FolderCreationContent(
    folderId: String? = null,
    onDismiss: () -> Unit,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val resultStore = LocalResultStore.current
    val vm = viewModel<FolderCreationVM>()
    val state by vm.state

    LaunchedEffect(folderId) {
        folderId?.let { nonNullId ->
            vm.onEvent(event = FolderCreationEvent.OnInitWithFolder(folderIdString = nonNullId))
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_COLOR)) {
        resultStore.getResult<YabaColor>(ResultStoreKeys.SELECTED_COLOR)?.let { newColor ->
            vm.onEvent(FolderCreationEvent.OnSelectNewColor(newColor = newColor))
            resultStore.removeResult(ResultStoreKeys.SELECTED_COLOR)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_ICON)) {
        resultStore.getResult<String>(ResultStoreKeys.SELECTED_ICON)?.let { newIcon ->
            vm.onEvent(FolderCreationEvent.OnSelectNewIcon(newIcon = newIcon))
            resultStore.removeResult(ResultStoreKeys.SELECTED_ICON)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            isStartingFlow = creationNavigator.size <= 2,
            canPerformDone = state.label.isNotBlank(),
            isEditing = state.editingFolder != null,
            onDone = { vm.onEvent(FolderCreationEvent.OnSave) },
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(12.dp))
        CreationContent(
            folderLabel = state.label,
            selectedIcon = state.selectedIcon,
            selectedColor = state.selectedColor,
            onChangeLabel = { newLabel ->
                vm.onEvent(
                    event = FolderCreationEvent.OnChangeLabel(newLabel = newLabel)
                )
            },
            onOpenIconSelection = {
                creationNavigator.add(
                    IconCategorySelectionRoute(
                        selectedIcon = state.selectedIcon,
                    )
                )
            },
            onOpenColorSelection = {
                creationNavigator.add(
                    ColorSelectionRoute(
                        selectedColor = state.selectedColor,
                    )
                )
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        DescriptionContent(
            folderDescription = state.description,
            onChangeDescription = { newDescription ->
                vm.onEvent(
                    event = FolderCreationEvent.OnChangeDescription(newDescription = newDescription)
                )
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        PresentableParentFolderItem(
            modifier = Modifier
                .height(60.dp)
                .padding(horizontal = 12.dp),
            model = state.selectedParent,
            onPressed = {
                // TODO: NAVIGATE TO PARENT SELECTION
            },
            onNavigateToEdit = {
                // TODO: NAVIGATE TO EDIT FOLDER
            },
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    isStartingFlow: Boolean,
    isEditing: Boolean,
    canPerformDone: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors()
                .copy(
                    containerColor = Color.Transparent,
                ),
        title = {
            Text(
                text =
                    stringResource(
                        resource =
                            if (isEditing) {
                                Res.string.edit_folder_title
                            } else {
                                Res.string.create_folder_title
                            }
                    ),
            )
        },
        navigationIcon = {
            if (isStartingFlow) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onDismiss,
                    colors =
                        ButtonDefaults.textButtonColors()
                            .copy(
                                contentColor = MaterialTheme.colorScheme.error,
                            )
                ) { Text(text = stringResource(Res.string.cancel)) }
            } else {
                IconButton(onClick = onDismiss) { YabaIcon(name = "arrow-left-01") }
            }
        },
        actions = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                enabled = canPerformDone,
                onClick = {
                    onDone()
                    onDismiss()
                }
            ) { Text(text = stringResource(Res.string.done)) }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreationContent(
    folderLabel: String,
    selectedColor: YabaColor,
    selectedIcon: String,
    onChangeLabel: (String) -> Unit,
    onOpenIconSelection: () -> Unit,
    onOpenColorSelection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier.weight(1F).height(60.dp),
            colors =
                ButtonDefaults.buttonColors()
                    .copy(
                        containerColor =
                            Color(selectedColor.iconTintArgb())
                                .copy(alpha = 0.2F)
                    ),
            shapes = ButtonDefaults.shapes(),
            onClick = onOpenIconSelection,
        ) {
            YabaIcon(
                name = selectedIcon,
                color = selectedColor,
            )
        }
        OutlinedTextField(
            modifier = Modifier.weight(4F).fillMaxWidth(),
            value = folderLabel,
            onValueChange = onChangeLabel,
            maxLines = 1,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text(text = stringResource(Res.string.create_folder_placeholder)) },
        )
        Button(
            modifier = Modifier.weight(1F).height(60.dp),
            colors =
                ButtonDefaults.buttonColors()
                    .copy(
                        containerColor =
                            Color(selectedColor.iconTintArgb())
                                .copy(alpha = 0.2F)
                    ),
            shapes = ButtonDefaults.shapes(),
            onClick = onOpenColorSelection,
        ) {
            YabaIcon(
                name = "paint-board",
                color = selectedColor,
            )
        }
    }
}

@Composable
private fun DescriptionContent(
    folderDescription: String,
    onChangeDescription: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier
            .heightIn(min = 60.dp, max = 120.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        value = folderDescription,
        onValueChange = onChangeDescription,
        shape = RoundedCornerShape(24.dp),
        placeholder = { Text(text = "TODO: ADD A PLACEHOLDER FOR DESCRIPTION IN HERE") },
        leadingIcon = { YabaIcon(name = "paragraph") }
    )
}
