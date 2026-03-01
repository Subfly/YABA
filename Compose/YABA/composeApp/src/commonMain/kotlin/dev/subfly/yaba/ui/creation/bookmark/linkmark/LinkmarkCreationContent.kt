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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.core.components.webview.ConverterInput
import dev.subfly.yaba.core.components.webview.YabaWebViewConverter
import dev.subfly.yaba.core.navigation.creation.ResultStoreKeys
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkFolderSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkInfoContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkLinkContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTagSelectionContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.ui.TagUiModel
import dev.subfly.yabacore.state.creation.linkmark.LinkmarkCreationEvent
import dev.subfly.yabacore.state.creation.linkmark.LinkmarkCreationToastMessages
import dev.subfly.yabacore.unfurl.ConverterAssetInput
import dev.subfly.yabacore.ui.webview.WebComponentUris
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.generic_unfurl_error_text
import yaba.composeapp.generated.resources.generic_unfurl_success_text
import yaba.composeapp.generated.resources.ok
import yaba.composeapp.generated.resources.unfurl_error_text
import yaba.composeapp.generated.resources.url_error_text

@Composable
fun LinkmarkCreationContent(bookmarkId: String?, initialUrl: String? = null) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { LinkmarkCreationVM() }
    val state by vm.state.collectAsStateWithLifecycle()

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

    val converterInput = state.converterHtml?.let { html ->
        ConverterInput(html = html, baseUrl = state.converterBaseUrl)
    }

    YabaWebViewConverter(
        modifier = Modifier.size(0.dp),
        baseUrl = WebComponentUris.getConverterUri(),
        input = converterInput,
        onConverterResult = { result ->
            vm.onEvent(
                LinkmarkCreationEvent.OnConverterSucceeded(
                    markdown = result.markdown,
                    assets = result.assets.map { a ->
                        ConverterAssetInput(
                            placeholder = a.placeholder,
                            url = a.url,
                            alt = a.alt,
                        )
                    },
                    title = state.label.takeIf { it.isNotBlank() },
                    author = null,
                ),
            )
        },
        onConverterError = { error ->
            vm.onEvent(LinkmarkCreationEvent.OnConverterFailed(error = error))
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
                LinkmarkPreviewContent(
                    state = state,
                    onChangePreviewType = { vm.onEvent(LinkmarkCreationEvent.OnCyclePreviewAppearance) },
                    onUpdateAccepted = { vm.onEvent(LinkmarkCreationEvent.OnApplyContentUpdates )},
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
                LinkmarkInfoContent(
                    state = state,
                    onChangeLabel = { newLabel ->
                        vm.onEvent(LinkmarkCreationEvent.OnChangeLabel(newLabel = newLabel))
                    },
                    onClearLabel = {
                        vm.onEvent(LinkmarkCreationEvent.OnClearLabel)
                    },
                    onChangeDescription = { newDescription ->
                        vm.onEvent(
                            LinkmarkCreationEvent.OnChangeDescription(newDescription = newDescription)
                        )
                    },
                    onChangeType = { newType ->
                        vm.onEvent(LinkmarkCreationEvent.OnChangeLinkType(linkType = newType))
                    }
                )
            }
            item { LinkmarkFolderSelectionContent(state = state) }
            item { LinkmarkTagSelectionContent(state = state) }
            item { Spacer(modifier = Modifier.height(36.dp)) }
        }
    }
}
