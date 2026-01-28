package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yabacore.model.ui.FolderUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.folder

@Composable
internal fun LinkmarkDetailFolderSectionContent(
    modifier: Modifier = Modifier,
    folder: FolderUiModel,
    mainColor: YabaColor,
    onClickFolder: (FolderUiModel) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LinkmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 12.dp),
            iconName = "folder-01",
            label = stringResource(Res.string.folder)
        )
        FolderItemView(
            model = folder,
            allowsDeletion = false,
            onClick = onClickFolder,
            containerColor = MaterialTheme.colorScheme.surface,
            onDeleteFolder = {}
        )
    }
}