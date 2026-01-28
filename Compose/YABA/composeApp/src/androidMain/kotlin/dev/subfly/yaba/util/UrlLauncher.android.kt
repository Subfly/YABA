package dev.subfly.yaba.util

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Android implementation of URL launcher.
 * Uses Intent.ACTION_VIEW which will:
 * - Open the app if it's installed and handles the URL scheme (deeplink)
 * - Fall back to the default browser if no app handles it
 */
@Composable
actual fun rememberUrlLauncher(): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context) {
        { url: String ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
