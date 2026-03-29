package dev.subfly.yaba.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.onClick
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.yabaRightClick(
    onRightClick: () -> Unit,
): Modifier {
    return this then Modifier.onClick(
        matcher = PointerMatcher.mouse(PointerButton.Secondary),
        onClick = onRightClick,
    )
}
