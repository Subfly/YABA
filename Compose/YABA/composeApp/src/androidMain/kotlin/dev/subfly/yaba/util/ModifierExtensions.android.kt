package dev.subfly.yaba.util

import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun Modifier.yabaClickable(
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier {
    return this then Modifier.combinedClickable(
        onLongClick = onLongClick,
        onClick = onClick
    )
}