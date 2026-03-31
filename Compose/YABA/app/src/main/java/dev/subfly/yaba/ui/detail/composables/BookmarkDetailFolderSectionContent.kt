package dev.subfly.yaba.ui.detail.composables

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.folder.FolderItemView
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

@Composable
internal fun BookmarkDetailFolderSectionContent(
    modifier: Modifier = Modifier,
    folder: FolderUiModel,
    mainColor: YabaColor,
    onClickFolder: (FolderUiModel) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BookmarkDetailLabel(
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 12.dp),
            iconName = "folder-01",
            label = stringResource(R.string.folder)
        )
        FolderItemView(
            model = folder,
            allowsDeletion = false,
            onClick = onClickFolder,
            containerColor = MaterialTheme.colorScheme.surface,
            showBookmarkCounts = false,
            onDeleteFolder = {}
        )
    }
}
