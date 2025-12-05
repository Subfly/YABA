package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.TagWithBookmarkCount
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(entity: TagEntity)

    @Upsert
    suspend fun upsertAll(entities: List<TagEntity>)

    @Delete
    suspend fun delete(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Uuid): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    fun observeById(id: Uuid): Flow<TagEntity?>

    @Query("SELECT * FROM tags ORDER BY `order` ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN tag_bookmarks ON tags.id = tag_bookmarks.tagId
        WHERE tag_bookmarks.bookmarkId = :bookmarkId
        ORDER BY tags.`order` ASC
        """
    )
    fun observeTagsForBookmark(bookmarkId: Uuid): Flow<List<TagEntity>>

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        ORDER BY
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN tags.`order` END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN tags.`order` END DESC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN tags.createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN tags.createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN tags.editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN tags.editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN tags.label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN tags.label END COLLATE NOCASE DESC
        """
    )
    fun observeTagsWithBookmarkCounts(
        sortType: String,
        sortOrder: String,
    ): Flow<List<TagWithBookmarkCount>>

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        ORDER BY
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN tags.`order` END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN tags.`order` END DESC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN tags.createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN tags.createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN tags.editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN tags.editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN tags.label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN tags.label END COLLATE NOCASE DESC
        """
    )
    suspend fun getTagsWithBookmarkCounts(
        sortType: String,
        sortOrder: String,
    ): List<TagWithBookmarkCount>

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        INNER JOIN tag_bookmarks ON tags.id = tag_bookmarks.tagId
        WHERE tag_bookmarks.bookmarkId = :bookmarkId
        ORDER BY
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN tags.`order` END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN tags.`order` END DESC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN tags.createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN tags.createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN tags.editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN tags.editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN tags.label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN tags.label END COLLATE NOCASE DESC
        """
    )
    fun observeTagsForBookmarkWithCounts(
        bookmarkId: Uuid,
        sortType: String,
        sortOrder: String,
    ): Flow<List<TagWithBookmarkCount>>

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        WHERE tags.id = :id
        LIMIT 1
        """
    )
    suspend fun getTagWithBookmarkCount(id: Uuid): TagWithBookmarkCount?

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        INNER JOIN tag_bookmarks ON tags.id = tag_bookmarks.tagId
        WHERE tag_bookmarks.bookmarkId = :bookmarkId
        ORDER BY tags.label COLLATE NOCASE ASC
        """
    )
    suspend fun getTagsForBookmarkWithCounts(
        bookmarkId: Uuid,
    ): List<TagWithBookmarkCount>
}
