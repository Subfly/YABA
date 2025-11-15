package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.subfly.yabacore.database.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<BookmarkEntity>)

    @Update
    suspend fun update(entity: BookmarkEntity)

    @Update
    suspend fun updateAll(entities: List<BookmarkEntity>)

    @Delete
    suspend fun delete(entity: BookmarkEntity)

    @Delete
    suspend fun deleteAll(entities: List<BookmarkEntity>)

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookmarkEntity?

    @Query("SELECT * FROM bookmarks ORDER BY editedAt DESC")
    fun getAllFlow(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY editedAt DESC")
    fun getForFolderFlow(folderId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY editedAt DESC")
    suspend fun getForFolderList(folderId: String): List<BookmarkEntity>

    @Query(
        "SELECT b.* FROM bookmarks b " +
                "INNER JOIN bookmark_tag_cross_ref r ON r.bookmarkId = b.id " +
                "WHERE r.tagId = :tagId ORDER BY b.editedAt DESC"
    )
    fun getForTagFlow(tagId: String): Flow<List<BookmarkEntity>>

    @Query(
        "SELECT * FROM bookmarks WHERE " +
                "title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' " +
                "ORDER BY editedAt DESC"
    )
    fun searchFlow(query: String): Flow<List<BookmarkEntity>>

    @Transaction
    suspend fun replaceAllForFolder(folderId: String, entities: List<BookmarkEntity>) {
        // Note: call-site should handle consistency; included here for convenience.
        // This method clears nothing by itself, it's just a batch insert/update path.
        insertAll(entities)
    }
}
