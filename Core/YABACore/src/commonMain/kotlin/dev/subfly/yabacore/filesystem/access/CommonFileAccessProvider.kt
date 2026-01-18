package dev.subfly.yabacore.filesystem.access

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.filesystem.settings.FileSystemSettings
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

internal object CommonFileAccessProvider {
    private val settingsStore = FileSystemSettings.store
    suspend fun currentRoot(): PlatformFile {
        val configured = settingsStore
            .getRootPath()
            ?.takeIf { it.isNotBlank() }
            ?.let(::PlatformFile)
            ?.normalizeDirectory()
        val directory = configured ?: createFallbackRoot()
        directory.createDirectories()
        return directory
    }

    suspend fun bookmarkDirectory(
        bookmarkId: String,
        subtypeDirectory: String?,
        ensureExists: Boolean,
    ): PlatformFile {
        val relativePath = CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId, subtypeDirectory)
        val folder = resolveRelativePath(relativePath, ensureParentExists = ensureExists)
        if (ensureExists) {
            folder.createDirectories()
        }
        return folder
    }

    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean,
    ): PlatformFile {
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

    suspend fun deleteBookmarkDirectory(bookmarkId: String) {
        val folder = CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId)
        delete(folder)
    }

    suspend fun readBytes(relativePath: String): ByteArray? {
        val target = resolveRelativePath(relativePath, ensureParentExists = false)
        return if (target.exists()) {
            withContext(Dispatchers.IO) {
                target.readBytes()
            }
        } else null
    }

    suspend fun readText(relativePath: String): String? {
        return readBytes(relativePath)?.decodeToString()
    }

    private fun createFallbackRoot(): PlatformFile =
        (FileKit.filesDir / CoreConstants.FileSystem.ROOT_DIR).also { it.createDirectories() }

    private fun PlatformFile.resolve(relativePath: String): PlatformFile =
        relativePath
            .split('/')
            .filter { it.isNotBlank() }
            .fold(this) { acc, segment ->
                acc / segment
            }

    private fun PlatformFile.normalizeDirectory(): PlatformFile =
        when {
            isDirectory() -> this
            else -> parent()?.normalizeDirectory() ?: this
        }
}
