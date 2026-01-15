package dev.subfly.yaba.ui.creation.bookmark.linkmark.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.folder.PresentableFolderItemView
import dev.subfly.yaba.core.navigation.creation.FolderSelectionRoute
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.FolderSelectionMode
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.linkmark.LinkmarkCreationUIState
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.folder
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun LinkmarkFolderSelectionContent(state: LinkmarkCreationUIState) {
    val creationNavigator = LocalCreationContentNavigator.current

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
        onPressed = {
            creationNavigator.add(
                FolderSelectionRoute(
                    mode = FolderSelectionMode.FOLDER_SELECTION,
                    contextFolderId = null,
                    contextBookmarkIds = null,
                )
            )
        },
    )
}