@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.CacheRebuilder
import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEvents
import dev.subfly.yabacore.filesystem.EntityFileManager
import dev.subfly.yabacore.filesystem.FileSystemStateManager
import dev.subfly.yabacore.filesystem.SyncState
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
     * 4. Update SQLite cache
     */
    suspend fun incrementalMerge() {
        FileSystemStateManager.setSyncState(SyncState.SYNCING)
        try {
            val objectIds = eventsDao.getObjectIdsWithEvents()

            objectIds.forEach { objectId ->
                mergeObjectEvents(objectId)
            }

            // Compact logs after merge
            LogCompaction.compactIfNeeded()

            FileSystemStateManager.setSyncState(SyncState.IN_SYNC)
        } catch (e: Exception) {
            FileSystemStateManager.setSyncState(SyncState.SYNC_FAILED)
            throw e
        }
    }

    /**
     * Receives events from a remote peer and applies them.
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
        val uuid = runCatching { Uuid.parse(objectId) }.getOrNull() ?: return

        // Check if entity is deleted
        val isDeleted = when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(uuid)
            ObjectType.TAG -> entityFileManager.isTagDeleted(uuid)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(uuid)
        }

        if (isDeleted) {
            // Entity is deleted, no need to merge, just clear events
            eventsDao.deleteEventsForObject(objectId)
            return
        }

        when (objectType) {
            ObjectType.FOLDER -> mergeFolderEvents(uuid, events)
            ObjectType.TAG -> mergeTagEvents(uuid, events)
            ObjectType.BOOKMARK -> mergeBookmarkEvents(uuid, events)
        }
    }

    private suspend fun mergeFolderEvents(folderId: Uuid, events: List<CRDTEvent>) {
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

        // Clear processed events
        eventsDao.deleteEventsForObject(folderId.toString())
    }

    private suspend fun mergeTagEvents(tagId: Uuid, events: List<CRDTEvent>) {
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

        eventsDao.deleteEventsForObject(tagId.toString())
    }

    private suspend fun mergeBookmarkEvents(bookmarkId: Uuid, events: List<CRDTEvent>) {
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
                clock = mergedMetaClock.toMap(),
            )
            entityFileManager.writeBookmarkMeta(updatedMeta)
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

        eventsDao.deleteEventsForObject(bookmarkId.toString())
    }
}
