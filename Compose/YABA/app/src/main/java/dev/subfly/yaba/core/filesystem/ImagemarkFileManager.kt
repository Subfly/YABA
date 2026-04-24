package dev.subfly.yaba.core.filesystem

import dev.subfly.yaba.core.common.CoreConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * File manager for Imagemark assets.
 *
 * - **Preview** (`image.<ext>`): compressed, used for cards/lists.
 * - **Original** (`image_original.<ext>`): full resolution for image bookmarks only; used for share/detail.
 */
object ImagemarkFileManager {
    private const val DEFAULT_IMAGE_EXTENSION = "jpeg"
    private val IMAGE_EXTENSIONS = listOf("jpeg", "jpg", "png", "webp", "gif")

    /** Saves the compressed preview; removes prior preview files only. */
    suspend fun saveImageBytes(
        bookmarkId: String,
        bytes: ByteArray,
        extension: String = DEFAULT_IMAGE_EXTENSION,
    ): YabaFile {
        purgePreviewImages(bookmarkId)
        val ext = sanitizeExtension(extension, DEFAULT_IMAGE_EXTENSION)
        val targetPath = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    /** Full-resolution copy for image bookmarks; removes prior original files only. */
    suspend fun saveOriginalImageBytes(
        bookmarkId: String,
        bytes: ByteArray,
        extension: String = DEFAULT_IMAGE_EXTENSION,
    ): YabaFile {
        purgeOriginalImages(bookmarkId)
        val ext = sanitizeExtension(extension, DEFAULT_IMAGE_EXTENSION)
        val targetPath = CoreConstants.FileSystem.Imagemark.imageOriginalPath(bookmarkId, ext)
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun importImageFromFile(
        bookmarkId: String,
        source: YabaFile,
    ): YabaFile {
        purgePreviewImages(bookmarkId)
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

    /** The compressed preview on disk, if any. */
    suspend fun getImageFile(bookmarkId: String): YabaFile? =
        findExistingPreviewImage(bookmarkId)

    /** File used for share/export: original when present, else legacy single preview file. */
    suspend fun getShareableImageFile(
        bookmarkId: String,
        originalRelativePath: String?,
    ): YabaFile? {
        if (originalRelativePath != null) {
            BookmarkFileManager.find(originalRelativePath)?.let { return it }
        }
        return findExistingOriginalImage(bookmarkId) ?: findExistingPreviewImage(bookmarkId)
    }

    /**
     * Full-resolution bytes for edit UI / hydration (prefers on-disk original, then legacy single file).
     */
    suspend fun readImageBytes(
        bookmarkId: String,
        originalRelativePath: String?,
    ): ByteArray? {
        val fromRel = originalRelativePath?.let { rel ->
            BookmarkFileManager.find(rel)?.let { f ->
                withContext(Dispatchers.IO) { f.readBytes() }
            }
        }
        if (fromRel != null) return fromRel
        val original = findExistingOriginalImage(bookmarkId)
        if (original != null) {
            return withContext(Dispatchers.IO) { original.readBytes() }
        }
        return getImageFile(bookmarkId)?.let { file ->
            withContext(Dispatchers.IO) { file.readBytes() }
        }
    }

    suspend fun getImageRelativePath(bookmarkId: String): String? {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            if (BookmarkFileManager.find(path) != null) return path
        }
        return null
    }

    /**
     * Relative path for full-resolution display: DB hint, else `image_original`, else legacy `image`.
     */
    suspend fun getDetailImageRelativePath(
        bookmarkId: String,
        originalFromEntity: String?,
    ): String? {
        if (originalFromEntity != null && BookmarkFileManager.find(originalFromEntity) != null) {
            return originalFromEntity
        }
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imageOriginalPath(bookmarkId, ext)
            if (BookmarkFileManager.find(path) != null) return path
        }
        return getImageRelativePath(bookmarkId)
    }

    private suspend fun findExistingPreviewImage(bookmarkId: String): YabaFile? {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            val file = BookmarkFileManager.find(path)
            if (file != null) return file
        }
        return null
    }

    private suspend fun findExistingOriginalImage(bookmarkId: String): YabaFile? {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imageOriginalPath(bookmarkId, ext)
            val file = BookmarkFileManager.find(path)
            if (file != null) return file
        }
        return null
    }

    private suspend fun purgePreviewImages(bookmarkId: String) {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imagePath(bookmarkId, ext)
            BookmarkFileManager.deleteRelativePath(relativePath = path)
        }
    }

    private suspend fun purgeOriginalImages(bookmarkId: String) {
        IMAGE_EXTENSIONS.forEach { ext ->
            val path = CoreConstants.FileSystem.Imagemark.imageOriginalPath(bookmarkId, ext)
            BookmarkFileManager.deleteRelativePath(relativePath = path)
        }
    }

    private fun sanitizeExtension(
        rawExtension: String?,
        fallback: String,
    ): String = rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { fallback }
}
