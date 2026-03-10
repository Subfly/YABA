@file:OptIn(ExperimentalEncodingApi::class)

package dev.subfly.yaba.ui.creation.bookmark.docmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import dev.subfly.yaba.core.components.webview.PdfConverterInput
import dev.subfly.yaba.core.components.webview.PdfTextSection
import dev.subfly.yaba.core.components.webview.YabaWebViewPdfConverter
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkFolderSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkInfoContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewAppearanceSwitcher
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewCard
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkTagSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.ui.creation.bookmark.model.BookmarkPreviewData
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.uiTitle
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.state.creation.docmark.DocmarkCreationEvent
import dev.subfly.yabacore.state.creation.docmark.DocmarkCreationUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.ui.webview.WebComponentUris
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_title_placeholder
import yaba.composeapp.generated.resources.preview
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
                DocmarkPreviewContent(
                    state = state,
                    onChangePreviewType = { vm.onEvent(DocmarkCreationEvent.OnCyclePreviewAppearance) },
                    onPickPdf = { vm.onEvent(DocmarkCreationEvent.OnPickPdf) },
                )
            }
            item {
                BookmarkInfoContent(
                    label = state.label,
                    description = state.description,
                    onChangeLabel = { vm.onEvent(DocmarkCreationEvent.OnChangeLabel(it)) },
                    onChangeDescription = { vm.onEvent(DocmarkCreationEvent.OnChangeDescription(it)) },
                    selectedFolder = state.selectedFolder,
                    enabled = state.isLoading.not(),
                    labelPlaceholder = Res.string.create_bookmark_title_placeholder,
                    nullModelPresentableColor = YabaColor.BLUE,
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
                                selectedTagIds = state.selectedTags.map { tag -> tag.id },
                            ),
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
private fun DocmarkPreviewContent(
    state: DocmarkCreationUIState,
    onChangePreviewType: () -> Unit,
    onPickPdf: () -> Unit,
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
                    imageData = state.previewImageBytes,
                    label = state.label,
                    description = state.description,
                    selectedFolder = state.selectedFolder,
                    selectedTags = state.selectedTags,
                    isLoading = state.isLoading,
                    emptyImageIconName = "doc-02",
                ),
                bookmarkAppearance = state.bookmarkAppearance,
                cardImageSizing = state.cardImageSizing,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
        },
    )
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
