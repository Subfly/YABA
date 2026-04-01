package dev.subfly.yaba.core.database

import android.content.Context
import androidx.room3.Room
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
import kotlinx.coroutines.Dispatchers
import kotlin.concurrent.Volatile

/**
 * Single entry point for the shared Room database and all DAOs.
 *
 * Call [initialize] once (pass Android context when needed) and use the exposed
 * properties everywhere instead of threading dependencies through constructors.
 *
 * Room is the single source of truth for metadata.
 */

object DatabaseProvider {
    @Volatile
    private var databaseRef: YabaDatabase? = null

    fun initialize(context: Context) {
        if (databaseRef == null) {
            val databasePath = context.applicationContext.getDatabasePath(YABA_DATABASE_FILE_NAME)
            databasePath.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            databaseRef =  Room.databaseBuilder(
                context.applicationContext,
                YabaDatabase::class.java,
                databasePath.absolutePath,
            ).setQueryCoroutineContext(Dispatchers.IO).build()
        }
    }

    private fun database(): YabaDatabase =
        databaseRef ?: error("DatabaseProvider.initialize() must be called before use")

    val database: YabaDatabase
        get() = database()

    val folderDao: FolderDao
        get() = database().folderDao()

    val tagDao: TagDao
        get() = database().tagDao()

    val bookmarkDao: BookmarkDao
        get() = database().bookmarkDao()

    val linkBookmarkDao: LinkBookmarkDao
        get() = database().linkBookmarkDao()

    val imageBookmarkDao: ImageBookmarkDao
        get() = database().imageBookmarkDao()

    val docBookmarkDao: DocBookmarkDao
        get() = database().docBookmarkDao()

    val noteBookmarkDao: NoteBookmarkDao
        get() = database().noteBookmarkDao()

    val tagBookmarkDao: TagBookmarkDao
        get() = database().tagBookmarkDao()

    val readableVersionDao: ReadableVersionDao
        get() = database().readableVersionDao()

    val annotationDao: AnnotationDao
        get() = database().annotationDao()
}
