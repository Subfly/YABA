package dev.subfly.yabacore.sync

import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.database.CacheRebuilder
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEvents
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.FileSystemStateManager
import dev.subfly.yabacore.filesystem.SyncState
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.filesystem.json.FolderMetaJson
import dev.subfly.yabacore.filesystem.json.HighlightAnchor
import dev.subfly.yabacore.filesystem.json.HighlightJson
import dev.subfly.yabacore.filesystem.json.TagMetaJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Sync modes for the SyncEngine.
 */
enum class SyncMode {
    /** Clear SQLite, scan filesystem, rebuild cache from scratch */
    FULL_REFRESH,
    /** Collect pending events, CRDT merge, update JSON, update SQLite */
    INCREMENTAL_MERGE,
}

/**
 * Filesystem-first SyncEngine.
 *
 * Operates in two modes:
 * - **Full Refresh**: Rebuilds SQLite cache entirely from filesystem JSON files
 * - **Incremental Merge**: Applies CRDT events to merge changes
 *
 * The filesystem is always the authoritative source of truth.
 *
 * Delete handling:
 * - DELETE events dominate all updates (delete dominance)
 * - System entities (Uncategorized folder, Pinned/Private tags) become hidden instead of deleted
 * - DELETE events are never compacted
 */
object SyncEngine {
    private val eventsDao get() = EventsDatabaseProvider.eventsDao
    private val entityFileManager get() = EntityFileManager
    private val crdtEngine get() = CRDTEngine

    /**
     * Performs a full refresh: clears SQLite and rebuilds from filesystem.
     *
     * Use this when:
     * - First launch after migration
     * - Database corruption detected
     * - User requests full resync
     */
    suspend fun fullRefresh() {
        FileSystemStateManager.setSyncState(SyncState.SYNCING)
        try {
            CacheRebuilder.rebuildFromFilesystem()

            // Compact logs after full refresh
            LogCompaction.compactIfNeeded()

            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
        } catch (e: Exception) {
            FileSystemStateManager.setSyncState(SyncState.SYNC_FAILED)
            throw e
        }
    }

    /**
     * Performs an incremental merge: applies pending CRDT events.
     *
     * For each object with pending events:
     * 1. Read current JSON from filesystem
     * 2. Merge events using CRDT rules
     * 3. Write merged JSON to filesystem
     * 4. Update SQLite cache (for cross-refs when tagIds change)
     *
     * Events are NOT deleted immediately after merge. Instead, LogCompaction runs
     * after the merge to clean up events using "snapshot dominance" and "tombstone
     * dominance" rules. This allows events to remain available for peer sync.
     */
    suspend fun incrementalMerge() {
        FileSystemStateManager.setSyncState(SyncState.SYNCING)
        try {
            val objectIds = eventsDao.getObjectIdsWithEvents()

            objectIds.forEach { objectId ->
                mergeObjectEvents(objectId)
            }

            // Run compaction after merge to clean up events that are now
            // older than the filesystem clock (snapshot dominance) or
            // belong to deleted entities (tombstone dominance).
            // This runs unconditionally to ensure events are cleaned up.
            LogCompaction.compact()

            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
        } catch (e: Exception) {
            FileSystemStateManager.setSyncState(SyncState.SYNC_FAILED)
            throw e
        }
    }

    /**
     * Receives events from a remote peer and applies them.
     *
     * Events are stored, merged into the filesystem, and then compaction
     * runs to clean up events that are now covered by the filesystem clock.
     */
    suspend fun receiveRemoteEvents(events: List<CRDTEvent>) {
        if (events.isEmpty()) return

        // Store events in database
        crdtEngine.applyEvents(events)

        // Merge each affected object
        val objectIds = events.map { it.objectId }.distinct()
        objectIds.forEach { objectId ->
            mergeObjectEvents(objectId)
        }

        // Run compaction to clean up events that are now older than filesystem
        LogCompaction.compact()
    }

    /**
     * Gets pending events to send to a remote peer.
     *
     * @param afterTimestamp Only get events after this timestamp (for delta sync)
     */
    suspend fun getPendingEventsForSync(afterTimestamp: Long = 0): List<CRDTEvent> {
        return if (afterTimestamp > 0) {
            crdtEngine.getEventsAfterTimestamp(afterTimestamp)
        } else {
            crdtEngine.getPendingEvents()
        }
    }

