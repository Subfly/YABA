package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.model.utils.DocmarkType
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object DocmarkFileManager {
    fun extensionForType(type: DocmarkType): String =
        when (type) {
            DocmarkType.PDF -> "pdf"
            DocmarkType.EPUB -> "epub"
        }

    suspend fun saveDocumentBytes(
        bookmarkId: String,
        bytes: ByteArray,
        type: DocmarkType,
    ): PlatformFile {
        purgeAllDocumentFiles(bookmarkId)
        val targetPath = getDocumentRelativePath(bookmarkId, type)
        BookmarkFileManager.writeBytes(
            relativePath = targetPath,
            bytes = bytes,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun savePdfBytes(
        bookmarkId: String,
        bytes: ByteArray,
    ): PlatformFile = saveDocumentBytes(bookmarkId, bytes, DocmarkType.PDF)

    suspend fun importPdfFromFile(
        bookmarkId: String,
        source: PlatformFile,
    ): PlatformFile {
        purgeAllDocumentFiles(bookmarkId)
        val targetPath = getDocumentRelativePath(bookmarkId, DocmarkType.PDF)
        BookmarkFileManager.copyFile(
            source = source,
            destinationRelativePath = targetPath,
            overwrite = true,
        )
        return BookmarkFileManager.resolve(targetPath)
    }

    suspend fun getDocumentFile(bookmarkId: String, type: DocmarkType): PlatformFile? =
        BookmarkFileManager.find(getDocumentRelativePath(bookmarkId, type))

    suspend fun getPdfFile(bookmarkId: String): PlatformFile? =
        getDocumentFile(bookmarkId, DocmarkType.PDF)

    fun getDocumentRelativePath(bookmarkId: String, type: DocmarkType): String =
        CoreConstants.FileSystem.Docmark.documentPath(
            bookmarkId = bookmarkId,
            extension = extensionForType(type),
        )

    fun getPdfRelativePath(bookmarkId: String): String =
        getDocumentRelativePath(bookmarkId, DocmarkType.PDF)

    suspend fun readDocumentBytes(bookmarkId: String, type: DocmarkType): ByteArray? {
        val file = getDocumentFile(bookmarkId, type) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes()
        }
    }

    suspend fun readPdfBytes(bookmarkId: String): ByteArray? =
        readDocumentBytes(bookmarkId, DocmarkType.PDF)

    private suspend fun purgeAllDocumentFiles(bookmarkId: String) {
        BookmarkFileManager.deleteRelativePath(getDocumentRelativePath(bookmarkId, DocmarkType.PDF))
        BookmarkFileManager.deleteRelativePath(getDocumentRelativePath(bookmarkId, DocmarkType.EPUB))
    }
}
