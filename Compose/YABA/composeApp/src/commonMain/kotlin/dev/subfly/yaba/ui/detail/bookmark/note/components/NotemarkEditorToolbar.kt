package dev.subfly.yaba.ui.detail.bookmark.note.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.YabaEditorCommands

private enum class ToolbarGroup {
    TableEdit,
    Heading,
    Text,
    Insert,
    Indent,
    History,
}

private enum class InsertNestedGroup {
    Image,
    Code,
    List,
    Math,
}

private enum class TableNestedGroup {
    Row,
    Column,
}

private data class ToolbarAction(
    val key: String,
    val icon: String,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val segmentAlpha: Float? = null,
    val onClick: () -> Unit,
)

private enum class ExtraPadding {
    START, END, NONE
}

private const val ToolbarBaseAlpha = 0.5f
private const val ToolbarSelectedOverlayAlpha = 0.42f
private const val ToolbarSelectedOnSegmentOverlayAlpha = 0.28f
private const val ToolbarNestedDepthStep = 0.1f
private const val ToolbarExpandedAreaOffset = 0.05f
private const val ToolbarExpandedToggleOffset = 0.1f

private fun clampToolbarAlpha(alpha: Float): Float = if (alpha > 1f) 1f else alpha

private fun expandedAreaAlpha(depth: Int): Float =
    clampToolbarAlpha(ToolbarBaseAlpha + ToolbarExpandedAreaOffset + (depth * ToolbarNestedDepthStep))

