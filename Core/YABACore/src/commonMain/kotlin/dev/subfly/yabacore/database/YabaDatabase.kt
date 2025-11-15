package dev.subfly.yabacore.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.subfly.yabacore.database.converters.InstantConverters
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.BookmarkTagCrossRef
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.entities.TombstoneEntity

const val YABA_DATABASE_VERSION = 1
const val YABA_DATABASE_FILE_NAME = "yaba.db"

@Database(
    entities =
        [
            BookmarkEntity::class,
            FolderEntity::class,
            TagEntity::class,
            BookmarkTagCrossRef::class,
            TombstoneEntity::class,
        ],
    version = YABA_DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class YabaDatabase : RoomDatabase()
