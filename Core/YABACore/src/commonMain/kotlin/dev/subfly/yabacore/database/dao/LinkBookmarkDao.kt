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

    @Upsert
    suspend fun upsertAll(entities: List<LinkBookmarkEntity>)

    @Delete
    suspend fun delete(entity: LinkBookmarkEntity)

    @Query("DELETE FROM link_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM link_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM link_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): LinkBookmarkEntity?

    /**
     * Observes link details for a specific bookmark.
     * Returns a Flow that emits the current link entity or null if not found.
     */
    @Query("SELECT * FROM link_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<LinkBookmarkEntity?>
}
