package dev.subfly.yaba.core.filesystem.access

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.filesystem.YabaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object CommonFileAccessProvider {
    suspend fun currentRoot(): YabaFile {
        val directory = defaultAppRoot()
        directory.createDirectories()
        return directory
    }

    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean,
    ): YabaFile {
        val root = currentRoot()
        val target = root.resolve(relativePath)
        if (ensureParentExists) {
            target.parent()?.createDirectories()
        }
        return target
    }

    suspend fun writeBytes(relativePath: String, bytes: ByteArray) {
        val target = resolveRelativePath(relativePath, ensureParentExists = true)
        withContext(Dispatchers.IO) { target.write(bytes) }
    }

    suspend fun delete(relativePath: String) {
        val target = resolveRelativePath(relativePath, ensureParentExists = false)
        if (target.exists()) {
            withContext(Dispatchers.IO) { target.delete() }
        }
    }

    suspend fun readBytes(relativePath: String): ByteArray? {
        val target = resolveRelativePath(relativePath, ensureParentExists = false)
        return if (target.exists()) {
            withContext(Dispatchers.IO) {
                target.readBytes()
            }
        } else {
            null
        }
    }

    suspend fun readText(relativePath: String): String? {
        return readBytes(relativePath)?.decodeToString()
    }

    /** App-private root: `filesDir` + [CoreConstants.FileSystem.ROOT_DIR] (not user-configurable). */
    private fun defaultAppRoot(): YabaFile {
        val ctx = FileAccessProvider.requireApplicationContext()
        val dir = File(ctx.filesDir, CoreConstants.FileSystem.ROOT_DIR).also { it.mkdirs() }
        return YabaFile.fromFile(ctx, dir)
    }

    private fun YabaFile.resolve(relativePath: String): YabaFile =
        relativePath
            .split('/')
            .filter { it.isNotBlank() }
            .fold(this) { acc, segment ->
                acc / segment
            }
}
