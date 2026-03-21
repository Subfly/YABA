package dev.subfly.yaba.ui.creation.bookmark.linkmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.core.navigation.creation.ImageSelectionRoute
import dev.subfly.yaba.core.navigation.creation.TagCreationRoute
import dev.subfly.yaba.core.navigation.creation.TagSelectionRoute
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkFolderSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkInfoContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewAppearanceSwitcher
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewCard
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.components.BookmarkTagSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkLinkContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.ui.creation.bookmark.model.BookmarkPreviewData
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.state.creation.linkmark.LinkmarkCreationEvent
import dev.subfly.yabacore.state.creation.linkmark.LinkmarkCreationToastMessages
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.webview.WebConverterInput
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import yaba.composeapp.generated.resources.create_bookmark_url_placeholder
import yaba.composeapp.generated.resources.generic_unfurl_error_text
import yaba.composeapp.generated.resources.generic_unfurl_success_text
import yaba.composeapp.generated.resources.ok
import yaba.composeapp.generated.resources.preview
import yaba.composeapp.generated.resources.unfurl_error_text
import yaba.composeapp.generated.resources.url_error_text

@Composable
fun LinkmarkCreationContent(bookmarkId: String?, initialUrl: String? = null) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { LinkmarkCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

    val converterInput by remember(state.converterHtml) {
        derivedStateOf {
            state.converterHtml?.let { html ->
                WebConverterInput(html = html, baseUrl = state.converterBaseUrl)
            }
        }
    }

    LaunchedEffect(bookmarkId, initialUrl) {
        vm.onEvent(
            LinkmarkCreationEvent.OnInit(
                linkmarkIdString = bookmarkId,
                initialUrl = initialUrl,
                toastMessages = LinkmarkCreationToastMessages(
                    unfurlSuccess = Res.string.generic_unfurl_success_text,
                    invalidUrl = Res.string.url_error_text,
                    unableToUnfurl = Res.string.unfurl_error_text,
                    genericUnfurlError = Res.string.generic_unfurl_error_text,
                    acceptLabel = Res.string.ok,
                ),
            )
        )
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_FOLDER)) {
        resultStore.getResult<FolderUiModel>(ResultStoreKeys.SELECTED_FOLDER)?.let { newFolder ->
            vm.onEvent(LinkmarkCreationEvent.OnSelectFolder(folder = newFolder))
            resultStore.removeResult(ResultStoreKeys.SELECTED_FOLDER)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_TAGS)) {
        resultStore.getResult<List<TagUiModel>>(ResultStoreKeys.SELECTED_TAGS)?.let { newTags ->
            vm.onEvent(LinkmarkCreationEvent.OnSelectTags(tags = newTags))
            resultStore.removeResult(ResultStoreKeys.SELECTED_TAGS)
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_IMAGE)) {
        resultStore.getResult<String>(ResultStoreKeys.SELECTED_IMAGE)?.let { newUrl ->
            vm.onEvent(LinkmarkCreationEvent.OnSelectImage(imageUrl = newUrl))
            resultStore.removeResult(ResultStoreKeys.SELECTED_IMAGE)
        }
    }

    YabaWebView(
        modifier = Modifier.size(0.dp),
        baseUrl = WebComponentUris.getConverterUri(),
        feature = YabaWebFeature.HtmlConverter(input = converterInput),
        onHostEvent = { ev ->
            when (ev) {
                is YabaWebHostEvent.HtmlConverterSuccess ->
                    vm.onEvent(
                        LinkmarkCreationEvent.OnConverterSucceeded(
                            html = ev.result.html,
                            assets = ev.result.assets,
                        ),
                    )
                is YabaWebHostEvent.HtmlConverterFailure ->
                    vm.onEvent(LinkmarkCreationEvent.OnConverterFailed(error = ev.error))
                else -> Unit
            }
        },
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9F)
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        LinkmarkTopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            canPerformDone = state.label.isNotBlank(),
            isEditing = state.editingLinkmark != null,
            isSaving = state.isSaving,
            onDone = {
                vm.onEvent(
                    LinkmarkCreationEvent.OnSave(
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
                // Means next pop up destination is Empty Route,
                // so dismiss first, then remove the last item
                if (creationNavigator.size == 2) {
                    appStateManager.onHideCreationContent()
                }
                creationNavigator.removeLastOrNull()
            },
        )
        LazyColumn {
            item {
                BookmarkPreviewContent(
                    label = stringResource(Res.string.preview),
                    iconName = "image-03",
                    extraContent = {
                        BookmarkPreviewAppearanceSwitcher(
                            bookmarkAppearance = state.bookmarkAppearance,
                            cardImageSizing = state.cardImageSizing,
                            color = state.selectedFolder?.color ?: YabaColor.BLUE,
                            onClick = { vm.onEvent(LinkmarkCreationEvent.OnCyclePreviewAppearance) },
                        )
                    },
                    content = {
                        BookmarkPreviewCard(
                            data = BookmarkPreviewData(
                                imageData = state.imageData,
                                domainImageData = state.iconData,
                                label = state.label,
                                description = state.description,
                                selectedFolder = state.selectedFolder,
                                selectedTags = state.selectedTags,
                                isLoading = state.isLoading,
                                emptyImageIconName = "bookmark-02",
                            ),
                            bookmarkAppearance = state.bookmarkAppearance,
                            cardImageSizing = state.cardImageSizing,
                            onClick = {
                                creationNavigator.add(
                                    ImageSelectionRoute(
                                        selectedImage = state.imageUrl,
                                        imageDataMap = state.selectableImages,
                                    )
                                )
                            },
                        )
                    },
                )
            }
            item {
                LinkmarkLinkContent(
                    state = state,
                    onChangeUrl = { newUrl ->
                        vm.onEvent(LinkmarkCreationEvent.OnChangeUrl(newUrl = newUrl))
                    }
                )
            }
            item {
                BookmarkInfoContent(
                    label = state.label,
                    description = state.description,
                    onChangeLabel = { newLabel ->
                        vm.onEvent(LinkmarkCreationEvent.OnChangeLabel(newLabel = newLabel))
                    },
                    onChangeDescription = { newDescription ->
                        vm.onEvent(
                            LinkmarkCreationEvent.OnChangeDescription(newDescription = newDescription)
                        )
                    },
                    selectedFolder = state.selectedFolder,
                    enabled = state.isLoading.not(),
                    labelPlaceholder = Res.string.create_bookmark_url_placeholder,
                    showClearLabelButton = true,
                    onClearLabel = {
                        vm.onEvent(LinkmarkCreationEvent.OnClearLabel)
                    },
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
