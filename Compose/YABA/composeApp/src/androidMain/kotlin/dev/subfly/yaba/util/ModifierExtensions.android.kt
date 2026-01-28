package dev.subfly.yaba.util

import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.yabaRightClick(
    onRightClick: () -> Unit,
): Modifier {
    // No-op on Android - right click is not supported
    return this
}