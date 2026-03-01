package dev.subfly.yaba.core.components.toast

import androidx.compose.runtime.Composable

@Composable
actual fun ToastOverlayWindow(content: @Composable () -> Unit) {
    content()
}
