@file:OptIn(ExperimentalFoundationApi::class)

package dev.subfly.yaba.ui.detail.bookmark.canvas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.webview.CanvasHostMetrics
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkReaderToolbarIconButtonColors

private enum class CanvasToolbarGroup {
    Line,
    Insert,
    History,
}

private enum class CanvasInsertNested {
    Image,
    Shape,
}

private data class CanvasToolbarAction(
    val key: String,
    val icon: String,
    val tooltipText: String,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val segmentAlpha: Float? = null,
    val iconColorOverride: Color? = null,
    val onClick: () -> Unit,
)

private enum class CanvasExtraPadding {
    START,
    END,
    NONE,
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
private fun CanvasToolbarPlainTooltipBox(
    modifier: Modifier = Modifier,
    tooltipText: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
        ),
        tooltip = {
            PlainTooltip { Text(tooltipText) }
        },
        state = rememberTooltipState(),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun CanvmarkEditorToolbar(
    modifier: Modifier = Modifier,
    color: YabaColor,
    metrics: CanvasHostMetrics,
    optionsSheetVisible: Boolean,
    onToolSelected: (String) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleOptionsSheet: () -> Unit,
    onPickImageFromGallery: () -> Unit,
    onCaptureImageFromCamera: () -> Unit,
    onSaveDocument: () -> Unit,
) {
    val toolbarSaveOpaqueColor = Color(color.iconTintArgb())
    var activeGroup by remember { mutableStateOf<CanvasToolbarGroup?>(null) }
    var activeInsertNested by remember { mutableStateOf<CanvasInsertNested?>(null) }

    LaunchedEffect(activeGroup) {
        if (activeGroup != CanvasToolbarGroup.Insert) {
            activeInsertNested = null
        }
    }

    val actions =
        remember(
            metrics,
            activeGroup,
            activeInsertNested,
            onToolSelected,
            onUndo,
            onRedo,
            onPickImageFromGallery,
            onCaptureImageFromCamera,
        ) {
            mutableListOf<CanvasToolbarAction>().apply {
                fun toggleGroup(group: CanvasToolbarGroup) {
                    activeGroup = if (activeGroup == group) null else group
                    if (group != CanvasToolbarGroup.Insert) {
                        activeInsertNested = null
                    }
                }

                fun toggleInsertNested(nested: CanvasInsertNested) {
                    activeGroup = CanvasToolbarGroup.Insert
                    activeInsertNested = if (activeInsertNested == nested) null else nested
                }

                fun isToolActive(tool: String): Boolean = metrics.activeTool == tool

                listOf(
                    Triple("selection", "cursor-rectangle-selection-01", "Selection"),
                    Triple("hand", "drag-04", "Pan"),
                    Triple("draw", "paint-brush-01", "Draw"),
                    Triple("eraser", "eraser", "Eraser"),
                ).forEach { (tool, icon, tip) ->
                    add(
                        CanvasToolbarAction(
                            key = "tool-$tool",
                            icon = icon,
                            tooltipText = tip,
                            selected = isToolActive(tool),
                            segmentAlpha = null,
                            onClick = { onToolSelected(tool) },
                        ),
                    )
                }

                val lineExpanded = activeGroup == CanvasToolbarGroup.Line
                val lineOrArrowActive = isToolActive("line") || isToolActive("arrow")
                add(
                    CanvasToolbarAction(
                        key = "group-line",
                        icon = "arrow-left-right-round",
                        tooltipText = "Line / Arrow",
                        selected = lineExpanded || lineOrArrowActive,
                        segmentAlpha = if (lineExpanded) expandedToggleAlpha(depth = 0) else null,
                        onClick = { toggleGroup(CanvasToolbarGroup.Line) },
                    ),
                )
                if (lineExpanded) {
                    add(
                        CanvasToolbarAction(
                            key = "tool-line",
                            icon = "liner",
                            tooltipText = "Line",
                            selected = isToolActive("line"),
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onToolSelected("line") },
                        ),
                    )
                    add(
                        CanvasToolbarAction(
                            key = "tool-arrow",
                            icon = "arrow-up-right-01",
                            tooltipText = "Arrow",
                            selected = isToolActive("arrow"),
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onToolSelected("arrow") },
                        ),
                    )
                }

                val insertExpanded = activeGroup == CanvasToolbarGroup.Insert
                val insertMenuActive =
                    isToolActive("text") ||
                            isToolActive("frame") ||
                            isToolActive("ellipse") ||
                            isToolActive("diamond") ||
                            isToolActive("rectangle")
                add(
                    CanvasToolbarAction(
                        key = "group-insert",
                        icon = "add-01",
                        tooltipText = "Insert",
                        selected = insertExpanded || insertMenuActive,
                        segmentAlpha = if (insertExpanded) expandedToggleAlpha(depth = 0) else null,
                        onClick = { toggleGroup(CanvasToolbarGroup.Insert) },
                    ),
                )
                if (insertExpanded) {
                    add(
                        CanvasToolbarAction(
                            key = "tool-text",
                            icon = "text",
                            tooltipText = "Text",
                            selected = isToolActive("text"),
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onToolSelected("text") },
                        ),
                    )
                    val imageNested = activeInsertNested == CanvasInsertNested.Image
                    add(
                        CanvasToolbarAction(
                            key = "nested-image",
                            icon = "image-add-02",
                            tooltipText = "Image",
                            selected = imageNested,
                            segmentAlpha =
                                if (imageNested) {
                                    expandedToggleAlpha(depth = 1)
                                } else {
                                    expandedAreaAlpha(depth = 0)
                                },
                            onClick = { toggleInsertNested(CanvasInsertNested.Image) },
                        ),
                    )
                    if (imageNested) {
                        add(
                            CanvasToolbarAction(
                                key = "image-gallery",
                                icon = "image-02",
                                tooltipText = "Gallery",
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = {
                                    onPickImageFromGallery()
                                    activeInsertNested = null
                                    activeGroup = null
                                },
                            ),
                        )
                        add(
                            CanvasToolbarAction(
                                key = "image-camera",
                                icon = "camera-01",
                                tooltipText = "Camera",
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = {
                                    onCaptureImageFromCamera()
                                    activeInsertNested = null
                                    activeGroup = null
                                },
                            ),
                        )
                    }
                    add(
                        CanvasToolbarAction(
                            key = "tool-frame",
                            icon = "dashed-line-02",
                            tooltipText = "Frame",
                            selected = isToolActive("frame"),
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = { onToolSelected("frame") },
                        ),
                    )
                    val shapeNested = activeInsertNested == CanvasInsertNested.Shape
                    add(
                        CanvasToolbarAction(
                            key = "nested-shape",
                            icon = "shapes",
                            tooltipText = "Shape",
                            selected =
                                shapeNested ||
                                        isToolActive("ellipse") ||
                                        isToolActive("diamond") ||
                                        isToolActive("rectangle"),
                            segmentAlpha =
                                if (shapeNested) {
                                    expandedToggleAlpha(depth = 1)
                                } else {
                                    expandedAreaAlpha(depth = 0)
                                },
                            onClick = { toggleInsertNested(CanvasInsertNested.Shape) },
                        ),
                    )
                    if (shapeNested) {
                        add(
                            CanvasToolbarAction(
                                key = "shape-ellipse",
                                icon = "circle",
                                tooltipText = "Circle",
                                selected = isToolActive("ellipse"),
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onToolSelected("ellipse") },
                            ),
                        )
                        add(
                            CanvasToolbarAction(
                                key = "shape-diamond",
                                icon = "diamond",
                                tooltipText = "Diamond",
                                selected = isToolActive("diamond"),
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onToolSelected("diamond") },
                            ),
                        )
                        add(
                            CanvasToolbarAction(
                                key = "shape-rect",
                                icon = "square",
                                tooltipText = "Rectangle",
                                selected = isToolActive("rectangle"),
                                segmentAlpha = expandedAreaAlpha(depth = 1),
                                onClick = { onToolSelected("rectangle") },
                            ),
                        )
                    }
                }

                val historyExpanded = activeGroup == CanvasToolbarGroup.History
                add(
                    CanvasToolbarAction(
                        key = "group-history",
                        icon = "repeat",
                        tooltipText = "History",
                        selected = historyExpanded,
                        segmentAlpha = if (historyExpanded) expandedToggleAlpha(depth = 0) else null,
                        onClick = { toggleGroup(CanvasToolbarGroup.History) },
                    ),
                )
                if (historyExpanded) {
                    add(
                        CanvasToolbarAction(
                            key = "history-undo",
                            icon = "undo-03",
                            tooltipText = "Undo",
                            enabled = metrics.canUndo,
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = onUndo,
                        ),
                    )
                    add(
                        CanvasToolbarAction(
                            key = "history-redo",
                            icon = "redo-03",
                            tooltipText = "Redo",
                            enabled = metrics.canRedo,
                            segmentAlpha = expandedAreaAlpha(depth = 0),
                            onClick = onRedo,
                        ),
                    )
                }

            }
        }

    val showOptionsAction =
        metrics.activeTool == "selection" && metrics.hasSelection

    val toolbarHead = actions.take(2)
    val toolbarTail = actions.drop(2)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            items(
                items = toolbarHead,
                key = { it.key },
            ) { action ->
                val extraPadding =
                    if (action.key == actions.first().key) {
                        CanvasExtraPadding.START
                    } else {
                        CanvasExtraPadding.NONE
                    }

                CanvasToolbarActionButton(
                    modifier = Modifier.animateItem(),
                    folderYabaColor = color,
                    extraPadding = extraPadding,
                    action = action,
                )
            }

            item(key = "canvas-options-slot") {
                AnimatedVisibility(visible = showOptionsAction) {
                    Box(
                        modifier = Modifier
                            .background(
                                color =
                                    Color(color.iconTintArgb()).copy(
                                        alpha =
                                            if (optionsSheetVisible) {
                                                0.72f
                                            } else {
                                                ToolbarBaseAlpha
                                            },
                                    ),
                            )
                            .padding(horizontal = 4.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CanvasToolbarPlainTooltipBox(tooltipText = "Options") {
                            IconButton(
                                onClick = onToggleOptionsSheet,
                                colors = bookmarkReaderToolbarIconButtonColors(color),
                                shapes = IconButtonDefaults.shapes(),
                            ) {
                                YabaIcon(
                                    name = "settings-05",
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }

            items(
                items = toolbarTail,
                key = { it.key },
            ) { action ->
                val extraPadding =
                    if (action.key == actions.last().key) {
                        CanvasExtraPadding.END
                    } else {
                        CanvasExtraPadding.NONE
                    }

                CanvasToolbarActionButton(
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
            CanvasToolbarPlainTooltipBox(tooltipText = "Save") {
                IconButton(
                    onClick = onSaveDocument,
                    colors = bookmarkReaderToolbarIconButtonColors(color),
                    shapes = IconButtonDefaults.shapes(),
                ) { YabaIcon(name = "floppy-disk", color = Color.White) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CanvasToolbarActionButton(
    modifier: Modifier = Modifier,
    folderYabaColor: YabaColor,
    extraPadding: CanvasExtraPadding,
    action: CanvasToolbarAction,
) {
    val segmentColor =
        Color(folderYabaColor.iconTintArgb()).copy(alpha = action.segmentAlpha ?: ToolbarBaseAlpha)

    Box(
        modifier = modifier
            .background(segmentColor)
            .padding(
                start = if (extraPadding == CanvasExtraPadding.START) 8.dp else 0.dp,
                end = if (extraPadding == CanvasExtraPadding.END) 8.dp else 0.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            CanvasToolbarPlainTooltipBox(tooltipText = action.tooltipText) {
                IconButton(
                    onClick = action.onClick,
                    enabled = action.enabled,
                    colors =
                        rememberCanvasToolbarButtonColors(
                            color = folderYabaColor,
                            selected = action.selected,
                            segmentAlpha = action.segmentAlpha,
                        ),
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    val iconColor =
                        action.iconColorOverride
                            ?: Color.White.copy(alpha = if (action.enabled) 1f else 0.5f)
                    YabaIcon(name = action.icon, color = iconColor)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun rememberCanvasToolbarButtonColors(
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
