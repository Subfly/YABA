package dev.subfly.yabacore.database.events

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

const val EVENTS_DATABASE_VERSION = 1
const val EVENTS_DATABASE_FILE_NAME = "events.db"

@Suppress("KotlinNoActualForExpect")
internal expect object EventsDatabaseCtor : RoomDatabaseConstructor<EventsDatabase> {
    override fun initialize(): EventsDatabase
}

/**
 * Separate Room database for CRDT events.
 *
 * This database stores the event log used for incremental sync.
 * It is part of the acceleration layer and can be deleted and rebuilt
 * (though this would require re-syncing events from peers).
 *
 * The filesystem JSON files remain the authoritative source of truth.
 */
@Database(
    entities = [CRDTEventEntity::class],
    version = EVENTS_DATABASE_VERSION,
    exportSchema = true,
)
@ConstructedBy(EventsDatabaseCtor::class)
abstract class EventsDatabase : RoomDatabase() {
    abstract fun eventsDao(): EventsDao
}
