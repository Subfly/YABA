package dev.subfly.yaba.util

import androidx.compose.runtime.Composable

/**
 * Returns a function that shares a URL using the platform's native share mechanism.
 *
 * Opens the system share sheet via Intent.ACTION_SEND.
 *
 * @return A function that takes a URL string and shares it.
 */
@Composable
expect fun rememberShareHandler(): (String) -> Unit
