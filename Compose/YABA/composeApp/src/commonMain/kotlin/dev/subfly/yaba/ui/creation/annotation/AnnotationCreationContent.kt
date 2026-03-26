package dev.subfly.yaba.ui.creation.annotation

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.item.annotation.AnnotationPreviewItemView
import dev.subfly.yaba.core.navigation.creation.ColorSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkCreationLabel
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yabacore.model.annotation.AnnotationReadableCreateRequest
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.annotation.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.annotation.AnnotationCreationEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.cancel
import yaba.composeapp.generated.resources.done
import yaba.composeapp.generated.resources.preview

// TODO: LOCALIZATIONS
@Composable
fun AnnotationCreationContent(
    bookmarkId: String,
    selectionDraft: ReadableSelectionDraft? = null,
    annotationId: String? = null,
) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { AnnotationCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(selectionDraft) {
        selectionDraft?.let { draft ->
            vm.onEvent(AnnotationCreationEvent.OnInitWithSelection(draft = draft))
        }
    }

    LaunchedEffect(annotationId) {
        annotationId?.let { id ->
            vm.onEvent(
                AnnotationCreationEvent.OnInitWithAnnotation(
                    bookmarkId = bookmarkId,
                    annotationId = id,
                ),
            )
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_COLOR)) {
        resultStore.getResult<YabaColor>(ResultStoreKeys.SELECTED_COLOR)?.let { newColor ->
            vm.onEvent(AnnotationCreationEvent.OnSelectNewColor(newColor = newColor))
            resultStore.removeResult(ResultStoreKeys.SELECTED_COLOR)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(rememberScrollState()),
    ) {
        TopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            isStartingFlow = creationNavigator.size <= 2,
            canPerformDone = state.hasValidSelection,
            isEditing = state.isEditing,
            isSaving = state.isSaving,
            onDone = {
                when {
                    state.isEditing || state.selectionDraft?.pdfAnchor != null -> {
                        vm.onEvent(
                            AnnotationCreationEvent.OnSave(
                                onSavedCallback = {
                                    if (creationNavigator.size == 2) {
                                        appStateManager.onHideCreationContent()
                                    }
                                    creationNavigator.removeLastOrNull()
                                },
                                onErrorCallback = { /* TODO: Show global toast */ },
                            ),
                        )
                    }
                    state.selectionDraft != null -> {
                        resultStore.setResult(
                            ResultStoreKeys.ANNOTATION_READABLE_CREATE_REQUEST,
                            AnnotationReadableCreateRequest(
                                selectionDraft = state.selectionDraft!!,
                                colorRole = state.selectedColor,
                                note = state.note.ifBlank { null },
                            ),
                        )
                        if (creationNavigator.size == 2) {
                            appStateManager.onHideCreationContent()
                        }
                        creationNavigator.removeLastOrNull()
                    }
                }
            },
            onDismiss = {
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        BookmarkCreationLabel(
            iconName = "image-03",
            label = stringResource(Res.string.preview)
        )

        Spacer(modifier = Modifier.height(12.dp))

        AnnotationPreviewItemView(
            model = AnnotationUiModel(
                id = "creation-preview",
                type = AnnotationType.READABLE,
                colorRole = state.selectedColor,
                note = state.note.ifBlank { null },
                quoteText = state.quoteText,
                extrasJson = null,
                createdAt = 0L,
                editedAt = 0L,
            ),
            index = 0,
            count = 1,
        )

        Spacer(modifier = Modifier.height(24.dp))

        BookmarkCreationLabel(
            iconName = "sticky-note-03",
            label = "Note"
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            modifier = Modifier
                .heightIn(min = 120.dp, max = 240.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(state.selectedColor.iconTintArgb()),
                unfocusedBorderColor = Color(state.selectedColor.iconTintArgb()).copy(alpha = 0.5F),
            ),
            enabled = !state.isLoading,
            value = state.note,
            onValueChange = { vm.onEvent(AnnotationCreationEvent.OnChangeNote(it)) },
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text(text = "Note…") },
            leadingIcon = {
                YabaIcon(
                    name = "paragraph",
                    color = state.selectedColor,
                )
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        ColorSelectionContent(
            selectedColor = state.selectedColor,
            onPressed = {
                creationNavigator.add(
                    ColorSelectionRoute(
                        selectedColor = state.selectedColor,
                        allowTransparent = false,
                    ),
                )
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isEditing) {
            TextButton(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    val annotation = state.annotation
                    when {
                        annotation != null && annotation.type == AnnotationType.READABLE -> {
                            resultStore.setResult(ResultStoreKeys.ANNOTATION_READABLE_DELETE_REQUEST, annotation.id)
                            if (creationNavigator.size == 2) {
                                appStateManager.onHideCreationContent()
                            }
                            creationNavigator.removeLastOrNull()
                        }
                        else -> {
                            vm.onEvent(
                                AnnotationCreationEvent.OnDelete(
                                    onDeletedCallback = {
                                        if (creationNavigator.size == 2) {
                                            appStateManager.onHideCreationContent()
                                        }
                                        creationNavigator.removeLastOrNull()
                                    },
                                    onErrorCallback = { /* TODO: Show global toast */ },
                                ),
                            )
                        }
                    }
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                YabaIcon(name = "delete-02", color = YabaColor.RED)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = "Delete Annotation", color = Color(YabaColor.RED.iconTintArgb()))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    isStartingFlow: Boolean,
    isEditing: Boolean,
    canPerformDone: Boolean,
    isSaving: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
        title = {
            Text(
                text = if (isEditing) {
                    "Edit Annotation"
                } else {
                    "Create Annotation"
                },
            )
        },
        navigationIcon = {
            if (isStartingFlow) {
                TextButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors().copy(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            } else {
                IconButton(onClick = onDismiss) {
                    YabaIcon(name = "arrow-left-01")
                }
            }
        },
        actions = {
            AnimatedContent(isSaving) { saving ->
                if (saving) {
                    CircularWavyProgressIndicator()
                } else {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        enabled = canPerformDone,
                        onClick = onDone,
                    ) {
                        Text(text = stringResource(Res.string.done))
                    }
                }
            }
        },
    )
}

@Composable
private fun ColorSelectionContent(
    modifier: Modifier = Modifier,
    selectedColor: YabaColor,
    onPressed: () -> Unit,
) {
    BookmarkCreationLabel(
        modifier = modifier.fillMaxWidth(),
        label = "Selected Color:",
        iconName = "paint-board",
        extraContent = {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
                    .background(
                        color = Color(selectedColor.iconTintArgb()),
                        shape = CircleShape,
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    )
                    .clip(CircleShape)
                    .clickable(onClick = onPressed),
            )
        }
    )
}
