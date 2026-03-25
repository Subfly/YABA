package dev.subfly.yaba.ui.detail.bookmark.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderPreferenceToolbarThemeItem(
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
                            Text(text = theme.readerPreferenceLabel())
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderPreferenceToolbarFontSizeItem(
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
                            Text(text = fontSize.readerPreferenceLabel())
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderPreferenceToolbarLineHeightItem(
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
                            Text(text = lineHeight.readerPreferenceLabel())
                        },
                    )
                }
            }
        }
    }
}

internal fun ReaderTheme.readerPreferenceLabel(): String =
    when (this) {
        ReaderTheme.SYSTEM -> "System" // TODO: Localize
        ReaderTheme.DARK -> "Dark" // TODO: Localize
        ReaderTheme.LIGHT -> "Light" // TODO: Localize
        ReaderTheme.SEPIA -> "Sepia" // TODO: Localize
    }

internal fun ReaderFontSize.readerPreferenceLabel(): String =
    when (this) {
        ReaderFontSize.SMALL -> "Small" // TODO: Localize
        ReaderFontSize.MEDIUM -> "Medium" // TODO: Localize
        ReaderFontSize.LARGE -> "Large" // TODO: Localize
    }

internal fun ReaderLineHeight.readerPreferenceLabel(): String =
    when (this) {
        ReaderLineHeight.NORMAL -> "Normal" // TODO: Localize
        ReaderLineHeight.RELAXED -> "Relaxed" // TODO: Localize
    }
