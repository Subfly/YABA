package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.models.TagWithBookmarkCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Upsert
    suspend fun upsert(entity: TagEntity)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TagEntity?

    @Query("SELECT * FROM tags")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TagEntity?>

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        WHERE tags.isHidden = 0
        ORDER BY
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
        WHERE tags.id = :id
        LIMIT 1
        """
    )
    suspend fun getTagWithBookmarkCount(id: String): TagWithBookmarkCount?

    @Query(
        """
        SELECT tags.*,
        (
            SELECT COUNT(*) FROM tag_bookmarks WHERE tag_bookmarks.tagId = tags.id
        ) AS bookmarkCount
        FROM tags
        INNER JOIN tag_bookmarks ON tags.id = tag_bookmarks.tagId
        WHERE tag_bookmarks.bookmarkId = :bookmarkId AND tags.isHidden = 0
        ORDER BY tags.label COLLATE NOCASE ASC
        """
    )
    suspend fun getTagsForBookmarkWithCounts(
        bookmarkId: String,
    ): List<TagWithBookmarkCount>
}
