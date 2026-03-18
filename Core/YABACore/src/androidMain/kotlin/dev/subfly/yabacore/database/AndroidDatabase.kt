package dev.subfly.yabacore.database

import android.content.Context
import androidx.room3.ExperimentalRoomApi
import androidx.room3.Room
import kotlinx.coroutines.Dispatchers

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
    ).setQueryCoroutineContext(Dispatchers.IO).build()
}
