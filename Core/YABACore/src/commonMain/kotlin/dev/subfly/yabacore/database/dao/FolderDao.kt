package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.models.FolderWithBookmarkCount
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Dao
interface FolderDao {
    @Upsert
    suspend fun upsert(entity: FolderEntity)

    @Upsert
    suspend fun upsertAll(entities: List<FolderEntity>)

    @Delete
    suspend fun delete(entity: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Uuid)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Uuid): FolderEntity?

    @Query("SELECT * FROM folders")
    suspend fun getAll(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    fun observeById(id: Uuid): Flow<FolderEntity?>

    @Query("SELECT * FROM folders ORDER BY `order` ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    fun observeRoot(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    fun observeChildren(parentId: Uuid): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY `order` ASC")
    suspend fun getChildren(parentId: Uuid): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY `order` ASC")
    suspend fun getRoot(): List<FolderEntity>

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE (
            :parentId IS NULL AND folders.parentId IS NULL
        ) OR (
            :parentId IS NOT NULL AND folders.parentId = :parentId
        )
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
    fun observeFoldersWithBookmarkCounts(
        parentId: Uuid?,
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
        WHERE (
            :parentId IS NULL AND folders.parentId IS NULL
        ) OR (
            :parentId IS NOT NULL AND folders.parentId = :parentId
        )
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
    suspend fun getFoldersWithBookmarkCounts(
        parentId: Uuid?,
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
        WHERE id = :id
        LIMIT 1
        """
    )
    suspend fun getFolderWithBookmarkCount(id: Uuid): FolderWithBookmarkCount?

    @Query(
        """
        SELECT folders.*,
        (
            SELECT COUNT(*) FROM bookmarks WHERE bookmarks.folderId = folders.id
        ) AS bookmarkCount
        FROM folders
        WHERE id NOT IN (:excludedIds)
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
        excludedIds: List<Uuid>,
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
