package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlin.time.Clock

/**
 * DB-first manager for highlight annotations.
 */
object HighlightManager {
    private val highlightDao get() = DatabaseProvider.highlightDao

    fun createHighlight(
        highlightId: String,
        bookmarkId: String,
        readableVersionId: String,
        type: HighlightType,
        colorRole: YabaColor = YabaColor.NONE,
        note: String? = null,
        quoteText: String? = null,
        extrasJson: String? = null,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()

        CoreOperationQueue.queue("CreateHighlight:$highlightId") {
            val entity = HighlightEntity(
                id = highlightId,
                bookmarkId = bookmarkId,
                readableVersionId = readableVersionId,
                type = type,
                colorRole = colorRole,
                note = note,
                quoteText = quoteText,
                extrasJson = extrasJson,
                createdAt = now,
                editedAt = now,
            )
            highlightDao.upsert(entity)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    fun updateHighlight(
        bookmarkId: String,
        highlightId: String,
        colorRole: YabaColor,
        note: String?,
    ) {
        CoreOperationQueue.queue("UpdateHighlight:$highlightId") {
            val existing = highlightDao.getById(highlightId) ?: return@queue
            val updated = existing.copy(
                colorRole = colorRole,
                note = note,
                editedAt = Clock.System.now().toEpochMilliseconds(),
            )
            highlightDao.upsert(updated)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    fun deleteHighlight(bookmarkId: String, highlightId: String) {
        CoreOperationQueue.queue("DeleteHighlight:$highlightId") {
            highlightDao.deleteById(highlightId)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    suspend fun getHighlightsForBookmark(bookmarkId: String): List<HighlightEntity> =
        highlightDao.getByBookmarkId(bookmarkId)

    suspend fun getHighlightsForVersion(
        bookmarkId: String,
        readableVersionId: String,
    ): List<HighlightEntity> =
        highlightDao.getByBookmarkId(bookmarkId, readableVersionId = readableVersionId)

    suspend fun getHighlight(bookmarkId: String, highlightId: String): HighlightEntity? =
        highlightDao.getById(highlightId)
}
