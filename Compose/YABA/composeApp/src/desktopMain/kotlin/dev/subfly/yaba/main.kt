package dev.subfly.yaba

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.subfly.yaba.core.app.App
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.util.FileKitHelper

fun main() {
    // Follow system appearance (dark/light mode) on macOS
    System.setProperty("apple.awt.application.appearance", "system")
    FileKitHelper.initialize()
    DatabaseProvider.initialize()

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

            App()
        }
    }
}
