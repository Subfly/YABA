package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yabacore.model.ui.BookmarkPreviewUiModel
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BookmarkDetailContentTopBar(
    color: YabaColor,
    onBack: () -> Unit,
    onShowDetail: () -> Unit,
    overflowMenu: @Composable () -> Unit,
    loadingIndicator: @Composable () -> Unit,
) {
    val iconButtonColors = bookmarkDetailIconButtonColors(color)
    val scheme = MaterialTheme.colorScheme
    val scrimStrong = scheme.surface.copy(alpha = 0.82f)
    val scrimSoft = scheme.surface.copy(alpha = 0.38f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(188.dp)
                    .graphicsLayer { clip = false }
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colorStops =
                                    arrayOf(
                                        0f to scrimStrong,
                                        0.42f to scrimSoft,
                                        0.82f to Color.Transparent,
                                        1f to Color.Transparent,
                                    ),
                            ),
                    )
                    .blur(radius = 16.dp),
        )
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
                    ) { YabaIcon(name = "arrow-left-01", color = Color.White) }
                },
                actions = {
                    IconButton(
                        onClick = onShowDetail,
                        colors = iconButtonColors,
                        shapes = IconButtonDefaults.shapes(),
                    ) { YabaIcon(name = "information-circle", color = Color.White) }
                    overflowMenu()
                },
            )
            loadingIndicator()
        }
    }
}

internal fun bookmarkFolderAccentColor(bookmark: BookmarkPreviewUiModel?): YabaColor {
    val c = bookmark?.parentFolder?.color
    return if (c == null || c == YabaColor.NONE) YabaColor.BLUE else c
}
