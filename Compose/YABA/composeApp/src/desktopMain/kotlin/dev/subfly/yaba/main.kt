package dev.subfly.yaba

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.subfly.yaba.core.app.App
import dev.subfly.yaba.ui.YabaWindowControlBar
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.util.FileKitHelper

fun main() = application {
    FileKitHelper.initialize()
    DatabaseProvider.initialize()

    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(width = 1200.dp, height = 1000.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        undecorated = true,
        transparent = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            YabaWindowControlBar(
                state = windowState,
                onClose = ::exitApplication,
            )
            App()
        }
    }
}
