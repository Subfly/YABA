package dev.subfly.yabacore.database

import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.dao.LinkBookmarkDao
import dev.subfly.yabacore.database.dao.TagBookmarkDao
import dev.subfly.yabacore.database.dao.TagDao
import kotlin.concurrent.Volatile

/**
 * Single entry point for the shared Room database and all DAOs.
 *
 * Call [initialize] once (pass Android context when needed) and use the exposed
 * properties everywhere instead of threading dependencies through constructors.
 *
 * Note: This database is a derived cache. The filesystem JSON files are the
 * authoritative source of truth. Use [CacheRebuilder] to rebuild from filesystem.
 */
expect fun buildDatabase(platformContext: Any? = null): YabaDatabase

object DatabaseProvider {
    @Volatile
    private var databaseRef: YabaDatabase? = null

    fun initialize(platformContext: Any? = null) {
        if (databaseRef == null) {
            databaseRef = buildDatabase(platformContext)
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

    val tagBookmarkDao: TagBookmarkDao
        get() = database().tagBookmarkDao()
}
