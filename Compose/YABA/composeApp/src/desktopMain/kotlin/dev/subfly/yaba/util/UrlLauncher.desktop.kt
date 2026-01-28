package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

/**
 * Desktop implementation of URL launcher.
 * Opens the URL in the default browser using java.awt.Desktop.
 */
@Composable
actual fun rememberUrlLauncher(): (String) -> Boolean {
    return remember {
        { url: String ->
            try {
                if (Desktop.isDesktopSupported()) {
                    val desktop = Desktop.getDesktop()
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(URI(url))
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}
