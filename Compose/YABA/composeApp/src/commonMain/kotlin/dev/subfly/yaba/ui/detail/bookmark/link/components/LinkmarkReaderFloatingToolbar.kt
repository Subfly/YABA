package dev.subfly.yaba.ui.detail.bookmark.link.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderFloatingToolbarColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yaba.util.LocalPaneInfo
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.LinkmarkReaderFloatingToolbar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    isVisible: Boolean,
    readerPreferences: ReaderPreferences,
    hasSelection: Boolean = false,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onHighlightClick: () -> Unit = {},
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
                ThemeOptionsButton(
                    folderYabaColor = color,
                    selectedTheme = readerPreferences.theme,
                    onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
                )
                FontSizeOptionsButton(
                    folderYabaColor = color,
                    selectedFontSize = readerPreferences.fontSize,
                    onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
                )
                LineHeightOptionsButton(
                    folderYabaColor = color,
                    selectedLineHeight = readerPreferences.lineHeight,
                    onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
                )
                AnimatedContent(
                    targetState = hasSelection
                ) { has ->
                    if (has) {
                        IconButton(
                            onClick = onHighlightClick,
                            colors = bookmarkReaderToolbarIconButtonColors(color),
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            YabaIcon(name = "highlighter", color = Color.White)
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
            ThemeOptionsButton(
                folderYabaColor = color,
                selectedTheme = readerPreferences.theme,
                onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
            )
            FontSizeOptionsButton(
                folderYabaColor = color,
                selectedFontSize = readerPreferences.fontSize,
                onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
            )
            LineHeightOptionsButton(
                folderYabaColor = color,
                selectedLineHeight = readerPreferences.lineHeight,
                onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
            )
            AnimatedContent(
                targetState = hasSelection
            ) { has ->
                if (has) {
                    IconButton(
                        onClick = onHighlightClick,
                        colors = bookmarkReaderToolbarIconButtonColors(color),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "highlighter", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeOptionsButton(
    folderYabaColor: YabaColor,
    selectedTheme: ReaderTheme,
    onSelectTheme: (ReaderTheme) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) {
            YabaIcon(name = "colors", color = Color.White)
        }
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
            ) {
                ReaderTheme.entries.fastForEachIndexed { index, theme ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, ReaderTheme.entries.size),
                        checked = selectedTheme == theme,
                        onCheckedChange = { _ ->
                            isExpanded = false
                            onSelectTheme(theme)
                        },
                        text = {
                            Text(text = theme.uiText())
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FontSizeOptionsButton(
    folderYabaColor: YabaColor,
    selectedFontSize: ReaderFontSize,
    onSelectFontSize: (ReaderFontSize) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) {
            YabaIcon(name = "text-square", color = Color.White)
        }
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
            ) {
                ReaderFontSize.entries.fastForEachIndexed { index, fontSize ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, ReaderFontSize.entries.size),
                        checked = selectedFontSize == fontSize,
                        onCheckedChange = { _ ->
                            isExpanded = false
                            onSelectFontSize(fontSize)
                        },
                        text = {
                            Text(text = fontSize.uiText())
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LineHeightOptionsButton(
    folderYabaColor: YabaColor,
    selectedLineHeight: ReaderLineHeight,
    onSelectLineHeight: (ReaderLineHeight) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { isExpanded = !isExpanded },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) {
            YabaIcon(name = "cursor-text", color = Color.White)
        }
        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
        ) {
            DropdownMenuGroup(
                shapes = MenuDefaults.groupShape(index = 0, count = 1),
            ) {
                ReaderLineHeight.entries.fastForEachIndexed { index, lineHeight ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, ReaderLineHeight.entries.size),
                        checked = selectedLineHeight == lineHeight,
                        onCheckedChange = { _ ->
                            isExpanded = false
                            onSelectLineHeight(lineHeight)
                        },
                        text = {
                            Text(text = lineHeight.uiText())
                        },
                    )
                }
            }
        }
    }
}

private fun ReaderTheme.uiText(): String =
    when (this) {
        ReaderTheme.SYSTEM -> "System" // TODO: Localize
        ReaderTheme.DARK -> "Dark" // TODO: Localize
        ReaderTheme.LIGHT -> "Light" // TODO: Localize
        ReaderTheme.SEPIA -> "Sepia" // TODO: Localize
    }

private fun ReaderFontSize.uiText(): String =
    when (this) {
        ReaderFontSize.SMALL -> "Small" // TODO: Localize
        ReaderFontSize.MEDIUM -> "Medium" // TODO: Localize
        ReaderFontSize.LARGE -> "Large" // TODO: Localize
    }

private fun ReaderLineHeight.uiText(): String =
    when (this) {
        ReaderLineHeight.NORMAL -> "Normal" // TODO: Localize
        ReaderLineHeight.RELAXED -> "Relaxed" // TODO: Localize
    }

private val toolbarOffset = 16.dp
