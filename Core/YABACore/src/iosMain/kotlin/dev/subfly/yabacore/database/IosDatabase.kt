package dev.subfly.yabacore.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

fun createYabaDatabase(
    databaseName: String = YABA_DATABASE_FILE_NAME,
): YabaDatabase {
    val dbFile = documentDirectory() + "/$databaseName"

    return Room.databaseBuilder<YabaDatabase>(dbFile).setDriver(BundledSQLiteDriver()).build()
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(documentDirectory?.path)
}
