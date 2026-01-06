package dev.subfly.yaba.ui.creation.bookmark.linkmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkLabel
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkPreviewContent
import dev.subfly.yaba.ui.creation.bookmark.linkmark.components.LinkmarkTopBar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.ContentAppearance
import dev.subfly.yabacore.model.utils.uiIconName
import dev.subfly.yabacore.model.utils.uiTitle
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.create_bookmark_add_tags
import yaba.composeapp.generated.resources.create_bookmark_edit_tags
import yaba.composeapp.generated.resources.folder
import yaba.composeapp.generated.resources.info
import yaba.composeapp.generated.resources.link
import yaba.composeapp.generated.resources.preview
import yaba.composeapp.generated.resources.tags_title

@Composable
fun LinkmarkCreationContent(bookmarkId: String?) {
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current

    val vm = viewModel { LinkmarkCreationVM() }
    val state by vm.state

    LaunchedEffect(bookmarkId) {
        vm.onEvent(LinkmarkCreationEvent.OnInit(linkmarkIdString = bookmarkId))
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
        LinkmarkPreviewContent(
            state = state,
            onChangePreviewType = {
                vm.onEvent(LinkmarkCreationEvent.OnCyclePreviewAppearance)
            },
            onOpenImageSelector = {},
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinkmarkLabel(
            label = stringResource(Res.string.link),
            iconName = "link-04"
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinkmarkLabel(
            label = stringResource(Res.string.info),
            iconName = "information-circle"
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinkmarkLabel(
            label = stringResource(Res.string.folder),
            iconName = "folder-01"
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinkmarkLabel(
            label = stringResource(Res.string.tags_title),
            iconName = "tag-01",
            extraContent = {
                TextButton(
                    onClick = {
                        // TODO: NAVIGATE TO TAG SELECTION
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        YabaIcon(
                            name = if (state.selectedTags.isEmpty()) {
                                "plus-sign"
                            } else {
                                "edit-02"
                            },
                            color = ButtonDefaults.textButtonColors().contentColor,
                        )
                        Text(
                            text = stringResource(
                                resource = if (state.selectedTags.isEmpty()) {
                                    Res.string.create_bookmark_add_tags
                                } else {
                                    Res.string.create_bookmark_edit_tags
                                }
                            )
                        )
                    }
                }
            }
        )
        Spacer(modifier = Modifier.height(36.dp))
    }
}
