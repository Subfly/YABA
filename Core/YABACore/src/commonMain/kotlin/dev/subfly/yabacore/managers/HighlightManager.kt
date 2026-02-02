package dev.subfly.yabacore.managers

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.json.HighlightJson
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.queue.CoreOperationQueue
import dev.subfly.yabacore.sync.CRDTEngine
import dev.subfly.yabacore.sync.FileTarget
import dev.subfly.yabacore.sync.ObjectType
import dev.subfly.yabacore.sync.VectorClock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Clock

/**
 * Filesystem-first manager for highlight annotations.
 *
 * Highlights are mutable entities stored in `/bookmarks/<id>/content/annotations/<highlightId>.json`.
 * All changes go through CRDT events for conflict-free sync.
 */
object HighlightManager {
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine
    private val highlightDao get() = DatabaseProvider.highlightDao

    // ==================== Create ====================

    /**
     * Creates a new highlight annotation (section-anchored).
     *
     * @param bookmarkId The bookmark this highlight belongs to
     * @param contentVersion The readable content version this highlight references
     * @param startSectionKey Section key where the highlight starts (e.g. "b:0")
     * @param startOffsetInSection Character offset within the start section (inclusive)
     * @param endSectionKey Section key where the highlight ends
     * @param endOffsetInSection Character offset within the end section (exclusive)
     * @param colorRole The highlight color role
     * @param note Optional note text
     * @return The created highlight ID
     */
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
            val payload = buildHighlightPayload(
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

            val event = crdtEngine.recordCreate(
                objectId = highlightId,
                objectType = ObjectType.HIGHLIGHT,
                file = FileTarget.HIGHLIGHT_JSON,
                payload = payload,
            )

            val highlightData = HighlightJson(
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
                clock = event.clock.toMap(),
            )

            // Write to filesystem
            entityFileManager.writeHighlight(highlightData)

            // Update Room index
            updateRoomIndex(highlightData)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }

        return highlightId
    }

    // ==================== Update ====================

    fun updateHighlight(
        bookmarkId: String,
        highlightId: String,
        colorRole: YabaColor,
        note: String?,
    ) {
        CoreOperationQueue.queue("UpdateHighlight:$highlightId") {
            updateHighlightInternal(bookmarkId, highlightId) { existing ->
                existing.copy(
                    colorRole = colorRole,
                    note = note,
                    editedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    /**
     * Internal update handler.
     */
    private suspend fun updateHighlightInternal(
        bookmarkId: String,
        highlightId: String,
        transform: (HighlightJson) -> HighlightJson,
    ) {
        val existing = entityFileManager.readHighlight(bookmarkId, highlightId) ?: return
        val existingClock = VectorClock.fromMap(existing.clock)

        val updated = transform(existing)

        // Build CRDT payload with changed fields only
        val changes = mutableMapOf<String, JsonElement>()
        if (updated.note != existing.note) {
            changes["note"] = CRDTEngine.nullableStringValue(updated.note)
        }
        if (updated.colorRole != existing.colorRole) {
            changes["colorRole"] = JsonPrimitive(updated.colorRole.code)
        }
        changes["editedAt"] = JsonPrimitive(updated.editedAt)

        // Record CRDT UPDATE event
        val event = crdtEngine.recordUpdate(
            objectId = highlightId,
            objectType = ObjectType.HIGHLIGHT,
            file = FileTarget.HIGHLIGHT_JSON,
            changes = changes,
            currentClock = existingClock,
        ) ?: return

        // Write updated highlight with new clock
        val finalHighlight = updated.copy(clock = event.clock.toMap())
        entityFileManager.writeHighlight(finalHighlight)

        // Update Room index
        updateRoomIndex(finalHighlight)
    }

    // ==================== Delete ====================

    /**
     * Deletes a highlight annotation.
     */
    fun deleteHighlight(bookmarkId: String, highlightId: String) {
        CoreOperationQueue.queue("DeleteHighlight:$highlightId") {
            val existing = entityFileManager.readHighlight(bookmarkId, highlightId) ?: return@queue
            val existingClock = VectorClock.fromMap(existing.clock)

            // Record CRDT DELETE event
            crdtEngine.recordDelete(
                objectId = highlightId,
                objectType = ObjectType.HIGHLIGHT,
                currentClock = existingClock,
            )

            // Delete from filesystem
            entityFileManager.deleteHighlight(bookmarkId, highlightId)

            // Delete from Room index
            highlightDao.deleteById(highlightId)
            AllBookmarksManager.touchBookmarkEditedAt(bookmarkId)
        }
    }

    // ==================== Read ====================

    /**
     * Gets all highlights for a bookmark.
     */
    suspend fun getHighlightsForBookmark(bookmarkId: String): List<HighlightJson> {
        return entityFileManager.readAllHighlights(bookmarkId)
    }

    /**
     * Gets all highlights for a specific content version.
     */
    suspend fun getHighlightsForVersion(
        bookmarkId: String,
        contentVersion: Int,
    ): List<HighlightJson> {
        return entityFileManager.readAllHighlights(bookmarkId)
            .filter { it.contentVersion == contentVersion }
    }

    /**
     * Gets a single highlight by ID.
     */
    suspend fun getHighlight(bookmarkId: String, highlightId: String): HighlightJson? {
        return entityFileManager.readHighlight(bookmarkId, highlightId)
    }

    // ==================== Private Helpers ====================

    private suspend fun updateRoomIndex(highlight: HighlightJson) {
        val entity = HighlightEntity(
            id = highlight.id,
            bookmarkId = highlight.bookmarkId,
            contentVersion = highlight.contentVersion,
            startSectionKey = highlight.startSectionKey,
            startOffsetInSection = highlight.startOffsetInSection,
            endSectionKey = highlight.endSectionKey,
            endOffsetInSection = highlight.endOffsetInSection,
            colorRole = highlight.colorRole,
            note = highlight.note,
            relativePath = CoreConstants.FileSystem.Linkmark.highlightPath(
                highlight.bookmarkId,
                highlight.id,
            ),
            createdAt = highlight.createdAt,
            editedAt = highlight.editedAt,
        )
        highlightDao.upsert(entity)
    }

    private fun buildHighlightPayload(
        bookmarkId: String,
        contentVersion: Int,
        startSectionKey: String,
        startOffsetInSection: Int,
        endSectionKey: String,
        endOffsetInSection: Int,
        colorRole: YabaColor,
        note: String?,
        createdAt: Long,
        editedAt: Long,
    ): Map<String, JsonElement> = mapOf(
        "bookmarkId" to JsonPrimitive(bookmarkId),
        "contentVersion" to JsonPrimitive(contentVersion),
        "startSectionKey" to JsonPrimitive(startSectionKey),
        "startOffsetInSection" to JsonPrimitive(startOffsetInSection),
        "endSectionKey" to JsonPrimitive(endSectionKey),
        "endOffsetInSection" to JsonPrimitive(endOffsetInSection),
        "colorRole" to JsonPrimitive(colorRole.code),
        "note" to CRDTEngine.nullableStringValue(note),
        "createdAt" to JsonPrimitive(createdAt),
        "editedAt" to JsonPrimitive(editedAt),
    )

}
