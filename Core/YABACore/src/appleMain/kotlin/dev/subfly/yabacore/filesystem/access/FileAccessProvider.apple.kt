package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.PlatformFile

actual object FileAccessProvider {
    private val delegate = CommonFileAccessProvider

    actual suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean,
    ): PlatformFile = delegate.resolveRelativePath(relativePath, ensureParentExists)

    actual suspend fun writeBytes(relativePath: String, bytes: ByteArray) =
        delegate.writeBytes(relativePath, bytes)

    actual suspend fun readText(relativePath: String): String? =
        delegate.readText(relativePath)

    actual suspend fun delete(relativePath: String) = delegate.delete(relativePath)
}
