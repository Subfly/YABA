package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object LinkmarkFileManager {
    private const val DEFAULT_LINK_IMAGE_EXTENSION = "jpeg"
    private const val DEFAULT_DOMAIN_ICON_EXTENSION = "png"
    private val LINK_IMAGE_EXTENSIONS = listOf("jpeg", "jpg", "png", "webp")
    private val DOMAIN_ICON_EXTENSIONS = listOf("png", "ico", "jpg", "jpeg", "webp")

    suspend fun saveLinkImageBytes(
        bookmarkId: String,
        bytes: ByteArray,
        extension: String = DEFAULT_LINK_IMAGE_EXTENSION,
    ): PlatformFile {
        purgeLinkImages(bookmarkId)
        val targetPath =
            CoreConstants.FileSystem.Linkmark.linkImagePath(
                bookmarkId,
                sanitizeExtension(
                    extension,
                    DEFAULT_LINK_IMAGE_EXTENSION
                ),
            )
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun importLinkImageFromFile(
        bookmarkId: String,
        source: PlatformFile,
    ): PlatformFile {
        purgeLinkImages(bookmarkId)
        val targetPath =
            CoreConstants.FileSystem.Linkmark.linkImagePath(
                bookmarkId,
                sanitizeExtension(
                    source.extension,
                    DEFAULT_LINK_IMAGE_EXTENSION
                ),
            )
        BookmarkFileManager.copyFile(
            source = source,
            destinationRelativePath = targetPath,
            overwrite = true,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun getLinkImageFile(bookmarkId: String): PlatformFile? =
        findExistingAsset(bookmarkId, LINK_IMAGE_EXTENSIONS) { id, ext ->
            CoreConstants.FileSystem.Linkmark.linkImagePath(id, ext)
        }

    suspend fun saveDomainIconBytes(
        bookmarkId: String,
        bytes: ByteArray,
    ): PlatformFile {
        purgeDomainIcons(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Linkmark.domainIconPath(
            bookmarkId,
            DEFAULT_DOMAIN_ICON_EXTENSION,
        )
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun importDomainIconFromFile(
        bookmarkId: String,
        source: PlatformFile,
    ): PlatformFile {
        purgeDomainIcons(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Linkmark.domainIconPath(
            bookmarkId,
            sanitizeExtension(
                source.extension,
                DEFAULT_DOMAIN_ICON_EXTENSION
            ),
        )
        BookmarkFileManager.copyFile(
            source = source,
            destinationRelativePath = targetPath,
            overwrite = true,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun getDomainIconFile(bookmarkId: String): PlatformFile? =
        findExistingAsset(bookmarkId, DOMAIN_ICON_EXTENSIONS) { id, ext ->
            CoreConstants.FileSystem.Linkmark.domainIconPath(id, ext)
        }

    /**
     * Reads the link image bytes for a bookmark if it exists.
     */
    suspend fun readLinkImageBytes(bookmarkId: String): ByteArray? {
        return getLinkImageFile(bookmarkId)?.let { file ->
            withContext(Dispatchers.IO) {
                file.readBytes()
            }
        }
    }

    /**
     * Reads the domain icon bytes for a bookmark if it exists.
     */
    suspend fun readDomainIconBytes(bookmarkId: String): ByteArray? {
        return getDomainIconFile(bookmarkId)?.let { file ->
            withContext(Dispatchers.IO) {
                file.readBytes()
            }
        }
    }

    suspend fun purgeLinkmarkFolder(bookmarkId: String) {
        val relativePath = CoreConstants.FileSystem.Linkmark.bookmarkFolder(bookmarkId)
        BookmarkFileManager.deleteRelativePath(
            relativePath = relativePath,
        )
    }

    private suspend fun purgeLinkImages(bookmarkId: String) {
        LINK_IMAGE_EXTENSIONS.forEach { extension ->
            val path = CoreConstants.FileSystem.Linkmark.linkImagePath(bookmarkId, extension)
            BookmarkFileManager.deleteRelativePath(relativePath = path)
        }
    }

    private suspend fun purgeDomainIcons(bookmarkId: String) {
        DOMAIN_ICON_EXTENSIONS.forEach { extension ->
            val path = CoreConstants.FileSystem.Linkmark.domainIconPath(bookmarkId, extension)
            BookmarkFileManager.deleteRelativePath(relativePath = path)
        }
    }

    private suspend fun findExistingAsset(
        bookmarkId: String,
        extensions: List<String>,
        builder: (String, String) -> String,
    ): PlatformFile? {
        extensions.forEach { extension ->
            val path = builder(bookmarkId, extension)
            val file = BookmarkFileManager.find(path)
            if (file != null) return file
        }
        return null
    }

    private fun sanitizeExtension(
        rawExtension: String?,
        fallback: String,
    ): String = rawExtension.orEmpty().lowercase().removePrefix(".").ifBlank { fallback }
}
