package dev.subfly.yaba.ui.detail.bookmark.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarColors
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.iconTintArgb

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun bookmarkDetailIconButtonColors(color: YabaColor): IconButtonColors {
    val folderColor = Color(color.iconTintArgb())
    val folderTintBackground = folderColor.copy(alpha = 0.5f)
    return IconButtonDefaults.iconButtonColors(
        containerColor = folderTintBackground,
        contentColor = Color.White,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun bookmarkReaderFloatingToolbarColors(color: YabaColor): FloatingToolbarColors {
    val folderColor = Color(color.iconTintArgb())
    return FloatingToolbarDefaults.vibrantFloatingToolbarColors(
        toolbarContainerColor = folderColor.copy(alpha = 0.5f),
        toolbarContentColor = Color.White,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun bookmarkReaderToolbarIconButtonColors(
    @Suppress("UNUSED_PARAMETER") color: YabaColor,
): IconButtonColors {
    return IconButtonDefaults.iconButtonColors(
        containerColor = Color.Transparent,
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.5f),
    )
}
