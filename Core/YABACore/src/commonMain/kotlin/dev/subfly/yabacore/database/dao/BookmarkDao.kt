package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.model.BookmarkKind
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface BookmarkDao {
    @Upsert
    suspend fun upsert(entity: BookmarkEntity)

    @Upsert
    suspend fun upsertAll(entities: List<BookmarkEntity>)

    @Delete
    suspend fun delete(entity: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Uuid>)

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Uuid): BookmarkEntity?

    @Transaction
    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getLinkBookmarkById(id: Uuid): LinkBookmarkWithRelations?

    @Query("SELECT * FROM bookmarks")
    suspend fun getAll(): List<BookmarkEntity>

    @Transaction
    @Query(
            """
        SELECT * FROM bookmarks
        WHERE folderId = :folderId AND kind = :kind
        """
    )
    fun observeLinkBookmarksForFolder(
            folderId: Uuid,
            kind: BookmarkKind = BookmarkKind.LINK,
    ): Flow<List<LinkBookmarkWithRelations>>

    @Transaction
    @Query(
            """
        SELECT * FROM bookmarks
        WHERE kind = :kind
        AND id IN (
            SELECT bookmarkId FROM tag_bookmarks WHERE tagId = :tagId
        )
        """
    )
    fun observeLinkBookmarksForTag(
            tagId: Uuid,
            kind: BookmarkKind = BookmarkKind.LINK,
    ): Flow<List<LinkBookmarkWithRelations>>

    @Transaction
    @Query(
            """
        SELECT * FROM bookmarks
        WHERE kind = :kind
        ORDER BY editedAt DESC
        """
    )
    fun observeAllLinkBookmarks(
            kind: BookmarkKind = BookmarkKind.LINK
    ): Flow<List<LinkBookmarkWithRelations>>

    @Transaction
    @Query(
            """
        SELECT * FROM bookmarks
        WHERE kind = :kind AND (
            label LIKE '%' || :query || '%'
        )
        """
    )
    fun observeLinkBookmarksSearch(
            query: String,
            kind: BookmarkKind = BookmarkKind.LINK,
    ): Flow<List<LinkBookmarkWithRelations>>
}
