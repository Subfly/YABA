package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@Composable
expect fun Modifier.yabaRightClick(
    onRightClick: () -> Unit,
): Modifier

@Composable
fun Modifier.yabaPointerEventSpy(
    onInteraction: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(
                pass = PointerEventPass.Initial,
            )
            if (event.type in setOf(PointerEventType.Move, PointerEventType.Scroll)) {
                onInteraction()
            }
        }
    }
}
