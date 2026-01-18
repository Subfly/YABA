package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef

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

    @Query("DELETE FROM tag_bookmarks")
    suspend fun deleteAll()
}