    /**
     * Checks if drift exists and fixes it.
     */
    suspend fun syncIfNeeded() {
        val drift = FileSystemStateManager.detectDrift()
        if (drift.hasDrift) {
            CacheRebuilder.fixDrift()
        }
    }

    /**
     * Performs sync based on the specified mode.
     */
    suspend fun sync(mode: SyncMode) {
        when (mode) {
            SyncMode.FULL_REFRESH -> fullRefresh()
            SyncMode.INCREMENTAL_MERGE -> incrementalMerge()
        }
    }

    // ==================== Private Helpers ====================

    private suspend fun mergeObjectEvents(objectId: String) {
        var events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return

        val objectType = events.first().objectType

        // Check if entity is already deleted on filesystem
        val isDeletedOnFilesystem = when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(objectId)
            ObjectType.TAG -> entityFileManager.isTagDeleted(objectId)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(objectId)
            ObjectType.HIGHLIGHT -> isHighlightDeleted(objectId)
        }

        if (isDeletedOnFilesystem) {
            // Entity is already deleted on filesystem
            // Only remove non-DELETE events (DELETE events are preserved forever)
            crdtEngine.deleteNonDeleteEventsForObject(objectId)
            return
        }

        // Check for DELETE events using the new typed event system
        val deleteEvents = events.filter { it.isDelete() }
        if (deleteEvents.isNotEmpty()) {
            // Find the winning delete event
            val winningDelete = deleteEvents.maxByOrNull { it.timestamp }
                ?: deleteEvents.first()

            // Check if this is a system entity
            val isSystemEntity = isSystemEntity(objectType, objectId)

            if (isSystemEntity) {
                // Legacy compatibility:
                // System entities must never be deleted as CRDT entities.
                // Convert DELETE -> UPDATE(isHidden=true) and remove DELETE events so they don't dominate.
                convertLegacySystemDeleteToHiddenUpdate(objectType, objectId, winningDelete)
                // Reload events after conversion/removal and continue with normal merge.
                events = eventsDao.getEventsForObject(objectId).toEvents()
            } else {
                // Normal entities get deleted
                applyDeletionFromEvent(objectType, objectId, winningDelete.clock)

                // Only remove non-DELETE events (DELETE events are preserved forever)
                crdtEngine.deleteNonDeleteEventsForObject(objectId)
                return
            }
        }

