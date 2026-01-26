package dev.subfly.yabacore.filesystem.access

import io.github.vinceglb.filekit.PlatformFile

expect object FileAccessProvider {
    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean = false,
    ): PlatformFile

    suspend fun writeBytes(relativePath: String, bytes: ByteArray)

    suspend fun readText(relativePath: String): String?

    suspend fun delete(relativePath: String)
}
