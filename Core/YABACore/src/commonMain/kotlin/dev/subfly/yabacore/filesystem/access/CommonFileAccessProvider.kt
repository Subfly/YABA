@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.filesystem.access

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.filesystem.settings.FileSystemSettingsStore
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.write
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

internal class CommonFileAccessProvider(
    private val settingsStore: FileSystemSettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun currentRoot(): PlatformFile {
        val configured =
            settingsStore
                .getRootPath()
                ?.takeIf { it.isNotBlank() }
                ?.let(::PlatformFile)
                ?.normalizeDirectory()
        val directory = configured ?: createFallbackRoot()
        directory.createDirectories()
        return directory
    }

    suspend fun bookmarkDirectory(
        bookmarkId: Uuid,
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
        withContext(ioDispatcher) { target.write(bytes) }
    }

    suspend fun delete(relativePath: String) {
        val target = resolveRelativePath(relativePath, ensureParentExists = false)
        if (target.exists()) {
            withContext(ioDispatcher) { target.delete() }
        }
    }

    suspend fun deleteBookmarkDirectory(bookmarkId: Uuid) {
        val folder = CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId)
        delete(folder)
    }

    private fun createFallbackRoot(): PlatformFile =
        (FileKit.filesDir / CoreConstants.FileSystem.ROOT_DIR).also { it.createDirectories() }

    private fun PlatformFile.resolve(relativePath: String): PlatformFile =
        relativePath.split('/').filter { it.isNotBlank() }.fold(this) { acc, segment ->
            acc / segment
        }

    private fun PlatformFile.normalizeDirectory(): PlatformFile =
        when {
            isDirectory() -> this
            else -> parent()?.normalizeDirectory() ?: this
        }
}
