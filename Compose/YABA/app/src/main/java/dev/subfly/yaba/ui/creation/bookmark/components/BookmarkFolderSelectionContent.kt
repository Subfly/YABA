package dev.subfly.yaba.ui.creation.bookmark.components

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.item.folder.PresentableFolderItemView
import dev.subfly.yaba.core.model.ui.FolderUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

@Composable
fun BookmarkFolderSelectionContent(
    selectedFolder: FolderUiModel?,
    onSelectFolder: () -> Unit,
    nullModelPresentableColor: YabaColor = YabaColor.BLUE,
) {
    Spacer(modifier = Modifier.height(24.dp))
    BookmarkCreationLabel(
        label = stringResource(R.string.folder),
        iconName = "folder-01"
    )
    Spacer(modifier = Modifier.height(12.dp))
    PresentableFolderItemView(
        modifier = Modifier.padding(horizontal = 12.dp),
        model = selectedFolder,
        nullModelPresentableColor = nullModelPresentableColor,
        cornerSize = 12.dp,
        onPressed = onSelectFolder,
    )
}
