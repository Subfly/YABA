package dev.subfly.yabacore.filesystem

import dev.subfly.yabacore.common.CoreConstants
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

object NotemarkFileManager {
    fun markdownBodyRelativePath(bookmarkId: String): String =
        CoreConstants.FileSystem.Notemark.markdownBodyPath(bookmarkId)

    suspend fun ensureEmptyMarkdownBody(bookmarkId: String) {
        val relativePath = markdownBodyRelativePath(bookmarkId)
        val file = BookmarkFileManager.resolve(relativePath)
        if (!file.exists()) {
            BookmarkFileManager.writeBytes(relativePath, ByteArray(0))
        }
    }

    suspend fun writeMarkdownBody(
        bookmarkId: String,
        markdown: String,
    ) {
        val relativePath = markdownBodyRelativePath(bookmarkId)
        BookmarkFileManager.writeBytes(relativePath, markdown.encodeToByteArray())
    }

    suspend fun readMarkdownBody(bookmarkId: String): String? {
        val relativePath = markdownBodyRelativePath(bookmarkId)
        val file = BookmarkFileManager.find(relativePath) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes().decodeToString()
        }
    }

    suspend fun readMarkdownByRelativePath(relativePath: String): String? {
        val file = BookmarkFileManager.find(relativePath) ?: return null
        return withContext(Dispatchers.IO) {
            file.readBytes().decodeToString()
        }
    }

    suspend fun getMarkdownBodyFile(bookmarkId: String): PlatformFile? =
        BookmarkFileManager.find(markdownBodyRelativePath(bookmarkId))
}
