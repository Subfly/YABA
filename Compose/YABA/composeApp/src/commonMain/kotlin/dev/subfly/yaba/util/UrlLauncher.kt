package dev.subfly.yaba.util

import androidx.compose.runtime.Composable

/**
 * Returns a function that opens URLs in the default browser or appropriate app.
 * 
 * On Android: Attempts to open the URL as a deeplink if an app is installed,
 * otherwise falls back to the default browser.
 * 
 * On Desktop: Opens the URL in the default browser.
 * 
 * @return A function that takes a URL string and returns true if successfully opened
 */
@Composable
expect fun rememberUrlLauncher(): (String) -> Boolean
