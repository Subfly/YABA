package dev.subfly.yaba.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.subfly.yaba.core.database.entities.TagBookmarkCrossRef

@Dao
interface TagBookmarkDao {
    @Upsert
    suspend fun insert(ref: TagBookmarkCrossRef)

    @Upsert
    suspend fun insertAll(refs: List<TagBookmarkCrossRef>)

    @Query("DELETE FROM tag_bookmarks WHERE bookmarkId = :bookmarkId AND tagId = :tagId")
    suspend fun delete(bookmarkId: String, tagId: String)

    @Query("DELETE FROM tag_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteForBookmark(bookmarkId: String)

    @Query("DELETE FROM tag_bookmarks WHERE tagId = :tagId")
    suspend fun deleteForTag(tagId: String)

    @Query("SELECT tagId FROM tag_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun getTagIdsForBookmark(bookmarkId: String): List<String>

    @Query("SELECT bookmarkId FROM tag_bookmarks WHERE tagId = :tagId")
    suspend fun getBookmarkIdsForTag(tagId: String): List<String>

    @Query("DELETE FROM tag_bookmarks")
    suspend fun deleteAll()
}
