package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlin.time.Clock

/**
 * DB-first manager for highlight annotations.
 *
 * Highlights are stored in Room only. Section-anchored: startSectionKey, startOffsetInSection,
 * endSectionKey, endOffsetInSection.
 */
object HighlightManager {
    private val highlightDao get() = DatabaseProvider.highlightDao

    fun createHighlight(
        bookmarkId: String,
        contentVersion: Int,
        startSectionKey: String,
        startOffsetInSection: Int,
        endSectionKey: String,
        endOffsetInSection: Int,
        colorRole: YabaColor = YabaColor.NONE,
        note: String? = null,
    ): String {
        val highlightId = IdGenerator.newId()
        val now = Clock.System.now().toEpochMilliseconds()

        CoreOperationQueue.queue("CreateHighlight:$highlightId") {
            val entity = HighlightEntity(
                id = highlightId,
                bookmarkId = bookmarkId,
                contentVersion = contentVersion,
                startSectionKey = startSectionKey,
                startOffsetInSection = startOffsetInSection,
                endSectionKey = endSectionKey,
                endOffsetInSection = endOffsetInSection,
                colorRole = colorRole,
                note = note,
                createdAt = now,
                editedAt = now,
            )
            highlightDao.upsert(entity)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }

        return highlightId
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
        contentVersion: Int,
    ): List<HighlightEntity> =
        highlightDao.getByBookmarkId(bookmarkId, version = contentVersion)

    suspend fun getHighlight(bookmarkId: String, highlightId: String): HighlightEntity? =
        highlightDao.getById(highlightId)
}
