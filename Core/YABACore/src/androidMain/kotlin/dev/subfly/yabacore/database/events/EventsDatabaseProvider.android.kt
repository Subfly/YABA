package dev.subfly.yabacore.database.events

import android.content.Context
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual fun buildEventsDatabase(platformContext: Any?): EventsDatabase {
    val context =
        platformContext as? Context
            ?: error(
                "Android EventsDatabaseProvider.initialize requires an android.content.Context"
            )
    return context.createEventsDatabase()
}

@OptIn(ExperimentalRoomApi::class)
fun Context.createEventsDatabase(
    databaseName: String = EVENTS_DATABASE_FILE_NAME,
): EventsDatabase {
    val databasePath = applicationContext.getDatabasePath(databaseName)
    databasePath.parentFile?.let { parent ->
        if (!parent.exists()) {
            parent.mkdirs()
        }
    }

    return Room.databaseBuilder(
        applicationContext,
        EventsDatabase::class.java,
        databasePath.absolutePath,
    ).setDriver(BundledSQLiteDriver()).build()
}
