package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalUuidApi::class)
@Dao
interface TagBookmarkDao {
    @Upsert suspend fun insert(ref: TagBookmarkCrossRef)

    @Upsert suspend fun insertAll(refs: List<TagBookmarkCrossRef>)

    @Query("DELETE FROM tag_bookmarks WHERE bookmarkId = :bookmarkId AND tagId = :tagId")
    suspend fun delete(bookmarkId: Uuid, tagId: Uuid)

    @Query("DELETE FROM tag_bookmarks WHERE bookmarkId = :bookmarkId")
    suspend fun deleteForBookmark(bookmarkId: Uuid)

    @Query("DELETE FROM tag_bookmarks WHERE tagId = :tagId") suspend fun deleteForTag(tagId: Uuid)

    @Query("DELETE FROM tag_bookmarks") suspend fun deleteAll()

    @Query("SELECT tagId FROM tag_bookmarks WHERE bookmarkId = :bookmarkId")
    fun observeTagIdsForBookmark(bookmarkId: Uuid): Flow<List<Uuid>>

    @Query("SELECT bookmarkId FROM tag_bookmarks WHERE tagId = :tagId")
    fun observeBookmarkIdsForTag(tagId: Uuid): Flow<List<Uuid>>
}
