package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.yabaRightClick(
    onRightClick: () -> Unit,
): Modifier {
    // No-op on Android — right click is not supported
    return this
}
