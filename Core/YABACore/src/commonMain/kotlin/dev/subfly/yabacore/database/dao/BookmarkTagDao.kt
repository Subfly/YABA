package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import dev.subfly.yabacore.database.entities.BookmarkTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkTagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRef(ref: BookmarkTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRefs(refs: List<BookmarkTagCrossRef>)

    @Delete
    suspend fun deleteRef(ref: BookmarkTagCrossRef)

    @Query(
        "DELETE FROM bookmark_tag_cross_ref WHERE bookmarkId = :bookmarkId AND tagId IN (:tagIds)"
    )
    suspend fun deleteRefs(bookmarkId: String, tagIds: List<String>)

    @Query("DELETE FROM bookmark_tag_cross_ref WHERE bookmarkId = :bookmarkId")
    suspend fun deleteAllForBookmark(bookmarkId: String)

    @Query("DELETE FROM bookmark_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteAllForTag(tagId: String)

    @Query("SELECT tagId FROM bookmark_tag_cross_ref WHERE bookmarkId = :bookmarkId")
    fun getTagIdsForBookmarkFlow(bookmarkId: String): Flow<List<String>>

    @Query("SELECT bookmarkId FROM bookmark_tag_cross_ref WHERE tagId = :tagId")
    fun getBookmarkIdsForTagFlow(tagId: String): Flow<List<String>>

    @Transaction
    suspend fun replaceTagsForBookmark(bookmarkId: String, tagIds: List<String>) {
        deleteAllForBookmark(bookmarkId)
        insertRefs(tagIds.map { BookmarkTagCrossRef(bookmarkId = bookmarkId, tagId = it) })
    }
}
