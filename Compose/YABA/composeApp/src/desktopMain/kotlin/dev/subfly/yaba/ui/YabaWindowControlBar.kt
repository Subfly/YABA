package dev.subfly.yaba.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yaba.ui.window.YabaWindowInteractionButton
import dev.subfly.yabacore.model.utils.YabaColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FrameWindowScope.YabaWindowControlBar(
    modifier: Modifier = Modifier,
    state: WindowState,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    YabaTheme {
        WindowDraggableArea {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    TitleArea(
                        onDoubleClick = {
                            val currentPlacement = state.placement
                            when (currentPlacement) {
                                WindowPlacement.Floating -> {
                                    state.placement = WindowPlacement.Maximized
                                }
                                WindowPlacement.Maximized -> {
                                    state.placement = WindowPlacement.Floating
                                }
                                WindowPlacement.Fullscreen -> Unit
                            }
                        }
                    )
                    WindowButtons(
                        isFullScreen = state.placement == WindowPlacement.Fullscreen,
                        onClose = onClose,
                        onMinimize = { state.isMinimized = true },
                        onMaximize = {
                            val currentPlacement = state.placement
                            when (currentPlacement) {
                                WindowPlacement.Floating -> {
                                    scope.launch {
                                        state.placement = WindowPlacement.Maximized
                                        delay(50)
                                        state.placement = WindowPlacement.Fullscreen
                                    }
                                }

                                WindowPlacement.Maximized -> {
                                    state.placement = WindowPlacement.Fullscreen
                                }

                                WindowPlacement.Fullscreen -> {
                                    state.placement = WindowPlacement.Floating
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
private fun BoxScope.TitleArea(
    onDoubleClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .align(Alignment.Center)
            .onClick(
                onClick = {},
                onDoubleClick = onDoubleClick,
            ),
        text = "YABA",
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun BoxScope.WindowButtons(
    isFullScreen: Boolean,
    onClose: () -> Unit,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
) {
    Row(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        YabaWindowInteractionButton(
            iconName = "cancel-01",
            color = YabaColor.RED,
            onClick = onClose,
        )
        YabaWindowInteractionButton(
            iconName = "minus-sign",
            color = YabaColor.YELLOW,
            onClick = onMinimize,
            disabled = isFullScreen,
        )
        YabaWindowInteractionButton(
            iconName = if (isFullScreen) "arrow-shrink-02" else "arrow-diagonal",
            color = YabaColor.GREEN,
            onClick = onMaximize,
        )
    }
}

