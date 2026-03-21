package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun bookmarkDetailIconButtonColors(folderYabaColor: YabaColor): IconButtonColors {
    val folderColor = Color(folderYabaColor.iconTintArgb())
    val folderTintBackground = folderColor.copy(alpha = 0.25f)
    return IconButtonDefaults.iconButtonColors(
        containerColor = folderTintBackground,
        contentColor = folderColor,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BookmarkDetailContentTopBar(
    folderYabaColor: YabaColor,
    onBack: () -> Unit,
    onShowDetail: () -> Unit,
    overflowMenu: @Composable () -> Unit,
    loadingIndicator: @Composable () -> Unit,
) {
    val iconButtonColors = bookmarkDetailIconButtonColors(folderYabaColor)

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    colors = iconButtonColors,
                    shapes = IconButtonDefaults.shapes(),
                ) { YabaIcon(name = "arrow-left-01", color = folderYabaColor) }
            },
            actions = {
                IconButton(
                    onClick = onShowDetail,
                    colors = iconButtonColors,
                    shapes = IconButtonDefaults.shapes(),
                ) { YabaIcon(name = "information-circle", color = folderYabaColor) }
                overflowMenu()
            },
        )
        loadingIndicator()
    }
}

internal fun bookmarkFolderAccentColor(bookmark: BookmarkPreviewUiModel?): YabaColor {
    val c = bookmark?.parentFolder?.color
    return if (c == null || c == YabaColor.NONE) YabaColor.BLUE else c
}
