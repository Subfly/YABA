package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * File manager for Imagemark assets.
 *
 * Stores a single image file per bookmark under `bookmarks/<bookmarkId>/image.<ext>`.
 * Imagemarks do not use localIconPath; the main image is the primary asset.
 */
object ImagemarkFileManager {
    private const val DEFAULT_IMAGE_EXTENSION = "jpeg"
    private val IMAGE_EXTENSIONS = listOf("jpeg", "jpg", "png", "webp", "gif")

    suspend fun saveImageBytes(
        bookmarkId: String,
        bytes: ByteArray,
        extension: String = DEFAULT_IMAGE_EXTENSION,
    ): PlatformFile {
        purgeImages(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Imagemark.imagePath(
            bookmarkId,
            sanitizeExtension(extension, DEFAULT_IMAGE_EXTENSION),
        )
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun importImageFromFile(
        bookmarkId: String,
        source: PlatformFile,
    ): PlatformFile {
        purgeImages(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Imagemark.imagePath(
            bookmarkId,
            sanitizeExtension(source.extension, DEFAULT_IMAGE_EXTENSION),
        )
        BookmarkFileManager.copyFile(
            source = source,
            destinationRelativePath = targetPath,
            overwrite = true,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun getImageFile(bookmarkId: String): PlatformFile? =
        findExistingImage(bookmarkId)

    suspend fun readImageBytes(bookmarkId: String): ByteArray? {
        return getImageFile(bookmarkId)?.let { file ->
            withContext(Dispatchers.IO) {
                file.readBytes()
            }
        }
    }

    suspend fun getImageRelativePath(bookmarkId: String): String? {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            if (BookmarkFileManager.find(path) != null) return path
        }
        return null
    }

    private suspend fun findExistingImage(bookmarkId: String): PlatformFile? {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            val file = BookmarkFileManager.find(path)
            if (file != null) return file
        }
        return null
    }

    private suspend fun purgeImages(bookmarkId: String) {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            BookmarkFileManager.deleteRelativePath(relativePath = path)
        }
    }

    private fun sanitizeExtension(
        rawExtension: String?,
        fallback: String,
    ): String = rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { fallback }
}
