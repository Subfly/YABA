package dev.subfly.yaba.ui.detail.bookmark.doc.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.subfly.yaba.util.LocalPaneInfo
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.DocmarkReaderFloatingToolbar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    hasSelection: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onHighlightClick: () -> Unit,
) {
    val paneInfo = LocalPaneInfo.current
    val isTwoPaneLayout = paneInfo.isTwoPaneLayout
    val vibrantColors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()

    if (isTwoPaneLayout) {
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = -toolbarOffset)
                .zIndex(1f),
            visible = isVisible,
            enter = fadeIn() + slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 2 },
            ),
            exit = fadeOut() + slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 2 },
            ),
        ) {
            VerticalFloatingToolbar(
                expanded = true,
                colors = vibrantColors,
            ) {
                IconButton(
                    onClick = onPrevPage,
                    enabled = canGoPrev,
                ) {
                    YabaIcon(name = "previous")
                }
                AnimatedContent(targetState = hasSelection) { has ->
                    if (has) {
                        IconButton(onClick = onHighlightClick) {
                            YabaIcon(name = "highlighter")
                        }
                    }
                }
                IconButton(
                    onClick = onNextPage,
                    enabled = canGoNext,
                ) {
                    YabaIcon(name = "next")
                }
            }
        }
        return
    }

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = -toolbarOffset)
            .then(modifier)
            .zIndex(1f),
        visible = isVisible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 },
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight / 2 },
        ),
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            colors = vibrantColors,
        ) {
            IconButton(
                onClick = onPrevPage,
                enabled = canGoPrev,
            ) {
                YabaIcon(name = "previous")
            }
            AnimatedContent(targetState = hasSelection) { has ->
                if (has) {
                    IconButton(onClick = onHighlightClick) {
                        YabaIcon(name = "highlighter")
                    }
                }
            }
            IconButton(
                onClick = onNextPage,
                enabled = canGoNext,
            ) {
                YabaIcon(name = "next")
            }
        }
    }
}

private val toolbarOffset = 16.dp
