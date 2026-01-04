package dev.subfly.yaba.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.util.DesktopPlatform
import dev.subfly.yaba.util.YabaDesktopPlatform

@Composable
fun FrameWindowScope.YabaWindowControlBar(
    modifier: Modifier = Modifier,
) {
    YabaTheme {
        WindowDraggableArea {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.background,
            ) {
                if (DesktopPlatform == YabaDesktopPlatform.WINDOWS) {

                } else {

                }
            }
        }
    }
}
