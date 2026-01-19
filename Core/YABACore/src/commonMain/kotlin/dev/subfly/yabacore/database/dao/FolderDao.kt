package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Upsert
    suspend fun upsert(entity: FolderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FolderEntity>)

    @Delete
    suspend fun delete(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteAll()

    // Visible-only queries (exclude hidden system folders)
    @Query("SELECT * FROM folders WHERE id = :id AND isHidden = 0 LIMIT 1")
    suspend fun getById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE isHidden = 0")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id AND isHidden = 0 LIMIT 1")
    fun observeById(id: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isHidden = 0 ORDER BY `order` ASC")
    suspend fun getChildren(parentId: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL AND isHidden = 0 ORDER BY `order` ASC")
    suspend fun getRoot(): List<FolderEntity>

    // Internal queries (include hidden folders). Use these for system-folder operations and drift recovery.
    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getByIdIncludingHidden(id: String): FolderEntity?

    @Query("SELECT * FROM folders")
    suspend fun getAllIncludingHidden(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun observeByIdIncludingHidden(id: String): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    suspend fun getChildrenIncludingHidden(parentId: String): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    suspend fun getRootIncludingHidden(): List<FolderEntity>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE folders.isHidden = 0
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
    suspend fun getAllFoldersWithBookmarkCounts(
        sortType: String,
        sortOrder: String,
    ): List<FolderWithBookmarkCount>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE folders.isHidden = 0
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
    fun observeAllFoldersWithBookmarkCounts(
        sortType: String,
        sortOrder: String,
    ): Flow<List<FolderWithBookmarkCount>>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE id = :id AND folders.isHidden = 0
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
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getFolderWithBookmarkCountIncludingHidden(id: String): FolderWithBookmarkCount?

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE id NOT IN (:excludedIds) AND folders.isHidden = 0
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
    suspend fun getMovableFoldersExcluding(
        excludedIds: List<String>,
        sortType: String,
        sortOrder: String,
    ): List<FolderWithBookmarkCount>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE folders.isHidden = 0
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
    suspend fun getMovableFolders(
        sortType: String,
        sortOrder: String,
    ): List<FolderWithBookmarkCount>
}
