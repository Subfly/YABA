package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/** Default empty rich-text document JSON (`doc` with empty `content`). */
const val EMPTY_EDITOR_DOCUMENT_JSON = """{"type":"doc","content":[]}"""

object NotemarkFileManager {
    fun documentBodyRelativePath(bookmarkId: String): String =
        CoreConstants.FileSystem.Notemark.documentBodyPath(bookmarkId)

    suspend fun ensureEmptyDocumentBody(bookmarkId: String) {
        val relativePath = documentBodyRelativePath(bookmarkId)
        val file = BookmarkFileManager.resolve(relativePath)
        if (!file.exists()) {
            BookmarkFileManager.writeBytes(relativePath, EMPTY_EDITOR_DOCUMENT_JSON.encodeToByteArray())
        }
    }

    suspend fun writeDocumentBody(
        bookmarkId: String,
        documentJson: String,
    ) {
        val relativePath = documentBodyRelativePath(bookmarkId)
        BookmarkFileManager.writeBytes(relativePath, documentJson.encodeToByteArray())
    }

    suspend fun readDocumentBody(bookmarkId: String): String? {
        val relativePath = documentBodyRelativePath(bookmarkId)
        val file = BookmarkFileManager.find(relativePath) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes().decodeToString()
        }
    }

    suspend fun readDocumentByRelativePath(relativePath: String): String? {
        val file = BookmarkFileManager.find(relativePath) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes().decodeToString()
        }
    }

    suspend fun getDocumentBodyFile(bookmarkId: String): PlatformFile? =
        BookmarkFileManager.find(documentBodyRelativePath(bookmarkId))
}
