@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.operations.FileOperationChange
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperationDraft
import dev.subfly.yabacore.filesystem.access.FileAccessProvider
import dev.subfly.yabacore.filesystem.crypto.computeSha256Hex
import dev.subfly.yabacore.filesystem.model.BookmarkFileAssetKind
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object BookmarkFileManager {
    private val accessProvider = FileAccessProvider
    private val clock: Clock = Clock.System

    suspend fun ensureBookmarkFolder(
        bookmarkId: Uuid,
        subtypeDirectory: String? = null,
    ): PlatformFile =
        accessProvider.bookmarkDirectory(
            bookmarkId = bookmarkId,
            subtypeDirectory = subtypeDirectory,
            ensureExists = true,
        )

    suspend fun deleteBookmarkTree(bookmarkId: Uuid) {
        accessProvider.deleteBookmarkDirectory(bookmarkId)
        logFileChange(
            kind = OperationKind.DELETE,
            relativePath = CoreConstants.FileSystem.bookmarkFolderPath(bookmarkId),
            assetKind = BookmarkFileAssetKind.UNKNOWN,
            sizeBytes = -1,
            checksum = "",
        )
    }

    suspend fun deleteRelativePath(
        relativePath: String,
        assetKind: BookmarkFileAssetKind = BookmarkFileAssetKind.UNKNOWN,
    ) {
        val existing = find(relativePath)
        accessProvider.delete(relativePath)
        val sizeBytes = existing?.takeUnless {
            it.isDirectory()
        }?.size() ?: -1
        logFileChange(
            kind = OperationKind.DELETE,
            relativePath = relativePath,
            assetKind = assetKind,
            sizeBytes = sizeBytes,
            checksum = "",
        )
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
        assetKind: BookmarkFileAssetKind = BookmarkFileAssetKind.UNKNOWN,
    ) {
        val existed = find(relativePath) != null
        accessProvider.writeBytes(relativePath, bytes)
        logFileChange(
            kind = if (existed) OperationKind.UPDATE else OperationKind.CREATE,
            relativePath = relativePath,
            assetKind = assetKind,
            sizeBytes = bytes.size.toLong(),
            checksum = computeSha256Hex(bytes),
        )
    }

    suspend fun copyFile(
        source: PlatformFile,
        destinationRelativePath: String,
        overwrite: Boolean = true,
        assetKind: BookmarkFileAssetKind = BookmarkFileAssetKind.UNKNOWN,
    ) {
        if (!overwrite && find(destinationRelativePath) != null) {
            return
        }
        val bytes = withContext(Dispatchers.IO) { source.readBytes() }
        writeBytes(destinationRelativePath, bytes, assetKind)
    }

    private suspend fun logFileChange(
        kind: OperationKind,
        relativePath: String,
        assetKind: BookmarkFileAssetKind,
        sizeBytes: Long,
        checksum: String,
    ) {
        val change = createFileOperationChange(
            relativePath = relativePath,
            assetKind = assetKind,
            sizeBytes = sizeBytes,
            checksum = checksum,
        ) ?: return
        OpApplier.applyLocal(listOf(change.toOperationDraft(kind, clock.now())))
    }

    private fun createFileOperationChange(
        relativePath: String,
        assetKind: BookmarkFileAssetKind,
        sizeBytes: Long,
        checksum: String,
    ): FileOperationChange? {
        val bookmarkId = extractBookmarkId(relativePath) ?: return null
        return FileOperationChange(
            bookmarkId = bookmarkId,
            relativePath = relativePath,
            assetKind = assetKind,
            sizeBytes = sizeBytes,
            checksum = checksum,
        )
    }

    private fun extractBookmarkId(relativePath: String): Uuid? {
        val segments = relativePath.split('/').filter { it.isNotBlank() }
        val bookmarksIndex = segments.indexOf(CoreConstants.FileSystem.BOOKMARKS_DIR)
        val idSegment = segments.getOrNull(bookmarksIndex + 1) ?: return null
        return runCatching { Uuid.parse(idSegment) }.getOrNull()
    }
}
