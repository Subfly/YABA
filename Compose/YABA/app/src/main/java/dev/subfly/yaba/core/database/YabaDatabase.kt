package dev.subfly.yaba.core.database

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import dev.subfly.yaba.core.database.converters.CoreTypeConverters
import dev.subfly.yaba.core.database.dao.BookmarkDao
import dev.subfly.yaba.core.database.dao.DocBookmarkDao
import dev.subfly.yaba.core.database.dao.FolderDao
import dev.subfly.yaba.core.database.dao.AnnotationDao
import dev.subfly.yaba.core.database.dao.ImageBookmarkDao
import dev.subfly.yaba.core.database.dao.LinkBookmarkDao
import dev.subfly.yaba.core.database.dao.NoteBookmarkDao
import dev.subfly.yaba.core.database.dao.ReadableVersionDao
import dev.subfly.yaba.core.database.dao.TagBookmarkDao
import dev.subfly.yaba.core.database.dao.TagDao
import dev.subfly.yaba.core.database.entities.BookmarkEntity
import dev.subfly.yaba.core.database.entities.DocBookmarkEntity
import dev.subfly.yaba.core.database.entities.FolderEntity
import dev.subfly.yaba.core.database.entities.AnnotationEntity
import dev.subfly.yaba.core.database.entities.ImageBookmarkEntity
import dev.subfly.yaba.core.database.entities.LinkBookmarkEntity
import dev.subfly.yaba.core.database.entities.NoteBookmarkEntity
import dev.subfly.yaba.core.database.entities.ReadableVersionEntity
import dev.subfly.yaba.core.database.entities.TagBookmarkCrossRef
import dev.subfly.yaba.core.database.entities.TagEntity

const val YABA_DATABASE_VERSION = 1
const val YABA_DATABASE_FILE_NAME = "yaba.db"

@Database(
    entities = [
        BookmarkEntity::class,
        FolderEntity::class,
        TagEntity::class,
        TagBookmarkCrossRef::class,
        LinkBookmarkEntity::class,
        ImageBookmarkEntity::class,
        DocBookmarkEntity::class,
        NoteBookmarkEntity::class,
        ReadableVersionEntity::class,
        AnnotationEntity::class,
    ],
    version = YABA_DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(CoreTypeConverters::class)
abstract class YabaDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun linkBookmarkDao(): LinkBookmarkDao
    abstract fun imageBookmarkDao(): ImageBookmarkDao
    abstract fun docBookmarkDao(): DocBookmarkDao
    abstract fun noteBookmarkDao(): NoteBookmarkDao
    abstract fun tagBookmarkDao(): TagBookmarkDao
    abstract fun readableVersionDao(): ReadableVersionDao
    abstract fun annotationDao(): AnnotationDao
}
