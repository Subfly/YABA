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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.model.utils.YabaColor

/** Gradient + blur behind [BookmarkDetailContentTopBar]. */
internal enum class BookmarkDetailTopBarScrim {
    /** Strong gradient and blur (default for most bookmark details). */
    Full,
    /** Lighter gradient and milder blur — status bar legibility on bright canvas without heavy chrome. */
    Subtle,
    /** No scrim. */
    None,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BookmarkDetailContentTopBar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    onBack: () -> Unit,
    onShowDetail: () -> Unit,
    overflowMenu: @Composable () -> Unit,
    loadingIndicator: @Composable () -> Unit,
    title: (@Composable () -> Unit)? = null,
    scrim: BookmarkDetailTopBarScrim = BookmarkDetailTopBarScrim.Full,
) {
    val iconButtonColors = bookmarkDetailIconButtonColors(color)
    val scheme = MaterialTheme.colorScheme
    val scrimOnTopBar =
        when (scrim) {
            BookmarkDetailTopBarScrim.Full ->
                Triple(
                    scheme.surface.copy(alpha = 0.82f),
                    scheme.surface.copy(alpha = 0.38f),
                    188.dp,
                )
            BookmarkDetailTopBarScrim.Subtle ->
                Triple(
                    scheme.surface.copy(alpha = 0.26f),
                    scheme.surface.copy(alpha = 0.10f),
                    120.dp,
                )
            BookmarkDetailTopBarScrim.None -> null
        }
    val blurRadius =
        when (scrim) {
            BookmarkDetailTopBarScrim.Full -> 16.dp
            BookmarkDetailTopBarScrim.Subtle -> 6.dp
            BookmarkDetailTopBarScrim.None -> 0.dp
        }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (scrimOnTopBar != null) {
            val (scrimStrong, scrimSoft, scrimHeight) = scrimOnTopBar
            Box(
                modifier =
                    modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(scrimHeight)
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
                        .blur(radius = blurRadius),
            )
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(if (scrim == BookmarkDetailTopBarScrim.None) modifier else Modifier),
        ) {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                title = { title?.invoke() },
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
