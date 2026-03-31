package dev.subfly.yaba.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.subfly.yaba.core.database.entities.NoteBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteBookmarkDao {
    @Upsert
    suspend fun upsert(entity: NoteBookmarkEntity)

    @Query("DELETE FROM note_bookmarks")
    suspend fun deleteAll()

    @Query("DELETE FROM note_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteById(bookmarkId: String)

    @Query("SELECT * FROM note_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    suspend fun getByBookmarkId(bookmarkId: String): NoteBookmarkEntity?

    @Query("SELECT * FROM note_bookmarks WHERE bookmarkId = :bookmarkId LIMIT 1")
    fun observeByBookmarkId(bookmarkId: String): Flow<NoteBookmarkEntity?>
}
