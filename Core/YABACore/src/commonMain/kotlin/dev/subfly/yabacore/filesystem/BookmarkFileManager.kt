package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * File manager for bookmark assets.
 *
 * Handles reading and writing binary content files within the bookmark directory
 * structure (e.g., preview images, icons, HTML exports).
 */
object BookmarkFileManager {
    private val accessProvider = FileAccessProvider

    /**
     * Recursively deletes the entire bookmark folder and its contents.
     * Call this when deleting a bookmark to remove all assets (readable, images, etc.).
     */
    suspend fun deleteBookmarkFolder(bookmarkId: String) {
        val path = CoreConstants.FileSystem.Linkmark.bookmarkFolder(bookmarkId)
        deleteDirectoryRecursive(path)
    }

    private suspend fun deleteDirectoryRecursive(relativePath: String) {
        val dir = accessProvider.resolveRelativePath(relativePath, ensureParentExists = false)
        if (!dir.exists()) return
        if (dir.isDirectory()) {
            dir.list().forEach { child ->
                val childPath = CoreConstants.FileSystem.join(relativePath, child.name)
                deleteDirectoryRecursive(childPath)
            }
        }
        accessProvider.delete(relativePath)
    }

    suspend fun deleteRelativePath(relativePath: String) {
        accessProvider.delete(relativePath)
    }

    suspend fun resolve(relativePath: String, ensureParentExists: Boolean = false): PlatformFile =
        accessProvider.resolveRelativePath(relativePath, ensureParentExists)

    suspend fun find(relativePath: String): PlatformFile? {
        val file = resolve(relativePath)
        return if (file.exists()) file else null
    }

    /**
     * Returns the absolute path string for a relative path.
     * This is the only way to get path strings - callers should not access [PlatformFile.path] directly.
     */
    suspend fun getAbsolutePath(relativePath: String): String {
        val file = resolve(relativePath)
        return file.path
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
