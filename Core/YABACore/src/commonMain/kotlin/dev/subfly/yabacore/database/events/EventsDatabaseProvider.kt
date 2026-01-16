package dev.subfly.yabacore.database.events

import kotlin.concurrent.Volatile

/**
 * Builds the EventsDatabase instance for the current platform.
 */
expect fun buildEventsDatabase(platformContext: Any? = null): EventsDatabase

/**
 * Singleton provider for the events database.
 *
 * Call [initialize] once at app startup (pass Android context if needed).
 */
object EventsDatabaseProvider {
    @Volatile
    private var databaseRef: EventsDatabase? = null

    fun initialize(platformContext: Any? = null) {
        if (databaseRef == null) {
            databaseRef = buildEventsDatabase(platformContext)
        }
    }

    private fun database(): EventsDatabase =
        databaseRef ?: error("EventsDatabaseProvider.initialize() must be called before use")

    val database: EventsDatabase
        get() = database()

    val eventsDao: EventsDao
        get() = database().eventsDao()
}
