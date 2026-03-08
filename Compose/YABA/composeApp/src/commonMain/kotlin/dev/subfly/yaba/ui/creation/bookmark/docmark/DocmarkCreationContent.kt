@file:OptIn(ExperimentalEncodingApi::class)

package dev.subfly.yaba.ui.creation.bookmark.docmark

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.item.folder.PresentableFolderItemView
import dev.subfly.yaba.core.components.item.tag.PresentableTagItemView
import dev.subfly.yaba.core.components.webview.PdfConverterInput
import dev.subfly.yaba.core.components.webview.PdfTextSection
import dev.subfly.yaba.core.components.webview.YabaWebViewPdfConverter
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkLabel
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.creation.docmark.DocmarkCreationEvent
import dev.subfly.yabacore.state.creation.docmark.DocmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.image.YabaImage
import dev.subfly.yabacore.ui.webview.WebComponentUris
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_add_tags
import yaba.composeapp.generated.resources.create_bookmark_description_placeholder
import yaba.composeapp.generated.resources.create_bookmark_edit_tags
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_description
import yaba.composeapp.generated.resources.create_bookmark_no_tags_selected_title
import yaba.composeapp.generated.resources.create_bookmark_url_placeholder
import yaba.composeapp.generated.resources.folder
import yaba.composeapp.generated.resources.tags_title
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun DocmarkCreationContent(bookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current
    val vm = viewModel { DocmarkCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(bookmarkId) {
        vm.onEvent(
            DocmarkCreationEvent.OnInit(
                docmarkIdString = bookmarkId,
            ),
        )
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_FOLDER)) {
        resultStore.getResult<FolderUiModel>(ResultStoreKeys.SELECTED_FOLDER)?.let { folder ->
            vm.onEvent(DocmarkCreationEvent.OnSelectFolder(folder))
            resultStore.removeResult(ResultStoreKeys.SELECTED_FOLDER)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_TAGS)) {
        resultStore.getResult<List<TagUiModel>>(ResultStoreKeys.SELECTED_TAGS)?.let { tags ->
            vm.onEvent(DocmarkCreationEvent.OnSelectTags(tags))
            resultStore.removeResult(ResultStoreKeys.SELECTED_TAGS)
        }
    }

    val pdfDataUrl = remember(state.pdfBytes, state.isInEditMode) {
        if (state.isInEditMode) return@remember null
        state.pdfBytes?.let { bytes ->
            "data:application/pdf;base64,${Base64.encode(bytes)}"
        }
    }
    val pdfConverterInput = remember(pdfDataUrl, state.isInEditMode) {
        if (state.isInEditMode || pdfDataUrl == null) null
        else PdfConverterInput(pdfUrl = pdfDataUrl)
    }

    YabaWebViewPdfConverter(
        modifier = Modifier.size(0.dp),
        baseUrl = WebComponentUris.getConverterUri(),
        input = pdfConverterInput,
        onPdfConverterResult = { result ->
            val previewBytes = result.firstPagePngDataUrl?.let(::decodeDataUrlToBytes)
            vm.onEvent(
                DocmarkCreationEvent.OnSetGeneratedPreview(
                    imageBytes = previewBytes,
                    extension = "png",
                ),
            )
            vm.onEvent(
                DocmarkCreationEvent.OnSetInternalReadableMarkdown(
                    markdown = buildHiddenMarkdown(result.sections),
                ),
            )
        },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        LinkmarkTopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            canPerformDone = state.canSave,
            isEditing = state.isInEditMode,
            isSaving = state.isSaving,
            onDone = {
                vm.onEvent(
                    DocmarkCreationEvent.OnSave(
                        onSavedCallback = {
                            if (creationNavigator.size == 2) appStateManager.onHideCreationContent()
                            creationNavigator.removeLastOrNull()
                        },
                        onErrorCallback = {},
                    ),
                )
            },
            onDismiss = {
                if (creationNavigator.size == 2) appStateManager.onHideCreationContent()
                creationNavigator.removeLastOrNull()
            },
        )

        LazyColumn {
            item {
                DocmarkFilePickerContent(
                    state = state,
                    onPickPdf = { vm.onEvent(DocmarkCreationEvent.OnPickPdf) },
                    onClearPdf = { vm.onEvent(DocmarkCreationEvent.OnClearPdf) },
                )
            }
            item {
                DocmarkInfoContent(
                    state = state,
                    onChangeLabel = { vm.onEvent(DocmarkCreationEvent.OnChangeLabel(it)) },
                    onChangeDescription = { vm.onEvent(DocmarkCreationEvent.OnChangeDescription(it)) },
                )
            }
            item {
                DocmarkFolderSelectionContent(
                    state = state,
                    onSelectFolder = {
                        creationNavigator.add(
                            FolderSelectionRoute(
                                mode = FolderSelectionMode.FOLDER_SELECTION,
                                contextFolderId = null,
                                contextBookmarkIds = null,
                            ),
                        )
                    },
                )
            }
            item {
                DocmarkTagSelectionContent(
                    state = state,
                    onSelectTags = {
                        creationNavigator.add(
                            TagSelectionRoute(
                                selectedTagIds = state.selectedTags.map { tag -> tag.id },
                            ),
                        )
                    },
                )
            }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}

@Composable
private fun DocmarkFilePickerContent(
    state: DocmarkCreationUIState,
    onPickPdf: () -> Unit,
    onClearPdf: () -> Unit,
) {
    val color = state.selectedFolder?.color ?: YabaColor.RED
    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(
        label = "PDF",
        iconName = "doc-02",
        extraContent = {
            if (state.pdfBytes != null && state.isInEditMode.not()) {
                TextButton(
                    onClick = onClearPdf,
                    colors = ButtonDefaults.textButtonColors()
                        .copy(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Remove", color = Color(YabaColor.RED.iconTintArgb()))
                    YabaIcon(
                        modifier = Modifier.padding(start = 8.dp),
                        name = "cancel-01",
                        color = YabaColor.RED,
                    )
                }
            }
        },
    )
    Spacer(modifier = Modifier.height(12.dp))

    if (state.previewImageBytes != null) {
        YabaImage(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(horizontal = 12.dp),
            bytes = state.previewImageBytes,
        )
        Spacer(modifier = Modifier.height(8.dp))
    } else if (state.pdfBytes == null && state.isInEditMode.not()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "No PDF selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (state.sourceFileName != null || state.isInEditMode) {
        val caption = state.sourceFileName ?: "PDF file is locked in edit mode"
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        onClick = onPickPdf,
        enabled = state.isLoading.not() && state.isInEditMode.not(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(color.iconTintArgb())),
    ) {
        YabaIcon(
            modifier = Modifier.padding(end = 8.dp),
            name = "add-circle",
            color = Color.White,
        )
        Text(
            text = if (state.pdfBytes == null) "Pick PDF" else "Pick Another PDF",
            color = Color.White,
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DocmarkInfoContent(
    state: DocmarkCreationUIState,
    onChangeLabel: (String) -> Unit,
    onChangeDescription: (String) -> Unit,
) {
    val color = state.selectedFolder?.color ?: YabaColor.RED
    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(label = "Info", iconName = "information-circle")
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = state.isLoading.not(),
        value = state.label,
        onValueChange = onChangeLabel,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_url_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "text",
                color = color,
            )
        },
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        modifier = Modifier
            .heightIn(min = 120.dp, max = 240.dp)
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(color.iconTintArgb()),
            unfocusedBorderColor = Color(color.iconTintArgb()).copy(alpha = 0.5F),
        ),
        enabled = state.isLoading.not(),
        value = state.description,
        onValueChange = onChangeDescription,
        shape = RoundedCornerShape(12.dp),
        placeholder = {
            Text(text = stringResource(Res.string.create_bookmark_description_placeholder))
        },
        leadingIcon = {
            YabaIcon(
                name = "paragraph",
                color = color,
            )
        }
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DocmarkFolderSelectionContent(
    state: DocmarkCreationUIState,
    onSelectFolder: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(label = stringResource(Res.string.folder), iconName = "folder-02")
    Spacer(modifier = Modifier.height(12.dp))
    state.selectedFolder?.let { folder ->
        PresentableFolderItemView(
            modifier = Modifier.padding(horizontal = 12.dp),
            model = folder,
            nullModelPresentableColor = YabaColor.RED,
            onPressed = onSelectFolder,
            cornerSize = 12.dp,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        modifier = Modifier.padding(horizontal = 12.dp),
        onClick = onSelectFolder,
    ) {
        Text("Select Folder")
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun DocmarkTagSelectionContent(
    state: DocmarkCreationUIState,
    onSelectTags: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    LinkmarkLabel(label = stringResource(Res.string.tags_title), iconName = "tag-02")
    Spacer(modifier = Modifier.height(12.dp))

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
                modifier = Modifier
                    .padding(12.dp)
                    .padding(vertical = 24.dp),
                iconName = "tag-03",
                labelRes = Res.string.create_bookmark_no_tags_selected_title,
                message = {
                    Text(
                        text = stringResource(Res.string.create_bookmark_no_tags_selected_description),
                    )
                },
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.selectedTags.forEach { tag ->
                PresentableTagItemView(
                    model = tag,
                    nullModelPresentableColor = YabaColor.RED,
                    onPressed = onSelectTags,
                    onNavigateToEdit = onSelectTags,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    TextButton(
        modifier = Modifier.padding(horizontal = 12.dp),
        onClick = onSelectTags,
    ) {
        Text(
            text = stringResource(
                if (state.selectedTags.isEmpty()) {
                    Res.string.create_bookmark_add_tags
                } else {
                    Res.string.create_bookmark_edit_tags
                },
            ),
        )
    }
    Spacer(modifier = Modifier.height(24.dp))
}

private fun decodeDataUrlToBytes(dataUrl: String): ByteArray? {
    val marker = ";base64,"
    val markerIndex = dataUrl.indexOf(marker)
    if (markerIndex < 0) return null
    val base64 = dataUrl.substring(markerIndex + marker.length)
    return runCatching { Base64.decode(base64) }.getOrNull()
}

private fun buildHiddenMarkdown(sections: List<PdfTextSection>): String? {
    if (sections.isEmpty()) return null
    return sections.joinToString(separator = "\n\n") { section ->
        val title = section.sectionKey.ifBlank { "page" }
        "## $title\n\n${section.text}"
    }.ifBlank { null }
}