        // No delete events - merge CREATE/UPDATE events
        when (objectType) {
            ObjectType.FOLDER -> mergeFolderEvents(objectId, events)
            ObjectType.TAG -> mergeTagEvents(objectId, events)
            ObjectType.BOOKMARK -> mergeBookmarkEvents(objectId, events)
            ObjectType.HIGHLIGHT -> mergeHighlightEvents(objectId, events)
        }
    }

    /**
     * Checks if an entity is a system entity that cannot be truly deleted.
     */
    private fun isSystemEntity(objectType: ObjectType, entityId: String): Boolean {
        return when (objectType) {
            ObjectType.FOLDER -> CoreConstants.Folder.isSystemFolder(entityId)
            ObjectType.TAG -> CoreConstants.Tag.isSystemTag(entityId)
            ObjectType.BOOKMARK -> false // No system bookmarks
            ObjectType.HIGHLIGHT -> false // No system highlights
        }
    }

    /**
     * Applies a deletion that was discovered via CRDT event resolution.
     * This writes the tombstone and cleans up the filesystem and index.
     */
    private suspend fun applyDeletionFromEvent(
        objectType: ObjectType,
        entityId: String,
        deletionClock: VectorClock,
    ) {
        when (objectType) {
            ObjectType.FOLDER -> {
                entityFileManager.deleteFolder(entityId, deletionClock)
                DatabaseProvider.folderDao.deleteById(entityId)
            }
            ObjectType.TAG -> {
                entityFileManager.deleteTag(entityId, deletionClock)
                DatabaseProvider.tagDao.deleteById(entityId)
            }
            ObjectType.BOOKMARK -> {
                entityFileManager.deleteBookmark(entityId, deletionClock)
                DatabaseProvider.tagBookmarkDao.deleteForBookmark(entityId)
                DatabaseProvider.linkBookmarkDao.deleteById(entityId)
                DatabaseProvider.bookmarkDao.deleteByIds(listOf(entityId))
                DatabaseProvider.highlightDao.deleteByBookmarkId(entityId)
            }
            ObjectType.HIGHLIGHT -> {
                deleteHighlightFile(entityId)
                DatabaseProvider.highlightDao.deleteById(entityId)
            }
        }
    }

    /**
     * Applies a "hidden" state for system entities that receive DELETE events.
     * System entities cannot be truly deleted, but they can be hidden.
     * This sets isHidden=true in the entity's meta.json.
     */
    private suspend fun applyHiddenFromEvent(
        objectType: ObjectType,
        entityId: String,
        deletionClock: VectorClock,
    ) {
        when (objectType) {
            ObjectType.FOLDER -> {
                val existing = entityFileManager.readFolderMeta(entityId)
                if (existing != null) {
                    val updated = existing.copy(
                        isHidden = true,
                        clock = deletionClock.toMap(),
                    )
                    entityFileManager.writeFolderMeta(updated)
                    // Update cache with hidden state
                    // The entity remains in the DB but with isHidden=true
                    DatabaseProvider.folderDao.upsert(updated.toEntity())
                }
            }
            ObjectType.TAG -> {
                val existing = entityFileManager.readTagMeta(entityId)
                if (existing != null) {
                    val updated = existing.copy(
                        isHidden = true,
                        clock = deletionClock.toMap(),
                    )
                    entityFileManager.writeTagMeta(updated)
                    // Update cache with hidden state
                    DatabaseProvider.tagDao.upsert(updated.toEntity())
                }
            }
            ObjectType.BOOKMARK -> {
                // Bookmarks don't have system entities, so this shouldn't be called
                // But if it is, just apply normal deletion
                applyDeletionFromEvent(objectType, entityId, deletionClock)
            }
            ObjectType.HIGHLIGHT -> {
                // Highlights don't have system entities
                applyDeletionFromEvent(objectType, entityId, deletionClock)
            }
        }
    }

    /**
     * Converts a legacy system-entity DELETE event into a visibility UPDATE (isHidden=true),
     * then removes DELETE events for the entity so future visibility updates can win.
     *
     * System entities must never be terminally deleted.
     */
    private suspend fun convertLegacySystemDeleteToHiddenUpdate(
        objectType: ObjectType,
        entityId: String,
        winningDelete: CRDTEvent,
    ) {
        // Apply hidden state with the delete's resolved clock (filesystem + cache)
        applyHiddenFromEvent(objectType, entityId, winningDelete.clock)

        // Insert an equivalent UPDATE event so state can sync without delete dominance
        val updateEvent = CRDTEvent(
            eventId = IdGenerator.newId(),
            objectId = entityId,
            objectType = objectType,
            eventType = EventType.UPDATE,
            file = FileTarget.META_JSON,
            payload = JsonObject(
                mapOf(
                    "isHidden" to JsonPrimitive(true),
                    "editedAt" to JsonPrimitive(winningDelete.timestamp),
                )
            ),
            clock = winningDelete.clock,
            timestamp = winningDelete.timestamp,
        )
        crdtEngine.applyEvent(updateEvent)

        // Remove legacy DELETE events for this entity (prohibited for system entities)
        crdtEngine.deleteDeleteEventsForObject(entityId)
    }

    private fun FolderMetaJson.toEntity(): FolderEntity = FolderEntity(
        id = id,
        parentId = parentId,
        label = label,
        description = description,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
        isHidden = isHidden,
    )

    private fun TagMetaJson.toEntity(): TagEntity = TagEntity(
        id = id,
        label = label,
        icon = icon,
        color = YabaColor.fromCode(colorCode),
        order = order,
        createdAt = createdAt,
        editedAt = editedAt,
        isHidden = isHidden,
    )

    private suspend fun mergeFolderEvents(folderId: String, events: List<CRDTEvent>) {
        val existingJson = entityFileManager.readFolderMeta(folderId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // Use CRDTEngine to merge events and get resolved fields
        val metaEvents = events.filter { it.file == FileTarget.META_JSON && !it.isDelete() }
        val resolvedFields = mutableMapOf<String, JsonElement>()
        var mergedClock = existingClock

        // Merge all event payloads, resolving conflicts per-field
        mergeEventPayloadsToFields(metaEvents, resolvedFields)
        metaEvents.forEach { mergedClock = mergedClock.merge(it.clock) }

        if (existingJson != null && resolvedFields.isNotEmpty()) {
            // Apply resolved fields to existing JSON
            val updatedJson = existingJson.copy(
                parentId = resolvedFields["parentId"]?.jsonPrimitive?.contentOrNull ?: existingJson.parentId,
                label = resolvedFields["label"]?.jsonPrimitive?.contentOrNull ?: existingJson.label,
                description = resolvedFields["description"]?.jsonPrimitive?.contentOrNull ?: existingJson.description,
                icon = resolvedFields["icon"]?.jsonPrimitive?.contentOrNull ?: existingJson.icon,
                colorCode = resolvedFields["colorCode"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.colorCode,
                order = resolvedFields["order"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.order,
                editedAt = resolvedFields["editedAt"]?.jsonPrimitive?.longOrNull ?: existingJson.editedAt,
                isHidden = resolvedFields["isHidden"]?.jsonPrimitive?.booleanOrNull ?: existingJson.isHidden,
                clock = mergedClock.toMap(),
            )
            entityFileManager.writeFolderMeta(updatedJson)
            DatabaseProvider.folderDao.upsert(updatedJson.toEntity())
        }

        // Events are NOT deleted immediately - LogCompaction will clean them up
        // using "snapshot dominance" rule once the filesystem clock is newer.
    }

    private suspend fun mergeTagEvents(tagId: String, events: List<CRDTEvent>) {
        val existingJson = entityFileManager.readTagMeta(tagId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        val metaEvents = events.filter { it.file == FileTarget.META_JSON && !it.isDelete() }
        val resolvedFields = mutableMapOf<String, JsonElement>()
        var mergedClock = existingClock

        mergeEventPayloadsToFields(metaEvents, resolvedFields)
        metaEvents.forEach { mergedClock = mergedClock.merge(it.clock) }

        if (existingJson != null && resolvedFields.isNotEmpty()) {
            val updatedJson = existingJson.copy(
                label = resolvedFields["label"]?.jsonPrimitive?.contentOrNull ?: existingJson.label,
                icon = resolvedFields["icon"]?.jsonPrimitive?.contentOrNull ?: existingJson.icon,
                colorCode = resolvedFields["colorCode"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.colorCode,
                order = resolvedFields["order"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.order,
                editedAt = resolvedFields["editedAt"]?.jsonPrimitive?.longOrNull ?: existingJson.editedAt,
                isHidden = resolvedFields["isHidden"]?.jsonPrimitive?.booleanOrNull ?: existingJson.isHidden,
                clock = mergedClock.toMap(),
            )
            entityFileManager.writeTagMeta(updatedJson)
            DatabaseProvider.tagDao.upsert(updatedJson.toEntity())
        }

        // Events are NOT deleted immediately - LogCompaction will clean them up
    }

    private suspend fun mergeBookmarkEvents(bookmarkId: String, events: List<CRDTEvent>) {
        val existingMeta = entityFileManager.readBookmarkMeta(bookmarkId)
        val existingLink = entityFileManager.readLinkJson(bookmarkId)
        val existingMetaClock = existingMeta?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val existingLinkClock = existingLink?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // Process meta.json events
        val metaEvents = events.filter { it.file == FileTarget.META_JSON && !it.isDelete() }
        val metaFields = mutableMapOf<String, JsonElement>()
        var mergedMetaClock = existingMetaClock

        mergeEventPayloadsToFields(metaEvents, metaFields)
        metaEvents.forEach { mergedMetaClock = mergedMetaClock.merge(it.clock) }

        // Process link.json events
        val linkEvents = events.filter { it.file == FileTarget.LINK_JSON && !it.isDelete() }
        val linkFields = mutableMapOf<String, JsonElement>()
        var mergedLinkClock = existingLinkClock

        mergeEventPayloadsToFields(linkEvents, linkFields)
        linkEvents.forEach { mergedLinkClock = mergedLinkClock.merge(it.clock) }

        if (existingMeta != null && metaFields.isNotEmpty()) {
            // Parse tagIds from JsonArray if present
            val resolvedTagIds = metaFields["tagIds"]?.let { tagIdsElement ->
                when (tagIdsElement) {
                    is JsonArray -> tagIdsElement.mapNotNull { it.jsonPrimitive.contentOrNull }
                    else -> null
                }
            } ?: existingMeta.tagIds

            val updatedMeta = existingMeta.copy(
                folderId = metaFields["folderId"]?.jsonPrimitive?.contentOrNull ?: existingMeta.folderId,
                label = metaFields["label"]?.jsonPrimitive?.contentOrNull ?: existingMeta.label,
                description = metaFields["description"]?.jsonPrimitive?.contentOrNull ?: existingMeta.description,
                editedAt = metaFields["editedAt"]?.jsonPrimitive?.longOrNull ?: existingMeta.editedAt,
                viewCount = metaFields["viewCount"]?.jsonPrimitive?.longOrNull ?: existingMeta.viewCount,
                isPrivate = metaFields["isPrivate"]?.jsonPrimitive?.booleanOrNull ?: existingMeta.isPrivate,
                isPinned = metaFields["isPinned"]?.jsonPrimitive?.booleanOrNull ?: existingMeta.isPinned,
                localImagePath = metaFields["localImagePath"]?.jsonPrimitive?.contentOrNull ?: existingMeta.localImagePath,
                localIconPath = metaFields["localIconPath"]?.jsonPrimitive?.contentOrNull ?: existingMeta.localIconPath,
                tagIds = resolvedTagIds,
                clock = mergedMetaClock.toMap(),
            )
            entityFileManager.writeBookmarkMeta(updatedMeta)

            // Update SQLite tag-bookmark cross-ref if tagIds changed
            if (metaFields.containsKey("tagIds")) {
                updateTagBookmarkCrossRefs(bookmarkId, resolvedTagIds)
            }
        }

        if (existingLink != null && linkFields.isNotEmpty()) {
            val updatedLink = existingLink.copy(
                url = linkFields["url"]?.jsonPrimitive?.contentOrNull ?: existingLink.url,
                domain = linkFields["domain"]?.jsonPrimitive?.contentOrNull ?: existingLink.domain,
                linkTypeCode = linkFields["linkTypeCode"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingLink.linkTypeCode,
                videoUrl = linkFields["videoUrl"]?.jsonPrimitive?.contentOrNull ?: existingLink.videoUrl,
                clock = mergedLinkClock.toMap(),
            )
            entityFileManager.writeLinkJson(bookmarkId, updatedLink)
        }

        // Events are NOT deleted immediately - LogCompaction will clean them up
    }

    /**
     * Merges event payloads into a field map, resolving conflicts per-field.
     * For each field, the event with the highest clock wins.
     */
    private fun mergeEventPayloadsToFields(
        events: List<CRDTEvent>,
        target: MutableMap<String, JsonElement>,
    ) {
        // Collect all fields and their events
        val fieldEvents = mutableMapOf<String, MutableList<Pair<CRDTEvent, JsonElement>>>()

        for (event in events) {
            for ((field, value) in event.payload) {
                fieldEvents.getOrPut(field) { mutableListOf() }.add(event to value)
            }
        }

        // Resolve each field - event with highest clock wins
        for ((field, eventValuePairs) in fieldEvents) {
            var winner = eventValuePairs.first()
            for (pair in eventValuePairs.drop(1)) {
                if (pair.first.clock.isNewerThan(winner.first.clock)) {
                    winner = pair
                } else if (!winner.first.clock.isNewerThan(pair.first.clock)) {
                    // Concurrent - use deterministic tie-breaker
                    val comparison = VectorClock.deterministicCompare(pair.first.clock, winner.first.clock)
                    if (comparison > 0) {
                        winner = pair
                    }
                }
            }
            target[field] = winner.second
        }
    }

    /**
     * Updates SQLite tag-bookmark cross-refs to match the given tagIds list.
     * This ensures the index stays in sync after merging tagIds from CRDT events.
     */
    private suspend fun updateTagBookmarkCrossRefs(bookmarkId: String, tagIds: List<String>) {
        val tagBookmarkDao = DatabaseProvider.tagBookmarkDao

        // Remove all existing cross-refs for this bookmark
        tagBookmarkDao.deleteForBookmark(bookmarkId)

        // Insert new cross-refs for each tag
        val crossRefs = tagIds.map { tagId ->
            dev.subfly.yabacore.database.entities.TagBookmarkCrossRef(
                tagId = tagId,
                bookmarkId = bookmarkId,
            )
        }
        if (crossRefs.isNotEmpty()) {
            tagBookmarkDao.insertAll(crossRefs)
        }
    }

    // ==================== Highlight Support ====================

    /**
     * Checks if a highlight annotation file has been deleted.
     */
    private suspend fun isHighlightDeleted(highlightId: String): Boolean {
        // Highlights don't have tombstone files - check Room index
        return DatabaseProvider.highlightDao.getById(highlightId) == null
    }

    /**
     * Deletes a highlight annotation file from the filesystem.
     */
    private suspend fun deleteHighlightFile(highlightId: String) {
        val highlight = DatabaseProvider.highlightDao.getById(highlightId) ?: return
        entityFileManager.deleteHighlight(highlight.bookmarkId, highlightId)
    }

    /**
     * Merges CRDT events for a highlight annotation.
     */
    private suspend fun mergeHighlightEvents(highlightId: String, events: List<CRDTEvent>) {
        val highlightFields = mutableMapOf<String, JsonElement>()
        val highlightEvents = events.filter { it.file == FileTarget.HIGHLIGHT_JSON }

        mergeEventPayloadsToFields(highlightEvents, highlightFields)

        // Extract required fields
        val bookmarkId = highlightFields["bookmarkId"]?.jsonPrimitive?.contentOrNull ?: return
        val contentVersion = highlightFields["contentVersion"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return

        // Build HighlightJson from merged fields
        val highlightData = HighlightJson(
            id = highlightId,
            bookmarkId = bookmarkId,
            contentVersion = contentVersion,
            startAnchor = parseAnchor(highlightFields["startAnchor"]) ?: return,
            endAnchor = parseAnchor(highlightFields["endAnchor"]) ?: return,
            colorRole = parseYabaColor(highlightFields["colorRole"]),
            note = highlightFields["note"]?.jsonPrimitive?.contentOrNull,
            createdAt = highlightFields["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            editedAt = highlightFields["editedAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            clock = extractClockFromFields(highlightFields),
        )

        // Write to filesystem using entityFileManager
        entityFileManager.writeHighlight(highlightData)

        // Update Room index
        val relativePath = CoreConstants.FileSystem.Linkmark.highlightPath(bookmarkId, highlightId)
        val entity = dev.subfly.yabacore.database.entities.HighlightEntity(
            id = highlightId,
            bookmarkId = bookmarkId,
            contentVersion = contentVersion,
            startBlockId = highlightData.startAnchor.blockId,
            startInlinePath = highlightData.startAnchor.inlinePath.joinToString(","),
            startOffset = highlightData.startAnchor.offset,
            endBlockId = highlightData.endAnchor.blockId,
            endInlinePath = highlightData.endAnchor.inlinePath.joinToString(","),
            endOffset = highlightData.endAnchor.offset,
            colorRole = highlightData.colorRole,
            note = highlightData.note,
            relativePath = relativePath,
            createdAt = highlightData.createdAt,
            editedAt = highlightData.editedAt,
        )
        DatabaseProvider.highlightDao.upsert(entity)
    }

    /**
     * Parses a highlight anchor from a JsonElement.
     */
    private fun parseAnchor(element: JsonElement?): HighlightAnchor? {
        val obj = element as? JsonObject ?: return null
        val blockId = obj["blockId"]?.jsonPrimitive?.contentOrNull ?: return null
        val inlinePath = (obj["inlinePath"] as? JsonArray)?.mapNotNull {
            it.jsonPrimitive.contentOrNull?.toIntOrNull()
        } ?: emptyList()
        val offset = obj["offset"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: return null
        return HighlightAnchor(blockId, inlinePath, offset)
    }

    /**
     * Extracts a clock map from merged fields.
     */
    private fun extractClockFromFields(fields: Map<String, JsonElement>): Map<String, Long> {
        val clockElement = fields["clock"] as? JsonObject ?: return emptyMap()
        return clockElement.mapNotNull { (key, value) ->
            val count = value.jsonPrimitive.longOrNull
            if (count != null) key to count else null
        }.toMap()
    }

    private fun parseYabaColor(element: JsonElement?): YabaColor {
        val primitive = element as? JsonPrimitive
        val intValue = primitive?.longOrNull?.toInt()
        if (intValue != null) return YabaColor.fromCode(intValue)
        return YabaColor.fromRoleString(primitive?.contentOrNull)
    }
}
