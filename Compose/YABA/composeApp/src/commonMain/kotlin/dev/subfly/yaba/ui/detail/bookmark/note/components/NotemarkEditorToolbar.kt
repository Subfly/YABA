package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarToggleIconButtonColors
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
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
    formatting: EditorFormattingState,
    onHighlightInactiveClick: () -> Unit,
    onHighlightActiveClick: () -> Unit,
    onDispatchCommand: (String) -> Unit,
    onOpenTableInsertSheet: () -> Unit,
    onOpenMathSheet: (isBlock: Boolean) -> Unit,
    onPickImageFromGallery: () -> Unit,
    onCaptureImageFromCamera: () -> Unit,
) {
    val toolbarContainerColor = Color(color.iconTintArgb()).copy(alpha = 0.5f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = toolbarContainerColor)
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            AnimatedContent(
                targetState = formatting.inTable,
                label = "notemarkTableEdit",
            ) { inTable ->
                if (inTable) {
                    TableEditDropdown(
                        folderYabaColor = color,
                        formatting = formatting,
                        onDispatchCommand = onDispatchCommand,
                    )
                }
            }

            HeadingInsertDropdown(
                folderYabaColor = color,
                onInsertHeadingMarkdown = { level ->
                    onDispatchCommand(YabaEditorCommands.setHeadingPayload(level))
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
                onOpenTableInsertSheet = onOpenTableInsertSheet,
                onOpenMathSheet = onOpenMathSheet,
                onPickImageFromGallery = onPickImageFromGallery,
                onCaptureImageFromCamera = onCaptureImageFromCamera,
            )

            HighlightToolbarButton(
                folderYabaColor = color,
                formatting = formatting,
                onInactiveClick = onHighlightInactiveClick,
                onActiveClick = onHighlightActiveClick,
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
private fun HighlightToolbarButton(
    folderYabaColor: YabaColor,
    formatting: EditorFormattingState,
    onInactiveClick: () -> Unit,
    onActiveClick: () -> Unit,
) {
    val active = formatting.textHighlight
    IconButton(
        onClick = {
            if (active) onActiveClick() else onInactiveClick()
        },
        colors = bookmarkReaderToolbarToggleIconButtonColors(folderYabaColor, active),
        shapes = IconButtonDefaults.shapes(),
    ) { YabaIcon(name = "highlighter", color = Color.White) }
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
                    FormatMenuRow(
                        "text-italic",
                        "Italic",
                        YabaEditorCommands.ToggleItalic
                    ) { it.italic },
                    FormatMenuRow(
                        "text-underline",
                        "Underline",
                        YabaEditorCommands.ToggleUnderline
                    ) { it.underline },
                    FormatMenuRow(
                        "text-strikethrough",
                        "Strikethrough",
                        YabaEditorCommands.ToggleStrikethrough
                    ) { it.strikethrough },
                    FormatMenuRow(
                        "text-subscript",
                        "Subscript",
                        YabaEditorCommands.ToggleSubscript
                    ) { it.subscript },
                    FormatMenuRow(
                        "text-superscript",
                        "Superscript",
                        YabaEditorCommands.ToggleSuperscript
                    ) { it.superscript },
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
private fun TableEditDropdown(
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
        ) { YabaIcon(name = "grid-table", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 6),
                    checked = false,
                    enabled = formatting.canAddRowAfter,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.AddRowAfter)
                    },
                    leadingIcon = { YabaIcon(name = "row-insert") },
                    text = { Text(text = "Add row below") }, // TODO: localize
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(1, 6),
                    checked = false,
                    enabled = formatting.canAddRowBefore,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.AddRowBefore)
                    },
                    leadingIcon = { YabaIcon(name = "row-insert") },
                    text = { Text(text = "Add row above") }, // TODO: localize
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(2, 6),
                    checked = false,
                    enabled = formatting.canAddColumnAfter,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.AddColumnAfter)
                    },
                    leadingIcon = { YabaIcon(name = "column-insert") },
                    text = { Text(text = "Add column to right") }, // TODO: localize
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(3, 6),
                    checked = false,
                    enabled = formatting.canAddColumnBefore,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.AddColumnBefore)
                    },
                    leadingIcon = { YabaIcon(name = "column-insert") },
                    text = { Text(text = "Add column to left") }, // TODO: localize
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(4, 6),
                    checked = false,
                    enabled = formatting.canDeleteRow,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.DeleteRow)
                    },
                    leadingIcon = { YabaIcon(name = "row-delete") },
                    text = { Text(text = "Remove row") }, // TODO: localize
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(5, 6),
                    checked = false,
                    enabled = formatting.canDeleteColumn,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.DeleteColumn)
                    },
                    leadingIcon = { YabaIcon(name = "column-delete") },
                    text = { Text(text = "Remove column") }, // TODO: localize
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
    onOpenTableInsertSheet: () -> Unit,
    onOpenMathSheet: (isBlock: Boolean) -> Unit,
    onPickImageFromGallery: () -> Unit,
    onCaptureImageFromCamera: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var listSubExpanded by remember { mutableStateOf(false) }
    var imageSubExpanded by remember { mutableStateOf(false) }
    var codeSubExpanded by remember { mutableStateOf(false) }
    var mathSubExpanded by remember { mutableStateOf(false) }

    val anyInsertToggleActive by remember(formatting) {
        derivedStateOf { YabaEditorCommands.hasAnyInsertMenuToggle(formatting) }
    }

    LaunchedEffect(expanded) {
        if (expanded.not()) {
            listSubExpanded = false
            imageSubExpanded = false
            codeSubExpanded = false
            mathSubExpanded = false
        }
    }

    Box {
        IconButton(
            onClick = { expanded = expanded.not() },
            colors = bookmarkReaderToolbarToggleIconButtonColors(
                folderYabaColor,
                anyInsertToggleActive,
            ),
            shapes = IconButtonDefaults.shapes(),
        ) { YabaIcon(name = "add-01", color = Color.White) }
        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(0, 8),
                    checked = false,
                    onCheckedChange = { _ ->
                        expanded = false
                        onOpenTableInsertSheet()
                    },
                    leadingIcon = { YabaIcon(name = "grid-table") },
                    text = { Text(text = "Table") }, // TODO: localize
                )
                Box {
                    val imageTrailingRotation by animateFloatAsState(
                        targetValue = if (imageSubExpanded) 90f else 0f,
                    )
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(1, 8),
                        checked = false,
                        onCheckedChange = { _ -> imageSubExpanded = true },
                        leadingIcon = { YabaIcon(name = "image-add-02") },
                        trailingIcon = {
                            YabaIcon(
                                modifier = Modifier.rotate(imageTrailingRotation),
                                name = "arrow-right-01",
                            )
                        },
                        text = { Text(text = "Image") }, // TODO: localize
                    )
                    DropdownMenuPopup(
                        expanded = imageSubExpanded,
                        onDismissRequest = { imageSubExpanded = false },
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 1),
                        ) {
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(0, 2),
                                checked = false,
                                onCheckedChange = { _ ->
                                    imageSubExpanded = false
                                    expanded = false
                                    onCaptureImageFromCamera()
                                },
                                leadingIcon = { YabaIcon(name = "camera-01") },
                                text = { Text(text = "Take Photo") }, // TODO: localize
                            )
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(1, 2),
                                checked = false,
                                onCheckedChange = { _ ->
                                    imageSubExpanded = false
                                    expanded = false
                                    onPickImageFromGallery()
                                },
                                leadingIcon = { YabaIcon(name = "add-circle") },
                                text = { Text(text = "Pick From Gallery") }, // TODO: localize
                            )
                        }
                    }
                }
                Box {
                    val codeTrailingRotation by animateFloatAsState(
                        targetValue = if (codeSubExpanded) 90f else 0f,
                    )
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(2, 8),
                        checked = formatting.code || formatting.codeBlock,
                        onCheckedChange = { _ -> codeSubExpanded = true },
                        leadingIcon = { YabaIcon(name = "source-code") },
                        trailingIcon = {
                            YabaIcon(
                                modifier = Modifier.rotate(codeTrailingRotation),
                                name = "arrow-right-01",
                            )
                        },
                        text = { Text(text = "Code") },
                    )
                    DropdownMenuPopup(
                        expanded = codeSubExpanded,
                        onDismissRequest = { codeSubExpanded = false },
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 1),
                        ) {
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(0, 2),
                                checked = formatting.code,
                                onCheckedChange = { _ ->
                                    codeSubExpanded = false
                                    expanded = false
                                    onDispatchCommand(YabaEditorCommands.ToggleCode)
                                },
                                leadingIcon = { YabaIcon(name = "code") },
                                text = { Text(text = "Inline code") },
                            )
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(1, 2),
                                checked = formatting.codeBlock,
                                onCheckedChange = { _ ->
                                    codeSubExpanded = false
                                    expanded = false
                                    onDispatchCommand(YabaEditorCommands.ToggleCodeBlock)
                                },
                                leadingIcon = { YabaIcon(name = "source-code-square") },
                                text = { Text(text = "Code block") },
                            )
                        }
                    }
                }
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(3, 8),
                    checked = formatting.blockquote,
                    onCheckedChange = { _ ->
                        expanded = false
                        onDispatchCommand(YabaEditorCommands.ToggleQuote)
                    },
                    leadingIcon = { YabaIcon(name = "quote-down") },
                    text = { Text(text = "Quote") },
                )
                DropdownMenuItem(
                    shapes = MenuDefaults.itemShape(4, 8),
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
                        shapes = MenuDefaults.itemShape(5, 8),
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
                    shapes = MenuDefaults.itemShape(6, 8),
                    checked = false,
                    enabled = false,
                    onCheckedChange = {},
                    leadingIcon = { YabaIcon(name = "link-04") },
                    text = { Text(text = "Link") },
                )
                Box {
                    val mathTrailingRotation by animateFloatAsState(
                        targetValue = if (mathSubExpanded) 90f else 0f,
                    )
                    DropdownMenuItem(
                        shapes = MenuDefaults.itemShape(7, 8),
                        checked = formatting.inlineMath || formatting.blockMath,
                        onCheckedChange = { _ -> mathSubExpanded = true },
                        leadingIcon = { YabaIcon(name = "calculator") },
                        trailingIcon = {
                            YabaIcon(
                                modifier = Modifier.rotate(mathTrailingRotation),
                                name = "arrow-right-01",
                            )
                        },
                        text = { Text(text = "Math") },
                    )
                    DropdownMenuPopup(
                        expanded = mathSubExpanded,
                        onDismissRequest = { mathSubExpanded = false },
                    ) {
                        DropdownMenuGroup(
                            shapes = MenuDefaults.groupShape(index = 0, count = 1),
                        ) {
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(0, 2),
                                checked = formatting.inlineMath,
                                onCheckedChange = { _ ->
                                    mathSubExpanded = false
                                    expanded = false
                                    onOpenMathSheet(false)
                                },
                                leadingIcon = { YabaIcon(name = "absolute") },
                                text = { Text(text = "Inline math") },
                            )
                            DropdownMenuItem(
                                shapes = MenuDefaults.itemShape(1, 2),
                                checked = formatting.blockMath,
                                onCheckedChange = { _ ->
                                    mathSubExpanded = false
                                    expanded = false
                                    onOpenMathSheet(true)
                                },
                                leadingIcon = { YabaIcon(name = "alpha-square") },
                                text = { Text(text = "Math block") },
                            )
                        }
                    }
                }
            }
        }
    }
}
