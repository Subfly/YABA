package dev.subfly.yaba.ui.detail.bookmark.link.components

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
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.ui.detail.bookmark.components.ReaderPreferenceToolbarFontSizeItem
import dev.subfly.yaba.ui.detail.bookmark.components.ReaderPreferenceToolbarLineHeightItem
import dev.subfly.yaba.ui.detail.bookmark.components.ReaderPreferenceToolbarThemeItem
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderFloatingToolbarColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yaba.util.LocalPaneInfo
import dev.subfly.yaba.core.model.utils.ReaderPreferences
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.linkmark.LinkmarkDetailEvent

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.LinkmarkReaderFloatingToolbar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    isVisible: Boolean,
    readerPreferences: ReaderPreferences,
    hasSelection: Boolean = false,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onAnnotationClick: () -> Unit = {},
) {
    val paneInfo = LocalPaneInfo.current
    val isTwoPaneLayout = paneInfo.isTwoPaneLayout
    val toolbarColors = bookmarkReaderFloatingToolbarColors(color)

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
                colors = toolbarColors,
            ) {
                ReaderPreferenceToolbarThemeItem(
                    folderYabaColor = color,
                    selectedTheme = readerPreferences.theme,
                    onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
                )
                ReaderPreferenceToolbarFontSizeItem(
                    folderYabaColor = color,
                    selectedFontSize = readerPreferences.fontSize,
                    onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
                )
                ReaderPreferenceToolbarLineHeightItem(
                    folderYabaColor = color,
                    selectedLineHeight = readerPreferences.lineHeight,
                    onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
                )
                AnimatedContent(
                    targetState = hasSelection
                ) { has ->
                    if (has) {
                        IconButton(
                            onClick = onAnnotationClick,
                            colors = bookmarkReaderToolbarIconButtonColors(color),
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            YabaIcon(name = "sticky-note-03", color = Color.White)
                        }
                    }
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
            colors = toolbarColors,
        ) {
            ReaderPreferenceToolbarThemeItem(
                folderYabaColor = color,
                selectedTheme = readerPreferences.theme,
                onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
            )
            ReaderPreferenceToolbarFontSizeItem(
                folderYabaColor = color,
                selectedFontSize = readerPreferences.fontSize,
                onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
            )
            ReaderPreferenceToolbarLineHeightItem(
                folderYabaColor = color,
                selectedLineHeight = readerPreferences.lineHeight,
                onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
            )
            AnimatedContent(
                targetState = hasSelection
            ) { has ->
                if (has) {
                    IconButton(
                        onClick = onAnnotationClick,
                        colors = bookmarkReaderToolbarIconButtonColors(color),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "sticky-note-03", color = Color.White)
                    }
                }
            }
        }
    }
}

private val toolbarOffset = 16.dp
