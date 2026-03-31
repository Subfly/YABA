package dev.subfly.yaba.core.util

import android.net.Uri
import dev.subfly.yaba.core.filesystem.access.FileAccessProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bundled assets under `app/src/main/assets/` (HugeIcons metadata JSON and SVG paths).
 */
object BundleReader {

    /**
     * Resolves HugeIcons SVG paths: `assets/files/icons/<name>.svg`.
     * Names may contain spaces; the URI is percent-encoded for Coil.
     */
    fun getIconUri(name: String): String {
        val encoded = Uri.encode(name)
        return "file:///android_asset/files/icons/$encoded.svg"
    }

    /**
     * Reads UTF-8 text (e.g. `files/metadata/icon_categories_header.json`).
     */
    internal suspend fun readAssetText(assetPath: String): String =
        withContext(Dispatchers.IO) {
            val ctx = FileAccessProvider.requireApplicationContext()
            ctx.assets.open(assetPath).use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
        }
}
