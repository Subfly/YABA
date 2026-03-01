package dev.subfly.yaba.core.components.toast

import androidx.compose.runtime.Composable

@Composable
expect fun ToastOverlayWindow(content: @Composable () -> Unit)
