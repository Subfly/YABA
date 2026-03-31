package dev.subfly.yaba.util

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberShareHandler(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url: String ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startActivity(
                Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
