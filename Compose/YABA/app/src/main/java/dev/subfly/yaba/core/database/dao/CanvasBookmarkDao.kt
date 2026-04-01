package dev.subfly.yaba.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.subfly.yaba.core.database.entities.CanvasBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CanvasBookmarkDao {
    @Upsert
    suspend fun upsert(entity: CanvasBookmarkEntity)

    @Query("DELETE FROM canvas_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM canvas_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM canvas_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): CanvasBookmarkEntity?

    @Query("SELECT * FROM canvas_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<CanvasBookmarkEntity?>
}
