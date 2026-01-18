package dev.subfly.yaba

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.onClick
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.subfly.yaba.core.app.App
import dev.subfly.yaba.core.theme.YabaTheme
import dev.subfly.yabacore.CoreRuntime
import dev.subfly.yabacore.util.FileKitHelper

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")
    FileKitHelper.initialize()
    CoreRuntime.initialize()

    application {
        val windowState = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(width = 1200.dp, height = 1000.dp)
        )

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = ""
        ) {
            // Configure macOS title bar to blend with app content
            LaunchedEffect(Unit) {
                window.rootPane.putClientProperty("apple.awt.fullWindowContent", true)
                window.rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
                window.rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }

            Column {
                TitleBarSpacer(state = windowState)
                App()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleBarSpacer(state: WindowState) {
    YabaTheme {
        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colorScheme.background)
                .onClick(
                    onClick = {},
                    onDoubleClick = {
                        val currentPlacement = state.placement
                        when (currentPlacement) {
                            WindowPlacement.Floating -> {
                                state.placement = WindowPlacement.Maximized
                            }

                            WindowPlacement.Maximized -> {
                                state.placement = WindowPlacement.Floating
                            }

                            else -> Unit
                        }
                    }
                )
        )
    }
}
