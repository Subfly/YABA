package dev.subfly.yabacore.database

import android.content.Context
import androidx.room.ExperimentalRoomApi
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

@OptIn(ExperimentalRoomApi::class)
fun Context.createYabaDatabase(
    databaseName: String = YABA_DATABASE_FILE_NAME,
): YabaDatabase {
    val databasePath = applicationContext.getDatabasePath(databaseName)
    databasePath.parentFile?.let { parent ->
        if (!parent.exists()) {
            parent.mkdirs()
        }
    }

    return Room.databaseBuilder(
        applicationContext,
        YabaDatabase::class.java,
        databasePath.absolutePath,
    ).setDriver(BundledSQLiteDriver()).build()
}
