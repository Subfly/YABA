package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.NoteBookmarkEntity
import dev.subfly.yabacore.database.mappers.toUiModel
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.NotemarkFileManager
import dev.subfly.yabacore.model.ui.NotemarkUiModel
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.webview.normalizeMarkdownEscapesCorruption
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

object NotemarkManager {
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val noteBookmarkDao get() = DatabaseProvider.noteBookmarkDao
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao

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
            markdownRelativePath = noteMeta.markdownRelativePath,
            readableVersionId = noteMeta.readableVersionId,
            localImagePath = localImageAbsolutePath,
            localIconPath = localIconAbsolutePath,
            parentFolder = folder,
            tags = tags,
        )
    }

    fun observeNoteDetails(bookmarkId: String): Flow<NoteBookmarkEntity?> =
        noteBookmarkDao.observeByBookmarkId(bookmarkId)

    suspend fun readNoteMarkdown(bookmarkId: String): String? {
        val note = noteBookmarkDao.getByBookmarkId(bookmarkId) ?: return null
        val raw = NotemarkFileManager.readMarkdownByRelativePath(note.markdownRelativePath)
        return raw?.let { normalizeMarkdownEscapesCorruption(it) }
    }

    suspend fun resolveNoteAssetsBaseUrl(bookmarkId: String): String? {
        val folderPath = BookmarkFileManager.getAbsolutePath(
            CoreConstants.FileSystem.bookmarkFolder(bookmarkId),
        )
        val base = if (folderPath.startsWith("file://")) folderPath else "file://$folderPath"
        return base.trimEnd('/') + "/"
    }

    /**
     * Persists canonical body and mirrors to readable for highlight anchoring.
     */
    fun saveNoteMarkdown(
        bookmarkId: String,
        markdown: String,
        touchEditedAt: Boolean = true,
    ) {
        CoreOperationQueue.queue("SaveNotemarkMarkdown:$bookmarkId") {
            saveNoteMarkdownInternal(bookmarkId, markdown, touchEditedAt)
        }
    }

    /**
     * Persists note markdown and waits for the operation queue to finish (for editor save UX).
     */
    suspend fun persistNoteMarkdownAwait(
        bookmarkId: String,
        markdown: String,
        touchEditedAt: Boolean = true,
    ): Result<Unit> =
        CoreOperationQueue.queueAndAwait("SaveNotemarkMarkdown:$bookmarkId") {
            saveNoteMarkdownInternal(bookmarkId, markdown, touchEditedAt)
        }

    private suspend fun saveNoteMarkdownInternal(
        bookmarkId: String,
        markdown: String,
        touchEditedAt: Boolean,
    ) {
        val note = noteBookmarkDao.getByBookmarkId(bookmarkId) ?: return
        NotemarkFileManager.writeMarkdownBody(bookmarkId, markdown)
        val bookmark = bookmarkDao.getById(bookmarkId)
        ReadableContentManager.syncNotemarkReadableMirror(
            bookmarkId = bookmarkId,
            versionId = note.readableVersionId,
            markdownBody = markdown,
            title = bookmark?.label?.takeIf { it.isNotBlank() },
            author = null,
        )
        if (touchEditedAt) {
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    /**
     * Creates empty body file, readable mirror row, and note subtype row. Call after bookmark metadata exists.
     *
     * @param readableVersionId When null, reuses an existing note row's id or allocates a new one for first create.
     */
    fun createOrUpdateNoteDetails(
        bookmarkId: String,
        markdownRelativePath: String = NotemarkFileManager.markdownBodyRelativePath(bookmarkId),
        readableVersionId: String? = null,
    ) {
        CoreOperationQueue.queue("CreateOrUpdateNoteDetails:$bookmarkId") {
            createOrUpdateNoteDetailsInternal(
                bookmarkId = bookmarkId,
                markdownRelativePath = markdownRelativePath,
                readableVersionId = readableVersionId,
            )
        }
    }

    private suspend fun createOrUpdateNoteDetailsInternal(
        bookmarkId: String,
        markdownRelativePath: String,
        readableVersionId: String?,
    ) {
        NotemarkFileManager.ensureEmptyMarkdownBody(bookmarkId)
        val bookmark = bookmarkDao.getById(bookmarkId) ?: return
        val body = NotemarkFileManager.readMarkdownBody(bookmarkId).orEmpty()
        val resolvedVersionId =
            readableVersionId
                ?: noteBookmarkDao.getByBookmarkId(bookmarkId)?.readableVersionId
                ?: IdGenerator.newId()
        ReadableContentManager.syncNotemarkReadableMirror(
            bookmarkId = bookmarkId,
            versionId = resolvedVersionId,
            markdownBody = body,
            title = bookmark.label.takeIf { it.isNotBlank() },
            author = null,
        )
        noteBookmarkDao.upsert(
            NoteBookmarkEntity(
                bookmarkId = bookmarkId,
                markdownRelativePath = markdownRelativePath,
                readableVersionId = resolvedVersionId,
            ),
        )
    }
}
