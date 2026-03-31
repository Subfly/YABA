package dev.subfly.yaba.core.filesystem.access

import android.content.Context
import dev.subfly.yaba.core.filesystem.YabaFile
import kotlin.concurrent.Volatile

/**
 * Entry point for app-private file storage under the configured YABA root.
 *
 * Call [initialize] once (e.g. from [android.app.Application] or the launcher [android.app.Activity])
 * before any suspend APIs on this object are used.
 */
object FileAccessProvider {
    @Volatile
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    internal fun requireApplicationContext(): Context =
        applicationContext ?: error("FileAccessProvider.initialize() must be called before use")

    private val delegate = CommonFileAccessProvider

    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean = false,
    ): YabaFile = delegate.resolveRelativePath(relativePath, ensureParentExists)

    suspend fun writeBytes(relativePath: String, bytes: ByteArray) =
        delegate.writeBytes(relativePath, bytes)

    suspend fun readText(relativePath: String): String? =
        delegate.readText(relativePath)

    suspend fun delete(relativePath: String) = delegate.delete(relativePath)
}
