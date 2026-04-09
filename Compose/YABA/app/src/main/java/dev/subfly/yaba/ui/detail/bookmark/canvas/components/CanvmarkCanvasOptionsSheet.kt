package dev.subfly.yaba.ui.detail.bookmark.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.webview.CanvasHostStyleState
import dev.subfly.yaba.core.webview.CanvasOptionGroups
import dev.subfly.yaba.core.webview.CanvasSelectionStylePatch
import dev.subfly.yaba.core.webview.DefaultExcalidrawArrowheadPickerOrder
import kotlin.math.roundToInt

// TODO: LOCALIZATIONS
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CanvmarkCanvasOptionsSheet(
    modifier: Modifier = Modifier,
    style: CanvasHostStyleState,
    onApplyPatch: (CanvasSelectionStylePatch) -> Unit,
    onLayer: (String) -> Unit,
    onDeleteSelected: () -> Unit,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.4f).dp
    val startKeys =
        style.availableStartArrowheads.ifEmpty { DefaultExcalidrawArrowheadPickerOrder }
    val endKeys = style.availableEndArrowheads.ifEmpty { DefaultExcalidrawArrowheadPickerOrder }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (style.hasOptionGroup(CanvasOptionGroups.STROKE)) {
            Text("Stroke", style = MaterialTheme.typography.labelLarge)
            ColorPaletteRow(
                colors = YabaColor.entries.filter { it != YabaColor.NONE },
                selectedCode = style.strokeYabaCode,
                mixed = style.mixedStroke,
                onSelect = { code -> onApplyPatch(CanvasSelectionStylePatch(strokeYabaCode = code)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.BACKGROUND)) {
            Text("Fill", style = MaterialTheme.typography.labelLarge)
            ColorPaletteRow(
                colors = YabaColor.entries,
                selectedCode = style.backgroundYabaCode,
                mixed = style.mixedBackground,
                onSelect = { code -> onApplyPatch(CanvasSelectionStylePatch(backgroundYabaCode = code)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.FILL_TYPE) && style.backgroundYabaCode > 0) {
            LabeledChipRow(
                label = "Fill pattern",
                mixed = style.mixedFillStyle,
                options =
                    listOf(
                        "solid" to "Solid",
                        "hachure" to "Hachure",
                        "cross-hatch" to "Cross-hatch",
                        "zigzag" to "Zigzag",
                    ),
                selected = style.fillStyleKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(fillStyleKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.STROKE_WIDTH)) {
            LabeledChipRow(
                label = "Stroke width",
                mixed = style.mixedStrokeWidth,
                options =
                    listOf(
                        "thin" to "Thin",
                        "bold" to "Bold",
                        "extraBold" to "Extra",
                    ),
                selected = style.strokeWidthKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(strokeWidthKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.STROKE_STYLE)) {
            LabeledChipRow(
                label = "Stroke style",
                mixed = style.mixedStrokeStyle,
                options =
                    listOf(
                        "solid" to "Solid",
                        "dashed" to "Dashed",
                        "dotted" to "Dotted",
                    ),
                selected = style.strokeStyle,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(strokeStyle = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.SLOPPINESS)) {
            LabeledChipRow(
                label = "Sloppiness",
                mixed = style.mixedRoughness,
                options =
                    listOf(
                        "architect" to "Architect",
                        "artist" to "Artist",
                        "cartoonist" to "Cartoonist",
                    ),
                selected = style.roughnessKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(roughnessKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.EDGES)) {
            LabeledChipRow(
                label = "Edges",
                mixed = style.mixedEdge,
                options =
                    listOf(
                        "sharp" to "Sharp",
                        "round" to "Round",
                    ),
                selected = style.edgeKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(edgeKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.FONT_SIZE)) {
            LabeledChipRow(
                label = "Font size",
                mixed = style.mixedFontSize,
                options =
                    listOf(
                        "S" to "S",
                        "M" to "M",
                        "L" to "L",
                        "XL" to "XL",
                    ),
                selected = style.fontSizeKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(fontSizeKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.ARROW_TYPE)) {
            LabeledChipRow(
                label = "Arrow type",
                mixed = style.mixedArrowType,
                options =
                    listOf(
                        "sharp" to "Sharp",
                        "curved" to "Curved",
                        "elbow" to "Elbow",
                    ),
                selected = style.arrowTypeKey,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(arrowTypeKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.START_ARROWHEAD)) {
            ArrowheadPickerSection(
                label = "Start arrowhead",
                mixed = style.mixedStartArrowhead,
                keys = startKeys,
                selectedKey = style.startArrowheadKey,
                flipPreview = false,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(startArrowheadKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.END_ARROWHEAD)) {
            ArrowheadPickerSection(
                label = "End arrowhead",
                mixed = style.mixedEndArrowhead,
                keys = endKeys,
                selectedKey = style.endArrowheadKey,
                flipPreview = true,
                onSelect = { onApplyPatch(CanvasSelectionStylePatch(endArrowheadKey = it)) },
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.OPACITY)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (style.mixedOpacity) "Opacity (mixed)" else "Opacity",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${style.opacityStep * 10}%",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Slider(
                value = style.opacityStep.toFloat(),
                onValueChange = { v ->
                    val step = v.roundToInt().coerceIn(0, 10)
                    onApplyPatch(CanvasSelectionStylePatch(opacityStep = step))
                },
                valueRange = 0f..10f,
                steps = 9,
                colors =
                    SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
            )
        }

        if (style.hasOptionGroup(CanvasOptionGroups.LAYERS)) {
            Text("Layers", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LayerIconButton(icon = "layer-send-to-back", contentDesc = "Send to back") {
                    onLayer("sendToBack")
                }
                LayerIconButton(icon = "layer-send-backward", contentDesc = "Send backward") {
                    onLayer("sendBackward")
                }
                LayerIconButton(icon = "layer-bring-forward", contentDesc = "Bring forward") {
                    onLayer("bringForward")
                }
                LayerIconButton(icon = "layer-bring-to-front", contentDesc = "Bring to front") {
                    onLayer("bringToFront")
                }
            }
        }

        if (style.hasOptionGroup(CanvasOptionGroups.DELETE)) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onDeleteSelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    YabaIcon(
                        name = "delete-02",
                        color = MaterialTheme.colorScheme.onError,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "Delete selection",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun excalidrawArrowheadLabel(key: String): String =
    when (key) {
        "none" -> "None"
        "arrow" -> "Open arrow"
        "triangle" -> "Triangle"
        "triangle_outline" -> "Triangle outline"
        "dot" -> "Dot"
        "circle" -> "Circle"
        "circle_outline" -> "Circle outline"
        "diamond" -> "Diamond"
        "diamond_outline" -> "Diamond outline"
        "bar" -> "Bar"
        "crowfoot_one" -> "Crow's foot (1)"
        "crowfoot_many" -> "Crow's foot (N)"
        "crowfoot_one_or_many" -> "Crow's foot (1/N)"
        else -> key.replace('_', ' ').replaceFirstChar { c -> c.titlecase() }
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArrowheadPickerSection(
    label: String,
    mixed: Boolean,
    keys: List<String>,
    selectedKey: String,
    flipPreview: Boolean,
    onSelect: (String) -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (mixed) "$label (mixed)" else label,
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            keys.forEach { key ->
                val selected = !mixed && key == selectedKey
                val desc = excalidrawArrowheadLabel(key)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .widthIn(min = 64.dp, max = 80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color =
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                    },
                                shape = RoundedCornerShape(10.dp),
                            )
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                            .clickable { onSelect(key) }
                            .padding(horizontal = 6.dp, vertical = 8.dp)
                            .semantics { contentDescription = desc },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExcalidrawArrowheadPreview(
                            arrowheadKey = key,
                            color = ink,
                            flipHorizontally = flipPreview,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPaletteRow(
    colors: List<YabaColor>,
    selectedCode: Int,
    mixed: Boolean,
    onSelect: (Int) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colors.forEach { yaba ->
            val code = yaba.code
            val selected = !mixed && code == selectedCode
            val borderColor =
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, borderColor, CircleShape)
                        .background(
                            if (yaba == YabaColor.NONE) {
                                Color.Transparent
                            } else {
                                Color(yaba.iconTintArgb())
                            },
                        )
                        .then(
                            if (yaba == YabaColor.NONE) {
                                Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelect(code) },
                contentAlignment = Alignment.Center,
            ) {}
        }
        if (mixed) {
            Text(
                text = "Mixed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabeledChipRow(
    label: String,
    mixed: Boolean,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (mixed) "$label (mixed)" else label,
            style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            options.forEach { (id, text) ->
                val isSelected = !mixed && id == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(id) },
                    label = { Text(text) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LayerIconButton(
    icon: String,
    contentDesc: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = contentDesc },
    ) {
        YabaIcon(
            name = icon,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
