package dev.subfly.yabacore.util

import android.content.Context
import androidx.activity.ComponentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.manualFileKitCoreInitialization

actual object FileKitHelper {
    actual fun init(platformContext: Any?) {
        val context = platformContext as? Context ?: return
        FileKit.manualFileKitCoreInitialization(context)
        (platformContext as? ComponentActivity)?.let { activity ->
            FileKit.init(activity)
        }
    }
}
