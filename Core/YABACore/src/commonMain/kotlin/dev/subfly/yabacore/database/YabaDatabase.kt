package dev.subfly.yabacore.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import dev.subfly.yabacore.database.converters.CoreTypeConverters
import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.dao.HighlightDao
import dev.subfly.yabacore.database.dao.LinkBookmarkDao
import dev.subfly.yabacore.database.dao.ReadableAssetDao
import dev.subfly.yabacore.database.dao.ReadableVersionDao
import dev.subfly.yabacore.database.dao.TagBookmarkDao
import dev.subfly.yabacore.database.dao.TagDao
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.HighlightEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity

const val YABA_DATABASE_VERSION = 2
const val YABA_DATABASE_FILE_NAME = "yaba.db"

@Suppress("KotlinNoActualForExpect")
internal expect object YabaDatabaseCtor : RoomDatabaseConstructor<YabaDatabase> {
    override fun initialize(): YabaDatabase
}

@Database(
    entities = [
        BookmarkEntity::class,
        FolderEntity::class,
        TagEntity::class,
        TagBookmarkCrossRef::class,
        LinkBookmarkEntity::class,
        ReadableVersionEntity::class,
        ReadableAssetEntity::class,
        HighlightEntity::class,
    ],
    version = YABA_DATABASE_VERSION,
    exportSchema = true,
)
@ConstructedBy(YabaDatabaseCtor::class)
@TypeConverters(CoreTypeConverters::class)
abstract class YabaDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun linkBookmarkDao(): LinkBookmarkDao
    abstract fun tagBookmarkDao(): TagBookmarkDao
    abstract fun readableVersionDao(): ReadableVersionDao
    abstract fun readableAssetDao(): ReadableAssetDao
    abstract fun highlightDao(): HighlightDao
}
