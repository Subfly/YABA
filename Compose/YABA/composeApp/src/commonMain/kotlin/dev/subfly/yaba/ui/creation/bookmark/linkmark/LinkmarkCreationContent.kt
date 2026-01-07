package dev.subfly.yaba.ui.creation.bookmark.linkmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.folder.FolderCreationEvent
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationEvent

@Composable
fun LinkmarkCreationContent(bookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val vm = viewModel { LinkmarkCreationVM() }
    val state by vm.state

    LaunchedEffect(bookmarkId) {
        vm.onEvent(LinkmarkCreationEvent.OnInit(linkmarkIdString = bookmarkId))
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_FOLDER)) {
        resultStore.getResult<FolderUiModel>(ResultStoreKeys.SELECTED_FOLDER)?.let { newFolder ->
            vm.onEvent(LinkmarkCreationEvent.OnSelectFolder(folder = newFolder))
            resultStore.removeResult(ResultStoreKeys.SELECTED_FOLDER)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        LinkmarkTopBar(
            modifier = Modifier.padding(horizontal = 8.dp),
            canPerformDone = state.label.isNotBlank(),
            isEditing = state.editingLinkmark != null,
            onDone = { vm.onEvent(LinkmarkCreationEvent.OnSave) },
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
                    onChangePreviewType = {
                        vm.onEvent(LinkmarkCreationEvent.OnCyclePreviewAppearance)
                    },
                    onOpenImageSelector = {},
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
            item {
                LinkmarkFolderSelectionContent(state = state)
            }
            item {
                LinkmarkTagSelectionContent(state = state)
            }
            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
