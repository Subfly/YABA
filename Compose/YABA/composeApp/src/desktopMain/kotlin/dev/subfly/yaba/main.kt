package dev.subfly.yaba

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.subfly.yaba.core.app.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YABA",
    ) {
        App()
    }
}