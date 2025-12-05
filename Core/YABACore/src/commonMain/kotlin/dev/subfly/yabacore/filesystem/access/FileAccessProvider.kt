package dev.subfly.yabacore.filesystem.access

import dev.subfly.yabacore.filesystem.settings.FileSystemSettings
import dev.subfly.yabacore.filesystem.settings.FileSystemSettingsStore
import io.github.vinceglb.filekit.PlatformFile
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
expect class FileAccessProvider
internal constructor(
    settingsStore: FileSystemSettingsStore,
) {
    suspend fun currentRoot(): PlatformFile

    suspend fun bookmarkDirectory(
        bookmarkId: Uuid,
        subtypeDirectory: String? = null,
        ensureExists: Boolean = true,
    ): PlatformFile

    suspend fun resolveRelativePath(
        relativePath: String,
        ensureParentExists: Boolean = false,
    ): PlatformFile

    suspend fun writeBytes(relativePath: String, bytes: ByteArray)

    suspend fun delete(relativePath: String)

    suspend fun deleteBookmarkDirectory(bookmarkId: Uuid)
}

fun createFileAccessProvider(
    settingsStore: FileSystemSettingsStore = FileSystemSettings.store,
): FileAccessProvider = FileAccessProvider(settingsStore)
