package dev.subfly.yabacore.database.events

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun buildEventsDatabase(platformContext: Any?): EventsDatabase = createEventsDatabase()

fun createEventsDatabase(
    databaseName: String = EVENTS_DATABASE_FILE_NAME,
): EventsDatabase {
    val osName = System.getProperty("os.name").lowercase()
    val dbDirPath =
        when {
            osName.contains("win") -> {
                val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
                "$appData${File.separator}YABA"
            }

            osName.contains("mac") -> {
                val home = System.getProperty("user.home")
                "$home${File.separator}Library${File.separator}Application Support${File.separator}YABA"
            }

            else -> {
                val home = System.getProperty("user.home")
                "$home${File.separator}.local${File.separator}share${File.separator}YABA"
            }
        }

    val dbDir = File(dbDirPath)
    if (!dbDir.exists()) {
        dbDir.mkdirs()
    }

    val dbPath = File(dbDir, databaseName).absolutePath

    return Room.databaseBuilder<EventsDatabase>(dbPath).setDriver(BundledSQLiteDriver()).build()
}
