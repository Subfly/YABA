package dev.subfly.yaba.util

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Returns a function that opens URLs in the default browser or appropriate app.
 */
@Composable
fun rememberUrlLauncher(): (String) -> Boolean {
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
