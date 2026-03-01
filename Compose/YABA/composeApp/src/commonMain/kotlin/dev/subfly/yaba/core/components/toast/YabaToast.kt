package dev.subfly.yaba.core.components.toast

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.toast.ToastIconType
import dev.subfly.yabacore.toast.ToastItem
import dev.subfly.yabacore.ui.icon.YabaIcon
import kotlin.math.roundToInt

@Composable
fun YabaToast(
    toast: ToastItem,
    isMobile: Boolean,
    onDismissRequest: () -> Unit,
    onAcceptRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = toastPalette(iconType = toast.iconType)
    val shape = RoundedCornerShape(16.dp)
    var dragOffset by remember(toast.id) { mutableFloatStateOf(0f) }
    val dismissThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 52.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        val next = dragOffset + delta
        dragOffset = next.coerceAtLeast(0f)
    }

    val dismissModifier = Modifier
        .draggable(
            state = draggableState,
            orientation = if (isMobile) Orientation.Vertical else Orientation.Horizontal,
            onDragStopped = { velocity ->
                val shouldDismiss = dragOffset > dismissThresholdPx || velocity > 1_600f
                if (shouldDismiss) {
                    onDismissRequest()
                }
                dragOffset = 0f
            },
        )
        .offset {
            if (isMobile) {
                IntOffset(x = 0, y = dragOffset.roundToInt())
            } else {
                IntOffset(x = dragOffset.roundToInt(), y = 0)
            }
        }

    Row(
        modifier = modifier
            .then(if (isMobile) Modifier else Modifier.widthIn(max = 400.dp))
            .height(60.dp)
            .shadow(elevation = 12.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(dismissModifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(if (toast.iconType == ToastIconType.NONE) 12.dp else 60.dp)
                .background(palette.accentColor),
            contentAlignment = Alignment.Center,
        ) {
            if (toast.iconType != ToastIconType.NONE) {
                YabaIcon(
                    modifier = Modifier.size(32.dp),
                    name = toast.iconType.iconName,
                    color = palette.iconColor,
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = resolveToastText(toast.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            toast.acceptText?.let { text ->
                onAcceptRequest?.let { action ->
                    TextButton(onClick = action) {
                        Text(
                            text = resolveToastText(text),
                            color = palette.accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun toastPalette(iconType: ToastIconType): ToastPalette {
    val scheme = MaterialTheme.colorScheme
    return when (iconType) {
        ToastIconType.WARNING -> ToastPalette(
            accentColor = Color(0xFFE19A00),
            iconColor = Color.White,
        )

        ToastIconType.SUCCESS -> ToastPalette(
            accentColor = Color(0xFF2E9A54),
            iconColor = Color.White,
        )

        ToastIconType.HINT -> ToastPalette(
            accentColor = scheme.primary,
            iconColor = scheme.onPrimary,
        )

        ToastIconType.ERROR -> ToastPalette(
            accentColor = scheme.error,
            iconColor = scheme.onError,
        )

        ToastIconType.NONE -> ToastPalette(
            accentColor = scheme.primary,
            iconColor = scheme.onPrimary,
        )
    }
}

private data class ToastPalette(
    val accentColor: Color,
    val iconColor: Color,
)