private fun expandedToggleAlpha(depth: Int): Float =
    clampToolbarAlpha(ToolbarBaseAlpha + ToolbarExpandedToggleOffset + (depth * ToolbarNestedDepthStep))

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
    onOpenLinkSheet: () -> Unit,
    onOpenMentionSheet: () -> Unit,
    onPickImageFromGallery: () -> Unit,
    onCaptureImageFromCamera: () -> Unit,
    onSaveDocument: () -> Unit,
) {
    val toolbarSaveOpaqueColor = Color(color.iconTintArgb())
    var activeGroup by remember { mutableStateOf<ToolbarGroup?>(null) }
    var activeInsertNestedGroup by remember { mutableStateOf<InsertNestedGroup?>(null) }
    var activeTableNestedGroup by remember { mutableStateOf<TableNestedGroup?>(null) }

    LaunchedEffect(formatting.inTable) {
        if (!formatting.inTable && activeGroup == ToolbarGroup.TableEdit) {
            activeGroup = null
        }
    }

    LaunchedEffect(activeGroup) {
        if (activeGroup != ToolbarGroup.Insert) {
            activeInsertNestedGroup = null
        }
        if (activeGroup != ToolbarGroup.TableEdit) {
            activeTableNestedGroup = null
        }
    }

    val actions = remember(
        formatting,
        activeGroup,
        activeInsertNestedGroup,
        activeTableNestedGroup,
        onDispatchCommand,
        onOpenTableInsertSheet,
        onOpenMathSheet,
        onOpenLinkSheet,
        onOpenMentionSheet,
        onPickImageFromGallery,
        onCaptureImageFromCamera,
        onHighlightInactiveClick,
        onHighlightActiveClick,
    ) {
        mutableListOf<ToolbarAction>().apply {
            fun toggleGroup(group: ToolbarGroup) {
                activeGroup = if (activeGroup == group) null else group
                if (group != ToolbarGroup.Insert) {
                    activeInsertNestedGroup = null
                }
            }

            fun toggleInsertNestedGroup(group: InsertNestedGroup) {
                activeGroup = ToolbarGroup.Insert
                activeInsertNestedGroup = if (activeInsertNestedGroup == group) null else group
            }

            fun toggleTableNestedGroup(group: TableNestedGroup) {
                activeGroup = ToolbarGroup.TableEdit
                activeTableNestedGroup = if (activeTableNestedGroup == group) null else group
            }

            if (formatting.inTable) {
                val isExpanded = activeGroup == ToolbarGroup.TableEdit
                add(
                    ToolbarAction(
                        key = "group-table",
                        icon = "grid-table",
                        selected = true,
                        segmentAlpha = if (isExpanded) expandedToggleAlpha(depth = 0) else null,
                        onClick = { toggleGroup(ToolbarGroup.TableEdit) },
                    ),
                )
                if (isExpanded) {
                    val rowExpanded = activeTableNestedGroup == TableNestedGroup.Row
                    add(
                        ToolbarAction(
                            key = "table-insert-row",
                            icon = "insert-row",
                            selected = rowExpanded,
                            segmentAlpha =
                                if (rowExpanded) expandedToggleAlpha(depth = 1) else expandedAreaAlpha(depth = 0),
                            onClick = { toggleTableNestedGroup(TableNestedGroup.Row) },
                        ),
                    )
                    if (rowExpanded) {
                        add(
                            ToolbarAction(
                                key = "table-row-before",
                                icon = "insert-row-up",
                                enabled = formatting.canAddRowBefore,
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onDispatchCommand(YabaEditorCommands.AddRowBefore) },
                            ),
                        )
                        add(
                            ToolbarAction(
                                key = "table-row-after",
                                icon = "insert-row-down",
                                enabled = formatting.canAddRowAfter,
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onDispatchCommand(YabaEditorCommands.AddRowAfter) },
                            ),
                        )
                    }

                    val columnExpanded = activeTableNestedGroup == TableNestedGroup.Column
                    add(
                        ToolbarAction(
                            key = "table-insert-column",
                            icon = "insert-column",
                            selected = columnExpanded,
                            segmentAlpha =
                                if (columnExpanded) {
                                    expandedToggleAlpha(depth = 1)
                                } else {
                                    expandedAreaAlpha(depth = 0)
                                },
                            onClick = { toggleTableNestedGroup(TableNestedGroup.Column) },
                        ),
                    )
                    if (columnExpanded) {
                        add(
                            ToolbarAction(
                                key = "table-column-before",
                                icon = "insert-column-left",
                                enabled = formatting.canAddColumnBefore,
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onDispatchCommand(YabaEditorCommands.AddColumnBefore) },
                            ),
                        )
                        add(
                            ToolbarAction(
                                key = "table-column-after",
                                icon = "insert-column-right",
                                enabled = formatting.canAddColumnAfter,
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onDispatchCommand(YabaEditorCommands.AddColumnAfter) },
                            ),
                        )
                    }
                    add(
                        ToolbarAction(
                            key = "table-delete-row",
                            icon = "delete-row",
                            enabled = formatting.canDeleteRow,
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onDispatchCommand(YabaEditorCommands.DeleteRow) },
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "table-delete-column",
                            icon = "delete-column",
                            enabled = formatting.canDeleteColumn,
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onDispatchCommand(YabaEditorCommands.DeleteColumn) },
                        ),
                    )
                }
            }

            val headingExpanded = activeGroup == ToolbarGroup.Heading
            add(
                ToolbarAction(
                    key = "group-heading",
                    icon = "heading",
                    selected = headingExpanded || formatting.headingLevel > 0,
                    segmentAlpha = if (headingExpanded) expandedToggleAlpha(depth = 0) else null,
                    onClick = { toggleGroup(ToolbarGroup.Heading) },
                ),
            )
            if (headingExpanded) {
                (1..6).forEach { level ->
                    add(
                        ToolbarAction(
                            key = "heading-$level",
                            icon = "heading-${level.toString().padStart(2, '0')}",
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onDispatchCommand(YabaEditorCommands.setHeadingPayload(level)) },
                        ),
                    )
                }
            }

            val textExpanded = activeGroup == ToolbarGroup.Text
            add(
                ToolbarAction(
                    key = "group-text",
                    icon = "text-font",
                    selected = textExpanded || YabaEditorCommands.hasAnyTextMark(formatting),
                    segmentAlpha = if (textExpanded) expandedToggleAlpha(depth = 0) else null,
                    onClick = { toggleGroup(ToolbarGroup.Text) },
                ),
            )
            if (textExpanded) {
                add(
                    ToolbarAction(
                        key = "text-bold",
                        icon = "text-bold",
                        selected = formatting.bold,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleBold) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "text-italic",
                        icon = "text-italic",
                        selected = formatting.italic,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleItalic) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "text-underline",
                        icon = "text-underline",
                        selected = formatting.underline,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleUnderline) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "text-strike",
                        icon = "text-strikethrough",
                        selected = formatting.strikethrough,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleStrikethrough) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "text-subscript",
                        icon = "text-subscript",
                        selected = formatting.subscript,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleSubscript) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "text-superscript",
                        icon = "text-superscript",
                        selected = formatting.superscript,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleSuperscript) },
                    ),
                )
            }

            val insertExpanded = activeGroup == ToolbarGroup.Insert
            add(
                ToolbarAction(
                    key = "group-insert",
                    icon = "add-01",
                    selected = insertExpanded || YabaEditorCommands.hasAnyInsertMenuToggle(formatting),
                    segmentAlpha = if (insertExpanded) expandedToggleAlpha(depth = 0) else null,
                    onClick = { toggleGroup(ToolbarGroup.Insert) },
                ),
            )
            if (insertExpanded) {
                add(
                    ToolbarAction(
                        key = "insert-table",
                        icon = "grid-table",
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = onOpenTableInsertSheet,
                    ),
                )
                add(
                    ToolbarAction(
                        key = "insert-link",
                        icon = "link-04",
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = onOpenLinkSheet,
                    ),
                )
                add(
                    ToolbarAction(
                        key = "insert-mention",
                        icon = "at",
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = onOpenMentionSheet,
                    ),
                )

                val imageExpanded = activeInsertNestedGroup == InsertNestedGroup.Image
                add(
                    ToolbarAction(
                        key = "insert-image",
                        icon = "image-add-02",
                        segmentAlpha =
                            if (imageExpanded) expandedToggleAlpha(depth = 1) else expandedAreaAlpha(depth = 0),
                        onClick = { toggleInsertNestedGroup(InsertNestedGroup.Image) },
                    ),
                )
                if (imageExpanded) {
                    add(
                        ToolbarAction(
                            key = "insert-image-camera",
                            icon = "camera-01",
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = onCaptureImageFromCamera,
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "insert-image-gallery",
                            icon = "image-02",
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = onPickImageFromGallery,
                        ),
                    )
                }

                val codeExpanded = activeInsertNestedGroup == InsertNestedGroup.Code
                add(
                    ToolbarAction(
                        key = "insert-code",
                        icon = "source-code",
                        selected = formatting.code || formatting.codeBlock,
                        segmentAlpha =
                            if (codeExpanded) expandedToggleAlpha(depth = 1) else expandedAreaAlpha(depth = 0),
                        onClick = { toggleInsertNestedGroup(InsertNestedGroup.Code) },
                    ),
                )
                if (codeExpanded) {
                    add(
                        ToolbarAction(
                            key = "insert-code-inline",
                            icon = "code",
                            selected = formatting.code,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onDispatchCommand(YabaEditorCommands.ToggleCode) },
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "insert-code-block",
                            icon = "source-code-square",
                            selected = formatting.codeBlock,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onDispatchCommand(YabaEditorCommands.ToggleCodeBlock) },
                        ),
                    )
                }

                add(
                    ToolbarAction(
                        key = "insert-quote",
                        icon = "quote-down",
                        selected = formatting.blockquote,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.ToggleQuote) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "insert-hr",
                        icon = "solid-line-01",
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.InsertHr) },
                    ),
                )

                val listExpanded = activeInsertNestedGroup == InsertNestedGroup.List
                add(
                    ToolbarAction(
                        key = "insert-list",
                        icon = "left-to-right-list-dash",
                        selected = formatting.bulletList || formatting.orderedList || formatting.taskList,
                        segmentAlpha =
                            if (listExpanded) expandedToggleAlpha(depth = 1) else expandedAreaAlpha(depth = 0),
                        onClick = { toggleInsertNestedGroup(InsertNestedGroup.List) },
                    ),
                )
                if (listExpanded) {
                    add(
                        ToolbarAction(
                            key = "insert-list-bullet",
                            icon = "left-to-right-list-bullet",
                            selected = formatting.bulletList,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onDispatchCommand(YabaEditorCommands.ToggleBulletedList) },
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "insert-list-number",
                            icon = "left-to-right-list-number",
                            selected = formatting.orderedList,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onDispatchCommand(YabaEditorCommands.ToggleNumberedList) },
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "insert-list-task",
                            icon = "check-list",
                            selected = formatting.taskList,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onDispatchCommand(YabaEditorCommands.ToggleTaskList) },
                        ),
                    )
                }

                val mathExpanded = activeInsertNestedGroup == InsertNestedGroup.Math
                add(
                    ToolbarAction(
                        key = "insert-math",
                        icon = "calculator",
                        selected = formatting.inlineMath || formatting.blockMath,
                        segmentAlpha =
                            if (mathExpanded) expandedToggleAlpha(depth = 1) else expandedAreaAlpha(depth = 0),
                        onClick = { toggleInsertNestedGroup(InsertNestedGroup.Math) },
                    ),
                )
                if (mathExpanded) {
                    add(
                        ToolbarAction(
                            key = "insert-math-inline",
                            icon = "absolute",
                            selected = formatting.inlineMath,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onOpenMathSheet(false) },
                        ),
                    )
                    add(
                        ToolbarAction(
                            key = "insert-math-block",
                            icon = "alpha-square",
                            selected = formatting.blockMath,
                            segmentAlpha = expandedAreaAlpha(depth = 1),
                            onClick = { onOpenMathSheet(true) },
                        ),
                    )
                }
            }

            add(
                ToolbarAction(
                    key = "highlight",
                    icon = "highlighter",
                    selected = formatting.textHighlight,
                    onClick = {
                        if (formatting.textHighlight) {
                            onHighlightActiveClick()
                        } else {
                            onHighlightInactiveClick()
                        }
                    },
                ),
            )

            val indentExpanded = activeGroup == ToolbarGroup.Indent
            add(
                ToolbarAction(
                    key = "group-indent",
                    icon = "text-indent",
                    selected = indentExpanded,
                    segmentAlpha = if (indentExpanded) expandedToggleAlpha(depth = 0) else null,
                    onClick = { toggleGroup(ToolbarGroup.Indent) },
                ),
            )
            if (indentExpanded) {
                add(
                    ToolbarAction(
                        key = "indent-more",
                        icon = "text-indent-more",
                        enabled = formatting.canIndent,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.Indent) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "indent-less",
                        icon = "text-indent-less",
                        enabled = formatting.canOutdent,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.Outdent) },
                    ),
                )
            }

            val historyExpanded = activeGroup == ToolbarGroup.History
            add(
                ToolbarAction(
                    key = "group-history",
                    icon = "repeat",
                    selected = historyExpanded,
                    segmentAlpha = if (historyExpanded) expandedToggleAlpha(depth = 0) else null,
                    onClick = { toggleGroup(ToolbarGroup.History) },
                ),
            )
            if (historyExpanded) {
                add(
                    ToolbarAction(
                        key = "history-undo",
                        icon = "undo-03",
                        enabled = formatting.canUndo,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.Undo) },
                    ),
                )
                add(
                    ToolbarAction(
                        key = "history-redo",
                        icon = "redo-03",
                        enabled = formatting.canRedo,
                        segmentAlpha = expandedAreaAlpha(depth = 0),
                        onClick = { onDispatchCommand(YabaEditorCommands.Redo) },
                    ),
                )
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            items(actions, key = { it.key }) { action ->
                val extraPadding = when (action) {
                    actions.first() -> ExtraPadding.START
                    actions.last() -> ExtraPadding.END
                    else -> ExtraPadding.NONE
                }

                ToolbarActionButton(
                    modifier = Modifier.animateItem(),
                    folderYabaColor = color,
                    extraPadding = extraPadding,
                    action = action,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .background(color = toolbarSaveOpaqueColor)
                    .padding(horizontal = 8.dp)
                    .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onSaveDocument,
                colors = bookmarkReaderToolbarIconButtonColors(color),
                shapes = IconButtonDefaults.shapes(),
            ) { YabaIcon(name = "floppy-disk", color = Color.White) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarActionButton(
    modifier: Modifier = Modifier,
    folderYabaColor: YabaColor,
    extraPadding: ExtraPadding,
    action: ToolbarAction,
) {
    val segmentColor = Color(
        folderYabaColor.iconTintArgb()
    ).copy(alpha = action.segmentAlpha ?: ToolbarBaseAlpha)

    Box(
        modifier = modifier
            .background(segmentColor)
            .padding(
                start = if (extraPadding == ExtraPadding.START) 8.dp else 0.dp,
                end = if (extraPadding == ExtraPadding.END) 8.dp else 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = action.onClick,
                enabled = action.enabled,
                colors = rememberNotemarkToolbarButtonColors(
                    color = folderYabaColor,
                    selected = action.selected,
                    segmentAlpha = action.segmentAlpha,
                ),
                shapes = IconButtonDefaults.shapes(),
            ) {
                YabaIcon(
                    name = action.icon,
                    color = Color.White.copy(alpha = if (action.enabled) 1f else 0.5f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberNotemarkToolbarButtonColors(
    color: YabaColor,
    selected: Boolean,
    segmentAlpha: Float?,
): IconButtonColors {
    val folderColor = Color(color.iconTintArgb())
    val containerColor =
        when {
            selected && segmentAlpha != null -> Color.White.copy(alpha = ToolbarSelectedOnSegmentOverlayAlpha)
            selected -> folderColor.copy(alpha = ToolbarSelectedOverlayAlpha)
            else -> Color.Transparent
        }
    return IconButtonDefaults.iconButtonColors(
        containerColor = containerColor,
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.5f),
    )
}
