package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkBookmarkDao {
    @Upsert
    suspend fun upsert(entity: LinkBookmarkEntity)

    @Query("DELETE FROM link_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM link_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM link_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): LinkBookmarkEntity?

    @Query("SELECT * FROM link_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<LinkBookmarkEntity?>
}
