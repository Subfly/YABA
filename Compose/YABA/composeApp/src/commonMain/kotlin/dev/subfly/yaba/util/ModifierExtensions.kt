package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.yabaClickable(
    onLongClick: () -> Unit,
    onClick: () -> Unit,
): Modifier
