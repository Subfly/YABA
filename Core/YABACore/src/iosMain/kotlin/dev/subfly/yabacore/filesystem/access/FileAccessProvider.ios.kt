package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.PlatformFile

actual object FileAccessProvider {
    private val delegate = CommonFileAccessProvider

    actual suspend fun currentRoot(): PlatformFile = delegate.currentRoot()

    actual suspend fun bookmarkDirectory(
        bookmarkId: String,
        subtypeDirectory: String?,
        ensureExists: Boolean,
    ): PlatformFile = delegate.bookmarkDirectory(bookmarkId, subtypeDirectory, ensureExists)

    actual suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean,
    ): PlatformFile = delegate.resolveRelativePath(relativePath, ensureParentExists)

    actual suspend fun writeBytes(relativePath: String, bytes: ByteArray) =
        delegate.writeBytes(relativePath, bytes)

    actual suspend fun readBytes(relativePath: String): ByteArray? =
        delegate.readBytes(relativePath)

    actual suspend fun readText(relativePath: String): String? =
        delegate.readText(relativePath)

    actual suspend fun delete(relativePath: String) = delegate.delete(relativePath)

    actual suspend fun deleteBookmarkDirectory(bookmarkId: String) =
        delegate.deleteBookmarkDirectory(bookmarkId)
}
