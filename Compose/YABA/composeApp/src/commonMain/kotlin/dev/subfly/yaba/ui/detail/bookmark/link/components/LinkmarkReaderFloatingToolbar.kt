package dev.subfly.yaba.ui.detail.bookmark.link.components

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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.IconButton
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
import dev.subfly.yaba.util.LocalPaneInfo
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BoxScope.LinkmarkReaderFloatingToolbar(
    isVisible: Boolean,
    fabColor: YabaColor,
    readerPreferences: ReaderPreferences,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onFabClick: () -> Unit,
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
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onFabClick,
                        containerColor = Color(fabColor.iconTintArgb()),
                    ) {
                        YabaIcon(name = "add-01", color = Color.White)
                    }
                },
                colors = vibrantColors,
            ) {
                ThemeOptionsButton(
                    selectedTheme = readerPreferences.theme,
                    onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
                )
                FontSizeOptionsButton(
                    selectedFontSize = readerPreferences.fontSize,
                    onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
                )
                LineHeightOptionsButton(
                    selectedLineHeight = readerPreferences.lineHeight,
                    onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
                )
            }
        }
        return
    }

    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .offset(y = -toolbarOffset)
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
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = Color(fabColor.iconTintArgb()),
                ) {
                    YabaIcon(name = "add-01", color = Color.White)
                }
            },
            colors = vibrantColors,
        ) {
            ThemeOptionsButton(
                selectedTheme = readerPreferences.theme,
                onSelectTheme = { onEvent(LinkmarkDetailEvent.OnSetReaderTheme(it)) },
            )
            FontSizeOptionsButton(
                selectedFontSize = readerPreferences.fontSize,
                onSelectFontSize = { onEvent(LinkmarkDetailEvent.OnSetReaderFontSize(it)) },
            )
            LineHeightOptionsButton(
                selectedLineHeight = readerPreferences.lineHeight,
                onSelectLineHeight = { onEvent(LinkmarkDetailEvent.OnSetReaderLineHeight(it)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeOptionsButton(
    selectedTheme: ReaderTheme,
    onSelectTheme: (ReaderTheme) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = !isExpanded }) {
            YabaIcon(name = "colors")
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
    selectedFontSize: ReaderFontSize,
    onSelectFontSize: (ReaderFontSize) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = !isExpanded }) {
            YabaIcon(name = "text-square")
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
    selectedLineHeight: ReaderLineHeight,
    onSelectLineHeight: (ReaderLineHeight) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isExpanded = !isExpanded }) {
            YabaIcon(name = "cursor-text")
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
