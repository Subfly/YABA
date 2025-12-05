package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.models.LinkBookmarkWithRelations
import dev.subfly.yabacore.model.utils.BookmarkKind
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

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()

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
        ORDER BY
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN label END COLLATE NOCASE DESC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC
        """
    )
    fun observeLinkBookmarksForFolder(
        folderId: Uuid,
        sortType: String,
        sortOrder: String,
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
        ORDER BY
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN label END COLLATE NOCASE DESC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC
        """
    )
    fun observeLinkBookmarksForTag(
        tagId: Uuid,
        sortType: String,
        sortOrder: String,
        kind: BookmarkKind = BookmarkKind.LINK,
    ): Flow<List<LinkBookmarkWithRelations>>

    @Transaction
    @Query(
        """
        SELECT * FROM bookmarks
        WHERE kind = :kind
        ORDER BY
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN label END COLLATE NOCASE DESC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC
        """
    )
    fun observeAllLinkBookmarks(
        sortType: String,
        sortOrder: String,
        kind: BookmarkKind = BookmarkKind.LINK,
    ): Flow<List<LinkBookmarkWithRelations>>

    @Transaction
    @Query(
        """
        SELECT * FROM bookmarks
        WHERE (
            :applyKindFilter = 0 OR kind IN (:kinds)
        )
        AND (
            :applyFolderFilter = 0 OR folderId IN (:folderIds)
        )
        AND (
            :query IS NULL OR :query = '' OR label LIKE '%' || :query || '%'
        )
        AND (
            :applyTagFilter = 0 OR id IN (
                SELECT bookmarkId FROM tag_bookmarks WHERE tagId IN (:tagIds)
            )
        )
        ORDER BY
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN label END COLLATE NOCASE DESC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN editedAt END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN editedAt END DESC
        """
    )
    fun observeLinkBookmarksSearch(
        query: String?,
        kinds: List<BookmarkKind>,
        applyKindFilter: Boolean,
        folderIds: List<Uuid>,
        applyFolderFilter: Boolean,
        tagIds: List<Uuid>,
        applyTagFilter: Boolean,
        sortType: String,
        sortOrder: String,
    ): Flow<List<LinkBookmarkWithRelations>>
}
