package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.ImageBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageBookmarkDao {
    @Upsert
    suspend fun upsert(entity: ImageBookmarkEntity)

    @Query("DELETE FROM image_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM image_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM image_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): ImageBookmarkEntity?

    @Query("SELECT * FROM image_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<ImageBookmarkEntity?>
}
