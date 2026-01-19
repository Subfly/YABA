package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEvents
import dev.subfly.yabacore.filesystem.EntityFileManager

/**
 * Configuration for log compaction.
 */
data class CompactionConfig(
    /** Maximum number of events before triggering compaction */
    val maxEventCount: Long = 10_000,
    /** Whether to compact after each sync */
    val compactOnSync: Boolean = true,
)

/**
 * Log compaction for CRDT events.
 *
 * Compaction rules from the spec:
 *
 * **Rule A: Snapshot Dominance**
 * If filesystem clock >= all event clocks for an object, delete CREATE/UPDATE events for that object.
 * DELETE events are NEVER deleted.
 *
 * **Rule B: Delete Events Preserved**
 * DELETE events are never compacted or removed. They must be preserved forever to prevent resurrection.
 *
 * **Rule C: Size Threshold**
 * If events.sqlite exceeds limit, compact eligible objects (but preserve DELETE events).
 */
object LogCompaction {
    private val eventsDao get() = EventsDatabaseProvider.eventsDao
    private val entityFileManager get() = EntityFileManager

    private var config = CompactionConfig()

    /**
     * Sets the compaction configuration.
     */
    fun configure(newConfig: CompactionConfig) {
        config = newConfig
    }

    /**
     * Runs log compaction, applying all compaction rules.
     * DELETE events are NEVER removed.
     *
     * @return Number of events removed
     */
    suspend fun compact(): Long {
        var removedCount = 0L

        val objectIds = eventsDao.getObjectIdsWithEvents()

        objectIds.forEach { objectId ->
            val events = eventsDao.getEventsForObject(objectId).toEvents()
            if (events.isEmpty()) return@forEach

            val objectType = events.first().objectType

            // Separate DELETE events (never removed) from other events
            val deleteEvents = events.filter { it.isDelete() }
            val nonDeleteEvents = events.filterNot { it.isDelete() }

            if (nonDeleteEvents.isEmpty()) {
                // Only DELETE events exist, nothing to compact
                return@forEach
            }

            // Check if entity is deleted on filesystem
            val isDeletedOnFilesystem = when (objectType) {
                ObjectType.FOLDER -> entityFileManager.isFolderDeleted(objectId)
                ObjectType.TAG -> entityFileManager.isTagDeleted(objectId)
                ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(objectId)
            }

            if (isDeletedOnFilesystem || deleteEvents.isNotEmpty()) {
                // Entity is deleted - remove only non-DELETE events
                val nonDeleteEventIds = nonDeleteEvents.map { it.eventId }
                if (nonDeleteEventIds.isNotEmpty()) {
                    eventsDao.deleteEventsByIds(nonDeleteEventIds)
                    removedCount += nonDeleteEventIds.size
                }
                return@forEach
            }

            // Rule A: Snapshot Dominance (only for non-deleted entities)
            val fileSystemClock = getFileSystemClock(objectType, objectId)
            if (fileSystemClock != null) {
                val eligibleForRemoval = nonDeleteEvents.filter { event ->
                    fileSystemClock.isNewerOrEqual(event.clock)
                }

                if (eligibleForRemoval.isNotEmpty()) {
                    val eventIdsToRemove = eligibleForRemoval.map { it.eventId }
                    eventsDao.deleteEventsByIds(eventIdsToRemove)
                    removedCount += eventIdsToRemove.size
                }
            }
        }

        return removedCount
    }

    /**
     * Checks if compaction is needed based on event count threshold.
     */
    suspend fun isCompactionNeeded(): Boolean {
        val eventCount = eventsDao.getEventCount()
        return eventCount > config.maxEventCount
    }

    /**
     * Runs compaction if needed based on the size threshold.
     *
     * @return Number of events removed, or 0 if compaction wasn't needed
     */
    suspend fun compactIfNeeded(): Long {
        return if (isCompactionNeeded()) {
            compact()
        } else {
            0L
        }
    }

    /**
     * Compacts events for a specific object.
     * DELETE events are NEVER removed.
     *
     * @return Number of events removed
     */
    suspend fun compactObject(objectId: String): Long {
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return 0L

        val objectType = events.first().objectType

        // Separate DELETE events (never removed) from other events
        val deleteEvents = events.filter { it.isDelete() }
        val nonDeleteEvents = events.filterNot { it.isDelete() }

        if (nonDeleteEvents.isEmpty()) {
            // Only DELETE events exist, nothing to compact
            return 0L
        }

        // Check if entity is deleted on filesystem
        val isDeletedOnFilesystem = when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(objectId)
            ObjectType.TAG -> entityFileManager.isTagDeleted(objectId)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(objectId)
        }

        if (isDeletedOnFilesystem || deleteEvents.isNotEmpty()) {
            // Entity is deleted - remove only non-DELETE events
            val nonDeleteEventIds = nonDeleteEvents.map { it.eventId }
            if (nonDeleteEventIds.isNotEmpty()) {
                eventsDao.deleteEventsByIds(nonDeleteEventIds)
                return nonDeleteEventIds.size.toLong()
            }
            return 0L
        }

        // Rule A: Snapshot Dominance
        val fileSystemClock = getFileSystemClock(objectType, objectId)
        if (fileSystemClock != null) {
            val eligibleForRemoval = nonDeleteEvents.filter { event ->
                fileSystemClock.isNewerOrEqual(event.clock)
            }

            if (eligibleForRemoval.isNotEmpty()) {
                val eventIdsToRemove = eligibleForRemoval.map { it.eventId }
                eventsDao.deleteEventsByIds(eventIdsToRemove)
                return eventIdsToRemove.size.toLong()
            }
        }

        return 0L
    }

    /**
     * Gets statistics about the event log.
     */
    suspend fun getStats(): LogStats {
        val allEvents = eventsDao.getAllEvents().toEvents()
        val deleteEventCount = allEvents.count { it.isDelete() }.toLong()
        val totalEvents = allEvents.size.toLong()
        val objectCount = eventsDao.getObjectIdsWithEvents().size.toLong()
        return LogStats(
            totalEvents = totalEvents,
            deleteEventCount = deleteEventCount,
            objectCount = objectCount,
            needsCompaction = totalEvents > config.maxEventCount,
        )
    }

    // ==================== Private Helpers ====================

    private suspend fun getFileSystemClock(objectType: ObjectType, entityId: String): VectorClock? {
        return when (objectType) {
            ObjectType.FOLDER -> {
                entityFileManager.readFolderMeta(entityId)?.let { VectorClock.fromMap(it.clock) }
            }
            ObjectType.TAG -> {
                entityFileManager.readTagMeta(entityId)?.let { VectorClock.fromMap(it.clock) }
            }
            ObjectType.BOOKMARK -> {
                // For bookmarks, we need to consider both meta.json and link.json clocks
                val metaClock = entityFileManager.readBookmarkMeta(entityId)?.let { VectorClock.fromMap(it.clock) }
                val linkClock = entityFileManager.readLinkJson(entityId)?.let { VectorClock.fromMap(it.clock) }

                when {
                    metaClock != null && linkClock != null -> metaClock.merge(linkClock)
                    metaClock != null -> metaClock
                    linkClock != null -> linkClock
                    else -> null
                }
            }
        }
    }
}

/**
 * Statistics about the event log.
 */
data class LogStats(
    val totalEvents: Long,
    val deleteEventCount: Long,
    val objectCount: Long,
    val needsCompaction: Boolean,
)
