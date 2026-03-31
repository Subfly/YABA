package dev.subfly.yaba.core.managers

import dev.subfly.yaba.core.common.CoreConstants
import dev.subfly.yaba.core.common.IdGenerator
import dev.subfly.yaba.core.database.DatabaseProvider
import dev.subfly.yaba.core.database.entities.NoteBookmarkEntity
import dev.subfly.yaba.core.database.mappers.toUiModel
import dev.subfly.yaba.core.filesystem.BookmarkFileManager
import dev.subfly.yaba.core.filesystem.EMPTY_EDITOR_DOCUMENT_JSON
import dev.subfly.yaba.core.filesystem.NotemarkFileManager
import dev.subfly.yaba.core.model.ui.NotemarkUiModel
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.queue.CoreOperationQueue
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

object NotemarkManager {
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val noteBookmarkDao
        get() = DatabaseProvider.noteBookmarkDao
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao

    suspend fun getNotemarkDetail(bookmarkId: String): NotemarkUiModel? {
        val bookmarkMetaData = bookmarkDao.getById(bookmarkId) ?: return null
        if (bookmarkMetaData.kind != BookmarkKind.NOTE) return null

        val noteMeta = noteBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val folder = folderDao.getFolderWithBookmarkCount(bookmarkMetaData.folderId)?.toUiModel()
        val tags = tagDao.getTagsForBookmarkWithCounts(bookmarkId).map { it.toUiModel() }

        val localImageAbsolutePath = bookmarkMetaData.localImagePath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }
        val localIconAbsolutePath = bookmarkMetaData.localIconPath?.let { relativePath ->
            BookmarkFileManager.getAbsolutePath(relativePath)
        }

        return NotemarkUiModel(
            id = bookmarkMetaData.id,
            folderId = bookmarkMetaData.folderId,
            kind = bookmarkMetaData.kind,
            label = bookmarkMetaData.label,
            description = bookmarkMetaData.description,
            createdAt = Instant.fromEpochMilliseconds(bookmarkMetaData.createdAt),
            editedAt = Instant.fromEpochMilliseconds(bookmarkMetaData.editedAt),
            viewCount = bookmarkMetaData.viewCount,
            isPrivate = bookmarkMetaData.isPrivate,
            isPinned = bookmarkMetaData.isPinned,
            documentRelativePath = noteMeta.documentRelativePath,
            readableVersionId = noteMeta.readableVersionId,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
        )
    }

    fun observeNoteDetails(bookmarkId: String): Flow<NoteBookmarkEntity?> =
        noteBookmarkDao.observeByBookmarkId(bookmarkId)

    suspend fun readNoteDocumentJson(bookmarkId: String): String? {
        val note = noteBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val raw = NotemarkFileManager.readDocumentByRelativePath(note.documentRelativePath)
        return raw
    }

    suspend fun resolveNoteAssetsBaseUrl(bookmarkId: String): String? {
        val folderPath = BookmarkFileManager.getAbsolutePath(
            CoreConstants.FileSystem.bookmarkFolder(bookmarkId),
        )
        val base = if (folderPath.startsWith("file://")) folderPath else "file://$folderPath"
        return base.trimEnd('/') + "/"
    }

    /**
     * Writes image bytes under the note's `assets/` folder and returns the canonical
     * document-relative `src` stored in editor document JSON (`../assets/<id>.<ext>`).
     */
    suspend fun saveInlineImageBytes(
        bookmarkId: String,
        bytes: ByteArray,
        extension: String,
    ): String {
        val assetId = IdGenerator.newId()
        val ext = sanitizeInlineImageExtension(extension)
        val relativePath = CoreConstants.FileSystem.Linkmark.assetPath(bookmarkId, assetId, ext)
        BookmarkFileManager.writeBytes(relativePath, bytes)
        return "../assets/$assetId.$ext"
    }

    fun sanitizeInlineImageExtension(raw: String?): String {
        val e = raw.orEmpty().lowercase().removePrefix(".").ifBlank { "jpeg" }
        return when (e) {
            "jpg" -> "jpeg"
            "jpeg", "png", "webp", "gif" -> e
            else -> "jpeg"
        }
    }

    /** Persists canonical editor document JSON and mirrors to readable for highlight anchoring. */
    fun saveNoteDocumentJson(
        bookmarkId: String,
        documentJson: String,
        touchEditedAt: Boolean = true,
    ) {
        CoreOperationQueue.queue("SaveNotemarkDocument:$bookmarkId") {
            saveNoteDocumentJsonInternal(bookmarkId, documentJson, touchEditedAt)
        }
    }

    suspend fun persistNoteDocumentJsonAwait(
        bookmarkId: String,
        documentJson: String,
        touchEditedAt: Boolean = true,
    ): Result<Unit> =
        CoreOperationQueue.queueAndAwait("SaveNotemarkDocument:$bookmarkId") {
            saveNoteDocumentJsonInternal(bookmarkId, documentJson, touchEditedAt)
        }

    private suspend fun saveNoteDocumentJsonInternal(
        bookmarkId: String,
        documentJson: String,
        touchEditedAt: Boolean,
    ) {
        val note = noteBookmarkDao.getByBookmarkId(bookmarkId) ?: return
        NotemarkFileManager.writeDocumentBody(bookmarkId, documentJson)
        ReadableContentManager.syncNotemarkReadableMirror(
            bookmarkId = bookmarkId,
            versionId = note.readableVersionId,
            documentJson = documentJson,
        )
        if (touchEditedAt) {
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    /**
     * Creates empty body file, readable mirror row, and note subtype row. Call after bookmark
     * metadata exists.
     */
    fun createOrUpdateNoteDetails(
        bookmarkId: String,
        documentRelativePath: String = NotemarkFileManager.documentBodyRelativePath(bookmarkId),
        readableVersionId: String? = null,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateNoteDetails:$bookmarkId") {
            createOrUpdateNoteDetailsInternal(
                bookmarkId = bookmarkId,
                documentRelativePath = documentRelativePath,
                readableVersionId = readableVersionId,
            )
        }
    }

    private suspend fun createOrUpdateNoteDetailsInternal(
        bookmarkId: String,
        documentRelativePath: String,
        readableVersionId: String?,
    ) {
        NotemarkFileManager.ensureEmptyDocumentBody(bookmarkId)
        val body = NotemarkFileManager.readDocumentBody(bookmarkId).orEmpty()
        val resolvedVersionId =
            readableVersionId
                ?: noteBookmarkDao.getByBookmarkId(bookmarkId)?.readableVersionId
                ?: IdGenerator.newId()
        ReadableContentManager.syncNotemarkReadableMirror(
            bookmarkId = bookmarkId,
            versionId = resolvedVersionId,
            documentJson = body.ifBlank { EMPTY_EDITOR_DOCUMENT_JSON },
        )
        noteBookmarkDao.upsert(
            NoteBookmarkEntity(
                bookmarkId = bookmarkId,
                documentRelativePath = documentRelativePath,
                readableVersionId = resolvedVersionId,
            ),
        )
    }
}
