package dev.subfly.yaba.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

@Composable
actual fun rememberShareHandler(): (String) -> Unit {
    return remember {
        { url: String ->
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(url), null)

            if (Desktop.isDesktopSupported()) {
                val awtDesktop = Desktop.getDesktop()
                if (awtDesktop.isSupported(Desktop.Action.BROWSE)) {
                    runCatching { awtDesktop.browse(URI(url)) }
                }
            }
        }
    }
}
