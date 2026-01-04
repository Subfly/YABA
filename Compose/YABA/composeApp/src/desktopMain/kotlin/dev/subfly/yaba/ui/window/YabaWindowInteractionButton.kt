package dev.subfly.yaba.ui.window

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.icon.iconTintArgb

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun YabaWindowInteractionButton(
    iconName: String,
    color: YabaColor,
    onClick: () -> Unit,
    disabled: Boolean = false,
) {
    var active by remember { mutableStateOf(false) }
    val animatedBG by animateColorAsState(
        targetValue = if (disabled) {
            if (active) Color(YabaColor.GRAY.iconTintArgb()) else Color.Transparent
        } else {
            if (active) Color(color.iconTintArgb()) else Color.Transparent
        }
    )
    val animatedFG by animateColorAsState(
        targetValue = if (disabled) {
            Color.Gray
        } else {
            if (active) Color.Black else Color.White
        }

    )

    Surface(
        modifier = Modifier
            .size(20.dp)
            .onPointerEvent(eventType = PointerEventType.Enter) { active = true }
            .onPointerEvent(eventType = PointerEventType.Exit) { active = false }
            .onClick(enabled = disabled.not(), onClick = onClick),
        shape = CircleShape,
        color = animatedBG,
    ) {
        YabaIcon(
            Modifier.padding(4.dp),
            name = iconName,
            color = animatedFG,
        )
    }
}
