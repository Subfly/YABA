package dev.subfly.yabacore.database

import androidx.room.Database
import androidx.room.RoomDatabase

const val YABA_DATABASE_VERSION = 1
const val YABA_DATABASE_FILE_NAME = "yaba.db"

@Database(
    entities = [],
    version = YABA_DATABASE_VERSION,
    exportSchema = true,
)
abstract class YabaDatabase : RoomDatabase()
