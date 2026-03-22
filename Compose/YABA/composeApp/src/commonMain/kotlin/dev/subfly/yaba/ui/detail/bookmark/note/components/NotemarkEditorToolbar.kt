package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderFloatingToolbarColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarToggleIconButtonColors
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.YabaEditorCommands

private data class FormatMenuRow(
    val icon: String,
    val label: String,
    val command: String,
    val isSelected: (EditorFormattingState) -> Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun NotemarkEditorToolbar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    canCreateHighlight: Boolean,
    formatting: EditorFormattingState,
    onHighlightClick: () -> Unit,
    onDispatchCommand: (String) -> Unit,
) {
    val toolbarColors = bookmarkReaderFloatingToolbarColors(color)

    HorizontalFloatingToolbar(
        modifier = modifier,
        expanded = true,
        colors = toolbarColors,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            AnimatedContent(
                targetState = canCreateHighlight,
                label = "notemarkHighlight",
            ) { can ->
                if (can) {
                    IconButton(
                        onClick = onHighlightClick,
                        colors = bookmarkReaderToolbarIconButtonColors(color),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        YabaIcon(name = "highlighter", color = Color.White)
                    }
                }
            }

            HeadingInsertDropdown(
                folderYabaColor = color,
                onInsertHeadingMarkdown = { level ->
                    onDispatchCommand(
                        YabaEditorCommands.insertTextPayload(
                            YabaEditorCommands.markdownHeadingPrefix(level),
                        ),
                    )
                },
            )

            TextMarksDropdown(
                folderYabaColor = color,
                formatting = formatting,
                onDispatchCommand = onDispatchCommand,
            )

            InsertBlocksDropdown(
                folderYabaColor = color,
                formatting = formatting,
                onDispatchCommand = onDispatchCommand,
            )

            IndentOutdentDropdown(
                folderYabaColor = color,
                formatting = formatting,
                onDispatchCommand = onDispatchCommand,
            )

            UndoRedoDropdown(
                folderYabaColor = color,
                formatting = formatting,
                onDispatchCommand = onDispatchCommand,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HeadingInsertDropdown(
    folderYabaColor: YabaColor,
    onInsertHeadingMarkdown: (level: Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "heading", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                (1..6).forEachIndexed { index, level ->
                    val iconName = "heading-${level.toString().padStart(2, '0')}"
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, 6),
                        checked = false,
                        onCheckedChange = { _ ->
                            expanded = false
                            onInsertHeadingMarkdown(level)
                        },
                        leadingIcon = { YabaIcon(name = iconName) },
                        text = { Text(text = "Heading $level") },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TextMarksDropdown(
    folderYabaColor: YabaColor,
    formatting: EditorFormattingState,
    onDispatchCommand: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val anyActive by remember(formatting) {
        derivedStateOf { YabaEditorCommands.hasAnyTextMark(formatting) }
    }

    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarToggleIconButtonColors(folderYabaColor, anyActive),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "text-font", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                listOf(
                    FormatMenuRow("text-bold", "Bold", YabaEditorCommands.ToggleBold) { it.bold },
                    FormatMenuRow("text-italic", "Italic", YabaEditorCommands.ToggleItalic) { it.italic },
                    FormatMenuRow("text-underline", "Underline", YabaEditorCommands.ToggleUnderline) { it.underline },
                    FormatMenuRow("text-strikethrough", "Strikethrough", YabaEditorCommands.ToggleStrikethrough) { it.strikethrough },
                    FormatMenuRow("text-subscript", "Subscript", YabaEditorCommands.ToggleSubscript) { it.subscript },
                    FormatMenuRow("text-superscript", "Superscript", YabaEditorCommands.ToggleSuperscript) { it.superscript },
                ).fastForEachIndexed { index, row ->
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(index, 6),
                        checked = row.isSelected(formatting),
                        onCheckedChange = { _ ->
                            onDispatchCommand(row.command)
                        },
                        leadingIcon = { YabaIcon(name = row.icon) },
                        text = { Text(text = row.label) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun IndentOutdentDropdown(
    folderYabaColor: YabaColor,
    formatting: EditorFormattingState,
    onDispatchCommand: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "text-indent", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    enabled = formatting.canIndent,
                    onCheckedChange = { _ ->
                        onDispatchCommand(YabaEditorCommands.Indent)
                    },
                    leadingIcon = { YabaIcon(name = "text-indent-more") },
                    text = { Text(text = "Indent") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    enabled = formatting.canOutdent,
                    onCheckedChange = { _ ->
                        onDispatchCommand(YabaEditorCommands.Outdent)
                    },
                    leadingIcon = { YabaIcon(name = "text-indent-less") },
                    text = { Text(text = "Outdent") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun UndoRedoDropdown(
    folderYabaColor: YabaColor,
    formatting: EditorFormattingState,
    onDispatchCommand: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "repeat", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 2),
                    checked = false,
                    enabled = formatting.canUndo,
                    onCheckedChange = { _ ->
                        onDispatchCommand(YabaEditorCommands.Undo)
                    },
                    leadingIcon = { YabaIcon(name = "undo-03") },
                    text = { Text(text = "Undo") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 2),
                    checked = false,
                    enabled = formatting.canRedo,
                    onCheckedChange = { _ ->
                        onDispatchCommand(YabaEditorCommands.Redo)
                    },
                    leadingIcon = { YabaIcon(name = "redo-03") },
                    text = { Text(text = "Redo") },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun InsertBlocksDropdown(
    folderYabaColor: YabaColor,
    formatting: EditorFormattingState,
    onDispatchCommand: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var listSubExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded.not()) listSubExpanded = false
    }

    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarIconButtonColors(folderYabaColor),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "add-01", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 9),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "grid-table") },
                    text = { Text(text = "Table") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 9),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "image-add-02") },
                    text = { Text(text = "Image") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(2, 9),
                    checked = formatting.code,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.ToggleCode)
                    },
                    leadingIcon = { YabaIcon(name = "source-code") },
                    text = { Text(text = "Code") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(3, 9),
                    checked = formatting.blockquote,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.ToggleQuote)
                    },
                    leadingIcon = { YabaIcon(name = "quote-down") },
                    text = { Text(text = "Quote") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(4, 9),
                    checked = false,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.InsertHr)
                    },
                    leadingIcon = { YabaIcon(name = "solid-line-01") },
                    text = { Text(text = "Horizontal rule") },
                )

                Box {
                    val listTrailingRotation by animateFloatAsState(
                        targetValue = if (listSubExpanded) 90f else 0f,
                    )
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(5, 9),
                        checked = formatting.bulletList || formatting.orderedList || formatting.taskList,
                        onCheckedChange = { _ -> listSubExpanded = true },
                        leadingIcon = { YabaIcon(name = "left-to-right-list-dash") },
                        trailingIcon = {
                            YabaIcon(
                                modifier = Modifier.rotate(listTrailingRotation),
                                name = "arrow-right-01",
                            )
                        },
                        text = { Text(text = "List") },
                    )
                    DropdownMenuPopup(
                        expanded = listSubExpanded,
                        onDismissRequest = { listSubExpanded = false },
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 1),
                        ) {
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(0, 3),
                                checked = formatting.bulletList,
                                onCheckedChange = { _ ->
                                    onDispatchCommand(YabaEditorCommands.ToggleBulletedList)
                                    listSubExpanded = false
                                    expanded = false
                                },
                                leadingIcon = { YabaIcon(name = "left-to-right-list-bullet") },
                                text = { Text(text = "Bulleted list") },
                            )
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(1, 3),
                                checked = formatting.orderedList,
                                onCheckedChange = { _ ->
                                    onDispatchCommand(YabaEditorCommands.ToggleNumberedList)
                                    listSubExpanded = false
                                    expanded = false
                                },
                                leadingIcon = { YabaIcon(name = "left-to-right-list-number") },
                                text = { Text(text = "Numbered list") },
                            )
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(2, 3),
                                checked = formatting.taskList,
                                onCheckedChange = { _ ->
                                    onDispatchCommand(YabaEditorCommands.ToggleTaskList)
                                    listSubExpanded = false
                                    expanded = false
                                },
                                leadingIcon = { YabaIcon(name = "check-list") },
                                text = { Text(text = "Task list") },
                            )
                        }
                    }
                }

                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(6, 9),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "link-04") },
                    text = { Text(text = "Link") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(7, 9),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "youtube") },
                    text = { Text(text = "YouTube") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(8, 9),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "calculator") },
                    text = { Text(text = "Math") },
                )
            }
        }
    }
}
