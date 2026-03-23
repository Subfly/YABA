package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.AnnotationEntity
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import kotlin.time.Clock

/**
 * DB-first manager for persisted annotations (read-it-later, PDF).
 */
object AnnotationManager {
    private val annotationDao get() = DatabaseProvider.annotationDao

    fun createAnnotation(
        annotationId: String,
        bookmarkId: String,
        readableVersionId: String,
        type: AnnotationType,
        colorRole: YabaColor = YabaColor.NONE,
        note: String? = null,
        quoteText: String? = null,
        extrasJson: String? = null,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()

        CoreOperationQueue.queue("CreateAnnotation:$annotationId") {
            val entity = AnnotationEntity(
                id = annotationId,
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
            annotationDao.upsert(entity)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    fun updateAnnotation(
        bookmarkId: String,
        annotationId: String,
        colorRole: YabaColor,
        note: String?,
    ) {
        CoreOperationQueue.queue("UpdateAnnotation:$annotationId") {
            val existing = annotationDao.getById(annotationId) ?: return@queue
            val updated = existing.copy(
                colorRole = colorRole,
                note = note,
                editedAt = Clock.System.now().toEpochMilliseconds(),
            )
            annotationDao.upsert(updated)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    fun deleteAnnotation(bookmarkId: String, annotationId: String) {
        CoreOperationQueue.queue("DeleteAnnotation:$annotationId") {
            annotationDao.deleteById(annotationId)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    suspend fun getAnnotationsForBookmark(bookmarkId: String): List<AnnotationEntity> =
        annotationDao.getByBookmarkId(bookmarkId)

    suspend fun getAnnotationsForVersion(
        bookmarkId: String,
        readableVersionId: String,
    ): List<AnnotationEntity> =
        annotationDao.getByBookmarkId(bookmarkId, readableVersionId = readableVersionId)

    suspend fun getAnnotation(bookmarkId: String, annotationId: String): AnnotationEntity? =
        annotationDao.getById(annotationId)
}
