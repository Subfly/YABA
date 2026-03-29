package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.net.URI

/**
 * JVM implementation: opens the URL in the default browser via [java.awt.Desktop].
 */
@Composable
actual fun rememberUrlLauncher(): (String) -> Boolean {
    return remember {
        { url: String ->
            try {
                if (Desktop.isDesktopSupported()) {
                    val awtDesktop = Desktop.getDesktop()
                    if (awtDesktop.isSupported(Desktop.Action.BROWSE)) {
                        awtDesktop.browse(URI(url))
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
