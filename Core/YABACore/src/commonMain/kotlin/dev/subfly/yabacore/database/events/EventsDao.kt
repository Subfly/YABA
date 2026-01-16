package dev.subfly.yabacore.database.events

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for CRDT events stored in events.sqlite.
 */
@Dao
interface EventsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CRDTEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<CRDTEventEntity>)

    @Query("SELECT * FROM crdt_events WHERE objectId = :objectId ORDER BY timestamp ASC")
    suspend fun getEventsForObject(objectId: String): List<CRDTEventEntity>

    @Query("SELECT * FROM crdt_events WHERE objectId = :objectId AND objectType = :objectType ORDER BY timestamp ASC")
    suspend fun getEventsForObjectOfType(objectId: String, objectType: String): List<CRDTEventEntity>

    @Query("SELECT * FROM crdt_events ORDER BY timestamp ASC")
    suspend fun getAllEvents(): List<CRDTEventEntity>

    @Query("SELECT * FROM crdt_events WHERE timestamp > :afterTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsAfterTimestamp(afterTimestamp: Long): List<CRDTEventEntity>

    @Query("DELETE FROM crdt_events WHERE objectId = :objectId")
    suspend fun deleteEventsForObject(objectId: String)

    @Query("DELETE FROM crdt_events WHERE eventId IN (:eventIds)")
    suspend fun deleteEventsByIds(eventIds: List<String>)

    @Query("DELETE FROM crdt_events")
    suspend fun deleteAllEvents()

    @Query("SELECT COUNT(*) FROM crdt_events")
    suspend fun getEventCount(): Long

    @Query("SELECT DISTINCT objectId FROM crdt_events")
    suspend fun getObjectIdsWithEvents(): List<String>

    @Query("SELECT * FROM crdt_events WHERE objectType = :objectType ORDER BY timestamp ASC")
    suspend fun getEventsByObjectType(objectType: String): List<CRDTEventEntity>
}
