package dev.subfly.yabacore.filesystem.access

import dev.subfly.yabacore.filesystem.settings.FileSystemSettingsStore
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
actual class FileAccessProvider actual constructor(
    settingsStore: FileSystemSettingsStore,
) {
    private val delegate = CommonFileAccessProvider(settingsStore = settingsStore)

    actual suspend fun currentRoot(): PlatformFile = delegate.currentRoot()

    actual suspend fun bookmarkDirectory(
        bookmarkId: Uuid,
        subtypeDirectory: String?,
        ensureExists: Boolean,
    ): PlatformFile = delegate.bookmarkDirectory(bookmarkId, subtypeDirectory, ensureExists)

    actual suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean,
    ): PlatformFile = delegate.resolveRelativePath(relativePath, ensureParentExists)

    actual suspend fun writeBytes(relativePath: String, bytes: ByteArray) =
        delegate.writeBytes(relativePath, bytes)

    actual suspend fun delete(relativePath: String) = delegate.delete(relativePath)

    actual suspend fun deleteBookmarkDirectory(bookmarkId: Uuid) =
        delegate.deleteBookmarkDirectory(bookmarkId)
}
