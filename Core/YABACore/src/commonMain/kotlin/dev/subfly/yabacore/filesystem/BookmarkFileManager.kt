@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi

/**
 * File manager for bookmark assets.
 *
 * Handles reading and writing binary content files within the bookmark directory
 * structure (e.g., preview images, icons, HTML exports).
 *
 * Note: Metadata operations are handled by [EntityFileManager].
 */
object BookmarkFileManager {
    private val accessProvider = FileAccessProvider

    suspend fun deleteRelativePath(relativePath: String) {
        accessProvider.delete(relativePath)
    }

    suspend fun resolve(relativePath: String, ensureParentExists: Boolean = false): PlatformFile =
        accessProvider.resolveRelativePath(relativePath, ensureParentExists)

    suspend fun find(relativePath: String): PlatformFile? {
        val file = resolve(relativePath)
        return if (file.exists()) file else null
    }

    suspend fun writeBytes(
        relativePath: String,
        bytes: ByteArray,
    ) {
        accessProvider.writeBytes(relativePath, bytes)
    }

    suspend fun copyFile(
        source: PlatformFile,
        destinationRelativePath: String,
        overwrite: Boolean = true,
    ) {
        if (!overwrite && find(destinationRelativePath) != null) {
            return
        }
        val bytes = withContext(Dispatchers.IO) { source.readBytes() }
        writeBytes(destinationRelativePath, bytes)
    }
}
