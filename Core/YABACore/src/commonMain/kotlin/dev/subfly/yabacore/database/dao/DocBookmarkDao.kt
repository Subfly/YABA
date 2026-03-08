package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.DocBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocBookmarkDao {
    @Upsert
    suspend fun upsert(entity: DocBookmarkEntity)

    @Query("DELETE FROM doc_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM doc_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM doc_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): DocBookmarkEntity?

    @Query("SELECT * FROM doc_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<DocBookmarkEntity?>
}
