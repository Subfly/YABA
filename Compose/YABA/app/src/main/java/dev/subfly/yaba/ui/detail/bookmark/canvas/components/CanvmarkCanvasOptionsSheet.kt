package dev.subfly.yaba.ui.detail.bookmark.canvas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.webview.CanvasHostStyleState
import dev.subfly.yaba.core.webview.CanvasSelectionStylePatch
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4F)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Stroke", style = MaterialTheme.typography.labelLarge)
        ColorPaletteRow(
            colors = YabaColor.entries.filter { it != YabaColor.NONE },
            selectedCode = style.strokeYabaCode,
            mixed = style.mixedStroke,
            onSelect = { code -> onApplyPatch(CanvasSelectionStylePatch(strokeYabaCode = code)) },
        )

        Text("Fill", style = MaterialTheme.typography.labelLarge)
        ColorPaletteRow(
            colors = YabaColor.entries,
            selectedCode = style.backgroundYabaCode,
            mixed = style.mixedBackground,
            onSelect = { code -> onApplyPatch(CanvasSelectionStylePatch(backgroundYabaCode = code)) },
        )

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

        Text("Layer", style = MaterialTheme.typography.labelLarge)
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

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                    .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(onClick = onDeleteSelected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YabaIcon(
                        name = "delete-02",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Delete selection",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

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
                                    CircleShape
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
