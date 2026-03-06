package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.notifications.NotificationManager
/**
 * Global data operations (e.g., wipe all data).
 *
 * Uses Room as the single source of truth. No filesystem metadata.
 */
object GlobalDataManager {
    private val folderDao get() = DatabaseProvider.folderDao
    private val tagDao get() = DatabaseProvider.tagDao
    private val bookmarkDao get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao get() = DatabaseProvider.tagBookmarkDao
    private val highlightDao get() = DatabaseProvider.highlightDao
    private val readableVersionDao get() = DatabaseProvider.readableVersionDao
    private val readableAssetDao get() = DatabaseProvider.readableAssetDao

    /**
     * Wipes all local data and bookmark folders.
     * Clears Room and deletes all bookmark content directories.
     */
    suspend fun wipeAll() {
        NotificationManager.cancelAllReminders()

        val bookmarkIds = bookmarkDao.getAll().map { it.id }
        bookmarkIds.forEach { bookmarkId ->
            BookmarkFileManager.deleteBookmarkFolder(bookmarkId)
        }

        tagBookmarkDao.deleteAll()
        highlightDao.deleteAll()
        readableVersionDao.deleteAll()
        readableAssetDao.deleteAll()
        linkBookmarkDao.deleteAll()
        bookmarkDao.deleteAll()
        tagDao.deleteAll()
        folderDao.deleteAll()
    }
}
