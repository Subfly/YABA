@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.events.EventsDatabaseProvider
import dev.subfly.yabacore.database.events.toEvents
import dev.subfly.yabacore.filesystem.EntityFileManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
 * If filesystem clock >= all event clocks for an object, delete events for that object
 *
 * **Rule B: Tombstone Dominance**
 * If `deleted.json` exists, delete all events for that object
 *
 * **Rule C: Size Threshold**
 * If events.sqlite exceeds limit, compact eligible objects
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
     *
     * @return Number of events removed
     */
    suspend fun compact(): Long {
        var removedCount = 0L

        val objectIds = eventsDao.getObjectIdsWithEvents()

        objectIds.forEach { objectId ->
            val uuid = runCatching { Uuid.parse(objectId) }.getOrNull() ?: return@forEach
            val events = eventsDao.getEventsForObject(objectId).toEvents()
            if (events.isEmpty()) return@forEach

            val objectType = events.first().objectType

            // Rule B: Tombstone Dominance
            val isDeleted = when (objectType) {
                ObjectType.FOLDER -> entityFileManager.isFolderDeleted(uuid)
                ObjectType.TAG -> entityFileManager.isTagDeleted(uuid)
                ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(uuid)
            }

            if (isDeleted) {
                eventsDao.deleteEventsForObject(objectId)
                removedCount += events.size
                return@forEach
            }

            // Rule A: Snapshot Dominance
            val fileSystemClock = getFileSystemClock(objectType, uuid)
            if (fileSystemClock != null) {
                val allEventsAreOlder = events.all { event ->
                    fileSystemClock.isNewerOrEqual(event.clock)
                }

                if (allEventsAreOlder) {
                    eventsDao.deleteEventsForObject(objectId)
                    removedCount += events.size
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
     *
     * @return Number of events removed
     */
    suspend fun compactObject(objectId: String): Long {
        val uuid = runCatching { Uuid.parse(objectId) }.getOrNull() ?: return 0L
        val events = eventsDao.getEventsForObject(objectId).toEvents()
        if (events.isEmpty()) return 0L

        val objectType = events.first().objectType

        // Rule B: Tombstone Dominance
        val isDeleted = when (objectType) {
            ObjectType.FOLDER -> entityFileManager.isFolderDeleted(uuid)
            ObjectType.TAG -> entityFileManager.isTagDeleted(uuid)
            ObjectType.BOOKMARK -> entityFileManager.isBookmarkDeleted(uuid)
        }

        if (isDeleted) {
            eventsDao.deleteEventsForObject(objectId)
            return events.size.toLong()
        }

        // Rule A: Snapshot Dominance
        val fileSystemClock = getFileSystemClock(objectType, uuid)
        if (fileSystemClock != null) {
            val allEventsAreOlder = events.all { event ->
                fileSystemClock.isNewerOrEqual(event.clock)
            }

            if (allEventsAreOlder) {
                eventsDao.deleteEventsForObject(objectId)
                return events.size.toLong()
            }
        }

        return 0L
    }

    /**
     * Gets statistics about the event log.
     */
    suspend fun getStats(): LogStats {
        val totalEvents = eventsDao.getEventCount()
        val objectCount = eventsDao.getObjectIdsWithEvents().size.toLong()
        return LogStats(
            totalEvents = totalEvents,
            objectCount = objectCount,
            needsCompaction = totalEvents > config.maxEventCount,
        )
    }

    // ==================== Private Helpers ====================

    private suspend fun getFileSystemClock(objectType: ObjectType, uuid: Uuid): VectorClock? {
        return when (objectType) {
            ObjectType.FOLDER -> {
                entityFileManager.readFolderMeta(uuid)?.let { VectorClock.fromMap(it.clock) }
            }
            ObjectType.TAG -> {
                entityFileManager.readTagMeta(uuid)?.let { VectorClock.fromMap(it.clock) }
            }
            ObjectType.BOOKMARK -> {
                // For bookmarks, we need to consider both meta.json and link.json clocks
                val metaClock = entityFileManager.readBookmarkMeta(uuid)?.let { VectorClock.fromMap(it.clock) }
                val linkClock = entityFileManager.readLinkJson(uuid)?.let { VectorClock.fromMap(it.clock) }

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
    val objectCount: Long,
    val needsCompaction: Boolean,
)
