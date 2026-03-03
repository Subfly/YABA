package dev.subfly.yaba.util

import androidx.compose.runtime.Composable

/**
 * Returns a function that shares a URL using the platform's native share mechanism.
 *
 * On Android: Opens the system share sheet via Intent.ACTION_SEND.
 * On Desktop: Copies the URL to the clipboard and opens it in the default browser.
 *
 * @return A function that takes a URL string and shares it.
 */
@Composable
expect fun rememberShareHandler(): (String) -> Unit
