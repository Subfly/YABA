package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Upsert
    suspend fun upsert(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteAll()

    @Query("SELECT * FROM folders WHERE isHidden = 0")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id AND isHidden = 0 LIMIT 1")
    fun observeById(id: String): Flow<FolderEntity?>

    @Query(
        """
        SELECT * FROM folders 
        WHERE (:parentId IS NULL AND parentId IS NULL OR :parentId IS NOT NULL AND parentId = :parentId)
        AND (:includeHidden = 1 OR isHidden = 0)
        ORDER BY `order` ASC
        """
    )
    suspend fun getFoldersByParent(
        parentId: String? = null,
        includeHidden: Boolean = false,
    ): List<FolderEntity>

    @Query("SELECT * FROM folders")
    suspend fun getAllIncludingHidden(): List<FolderEntity>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getFolderWithBookmarkCount(id: String): FolderWithBookmarkCount?

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE folders.isHidden = 0
        AND (:excludedIdsCount = 0 OR id NOT IN (:excludedIds))
        ORDER BY
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN folders.`order` END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN folders.`order` END DESC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN folders.createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN folders.createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN folders.editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN folders.editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN folders.label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN folders.label END COLLATE NOCASE DESC
        """
    )
    fun observeFolders(
        sortType: String,
        sortOrder: String,
        excludedIds: List<String> = emptyList(),
        excludedIdsCount: Int = 0,
    ): Flow<List<FolderWithBookmarkCount>>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE folders.isHidden = 0
        AND (:excludedIdsCount = 0 OR id NOT IN (:excludedIds))
        ORDER BY
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'ASCENDING' THEN folders.`order` END ASC,
            CASE WHEN :sortType = 'CUSTOM' AND :sortOrder = 'DESCENDING' THEN folders.`order` END DESC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'ASCENDING' THEN folders.createdAt END ASC,
            CASE WHEN :sortType = 'CREATED_AT' AND :sortOrder = 'DESCENDING' THEN folders.createdAt END DESC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'ASCENDING' THEN folders.editedAt END ASC,
            CASE WHEN :sortType = 'EDITED_AT' AND :sortOrder = 'DESCENDING' THEN folders.editedAt END DESC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'ASCENDING' THEN folders.label END COLLATE NOCASE ASC,
            CASE WHEN :sortType = 'LABEL' AND :sortOrder = 'DESCENDING' THEN folders.label END COLLATE NOCASE DESC
        """
    )
    suspend fun getFolders(
        sortType: String,
        sortOrder: String,
        excludedIds: List<String> = emptyList(),
        excludedIdsCount: Int = 0,
    ): List<FolderWithBookmarkCount>
}
