package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object DocmarkFileManager {
    private const val DEFAULT_PDF_EXTENSION = "pdf"

    suspend fun savePdfBytes(
        bookmarkId: String,
        bytes: ByteArray,
    ): PlatformFile {
        purgePdf(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Docmark.pdfPath(
            bookmarkId = bookmarkId,
            extension = DEFAULT_PDF_EXTENSION,
        )
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun importPdfFromFile(
        bookmarkId: String,
        source: PlatformFile,
    ): PlatformFile {
        purgePdf(bookmarkId)
        val targetPath = CoreConstants.FileSystem.Docmark.pdfPath(
            bookmarkId = bookmarkId,
            extension = DEFAULT_PDF_EXTENSION,
        )
        BookmarkFileManager.copyFile(
            source = source,
            destinationRelativePath = targetPath,
            overwrite = true,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun getPdfFile(bookmarkId: String): PlatformFile? =
        BookmarkFileManager.find(getPdfRelativePath(bookmarkId))

    suspend fun getPdfRelativePath(bookmarkId: String): String =
        CoreConstants.FileSystem.Docmark.pdfPath(
            bookmarkId = bookmarkId,
            extension = DEFAULT_PDF_EXTENSION,
        )

    suspend fun readPdfBytes(bookmarkId: String): ByteArray? {
        val file = getPdfFile(bookmarkId) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes()
        }
    }

    private suspend fun purgePdf(bookmarkId: String) {
        BookmarkFileManager.deleteRelativePath(getPdfRelativePath(bookmarkId))
    }
}
