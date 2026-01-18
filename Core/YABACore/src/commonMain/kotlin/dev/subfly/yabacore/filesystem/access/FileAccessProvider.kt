package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.PlatformFile

expect object FileAccessProvider {
    suspend fun currentRoot(): PlatformFile

    suspend fun bookmarkDirectory(
        bookmarkId: String,
        subtypeDirectory: String? = null,
        ensureExists: Boolean = true,
    ): PlatformFile

    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean = false,
    ): PlatformFile

    suspend fun writeBytes(relativePath: String, bytes: ByteArray)

    suspend fun readBytes(relativePath: String): ByteArray?

    suspend fun readText(relativePath: String): String?

    suspend fun delete(relativePath: String)

    suspend fun deleteBookmarkDirectory(bookmarkId: String)
}

fun createFileAccessProvider(): FileAccessProvider = FileAccessProvider
