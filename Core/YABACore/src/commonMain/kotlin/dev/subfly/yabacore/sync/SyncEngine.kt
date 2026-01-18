package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.CacheRebuilder
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEvents
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.FileSystemStateManager
import dev.subfly.yabacore.filesystem.SyncState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
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
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return

        val objectType = events.first().objectType

        // Check if entity is already deleted on filesystem
        val isDeletedOnFilesystem = when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(objectId)
            ObjectType.TAG -> entityFileManager.isTagDeleted(objectId)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(objectId)
        }

        if (isDeletedOnFilesystem) {
            // Entity is already deleted on filesystem, just clear events
            eventsDao.deleteEventsForObject(objectId)
            return
        }

        // Check if events contain a winning _deleted=true event
        // This handles the case where deletion arrives via events before filesystem sync
        val metaEvents = events.filter { it.file == FileTarget.META_JSON }
        val deletedEvents = metaEvents.filter { it.field == "_deleted" }
        if (deletedEvents.isNotEmpty()) {
            val resolved = crdtEngine.resolveFieldFromEvents(deletedEvents)
            val isDeleted = resolved.value.jsonPrimitive.booleanOrNull == true
            if (isDeleted) {
                // Deletion event wins - apply deletion to filesystem and index
                applyDeletionFromEvent(objectType, objectId, resolved.winningClock)
                eventsDao.deleteEventsForObject(objectId)
                return
            }
        }

        when (objectType) {
            ObjectType.FOLDER -> mergeFolderEvents(objectId, events)
            ObjectType.TAG -> mergeTagEvents(objectId, events)
            ObjectType.BOOKMARK -> mergeBookmarkEvents(objectId, events)
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
            }
        }
    }

    private suspend fun mergeFolderEvents(folderId: String, events: List<CRDTEvent>) {
        val existingJson = entityFileManager.readFolderMeta(folderId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // Group events by field and resolve each
        val metaEvents = events.filter { it.file == FileTarget.META_JSON }
        val resolvedFields = mutableMapOf<String, JsonElement>()
        var mergedClock = existingClock

        metaEvents.groupBy { it.field }.forEach { (field, fieldEvents) ->
            val resolved = crdtEngine.resolveFieldFromEvents(fieldEvents)
            resolvedFields[field] = resolved.value
            mergedClock = mergedClock.merge(resolved.winningClock)
        }

        if (existingJson != null) {
            // Apply resolved fields to existing JSON
            val updatedJson = existingJson.copy(
                parentId = resolvedFields["parentId"]?.jsonPrimitive?.contentOrNull ?: existingJson.parentId,
                label = resolvedFields["label"]?.jsonPrimitive?.contentOrNull ?: existingJson.label,
                description = resolvedFields["description"]?.jsonPrimitive?.contentOrNull ?: existingJson.description,
                icon = resolvedFields["icon"]?.jsonPrimitive?.contentOrNull ?: existingJson.icon,
                colorCode = resolvedFields["colorCode"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.colorCode,
                order = resolvedFields["order"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.order,
                editedAt = resolvedFields["editedAt"]?.jsonPrimitive?.longOrNull ?: existingJson.editedAt,
                clock = mergedClock.toMap(),
            )
            entityFileManager.writeFolderMeta(updatedJson)
        }

        // Events are NOT deleted immediately - LogCompaction will clean them up
        // using "snapshot dominance" rule once the filesystem clock is newer.
        // This allows events to remain available for peer sync until compaction.
    }

    private suspend fun mergeTagEvents(tagId: String, events: List<CRDTEvent>) {
        val existingJson = entityFileManager.readTagMeta(tagId)
        val existingClock = existingJson?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        val metaEvents = events.filter { it.file == FileTarget.META_JSON }
        val resolvedFields = mutableMapOf<String, JsonElement>()
        var mergedClock = existingClock

        metaEvents.groupBy { it.field }.forEach { (field, fieldEvents) ->
            val resolved = crdtEngine.resolveFieldFromEvents(fieldEvents)
            resolvedFields[field] = resolved.value
            mergedClock = mergedClock.merge(resolved.winningClock)
        }

        if (existingJson != null) {
            val updatedJson = existingJson.copy(
                label = resolvedFields["label"]?.jsonPrimitive?.contentOrNull ?: existingJson.label,
                icon = resolvedFields["icon"]?.jsonPrimitive?.contentOrNull ?: existingJson.icon,
                colorCode = resolvedFields["colorCode"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.colorCode,
                order = resolvedFields["order"]?.jsonPrimitive?.longOrNull?.toInt() ?: existingJson.order,
                editedAt = resolvedFields["editedAt"]?.jsonPrimitive?.longOrNull ?: existingJson.editedAt,
                clock = mergedClock.toMap(),
            )
            entityFileManager.writeTagMeta(updatedJson)
        }

        // Events are NOT deleted immediately - LogCompaction will clean them up
        // using "snapshot dominance" rule once the filesystem clock is newer.
    }

    private suspend fun mergeBookmarkEvents(bookmarkId: String, events: List<CRDTEvent>) {
        val existingMeta = entityFileManager.readBookmarkMeta(bookmarkId)
        val existingLink = entityFileManager.readLinkJson(bookmarkId)
        val existingMetaClock = existingMeta?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()
        val existingLinkClock = existingLink?.let { VectorClock.fromMap(it.clock) } ?: VectorClock.empty()

        // Process meta.json events
        val metaEvents = events.filter { it.file == FileTarget.META_JSON }
        val metaFields = mutableMapOf<String, JsonElement>()
        var mergedMetaClock = existingMetaClock

        metaEvents.groupBy { it.field }.forEach { (field, fieldEvents) ->
            val resolved = crdtEngine.resolveFieldFromEvents(fieldEvents)
            metaFields[field] = resolved.value
            mergedMetaClock = mergedMetaClock.merge(resolved.winningClock)
        }

        // Process link.json events
        val linkEvents = events.filter { it.file == FileTarget.LINK_JSON }
        val linkFields = mutableMapOf<String, JsonElement>()
        var mergedLinkClock = existingLinkClock

        linkEvents.groupBy { it.field }.forEach { (field, fieldEvents) ->
            val resolved = crdtEngine.resolveFieldFromEvents(fieldEvents)
            linkFields[field] = resolved.value
            mergedLinkClock = mergedLinkClock.merge(resolved.winningClock)
        }

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
        // using "snapshot dominance" rule once the filesystem clock is newer.
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
}
