package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Upsert
    suspend fun upsert(entity: HighlightEntity)

    @Query("DELETE FROM highlights")
    suspend fun deleteAll()

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteById(highlightId: String)

    @Query("DELETE FROM highlights WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("SELECT * FROM highlights WHERE id = :highlightId LIMIT 1")
    suspend fun getById(highlightId: String): HighlightEntity?

    @Query(
        """
        SELECT * FROM highlights 
        WHERE bookmarkId = :bookmarkId 
        AND (:readableVersionId IS NULL OR readableVersionId = :readableVersionId)
        ORDER BY createdAt ASC
        """
    )
    suspend fun getByBookmarkId(
        bookmarkId: String,
        readableVersionId: String? = null,
    ): List<HighlightEntity>

    @Query(
        """
        SELECT * FROM highlights 
        WHERE bookmarkId = :bookmarkId 
        AND (:readableVersionId IS NULL OR readableVersionId = :readableVersionId)
        ORDER BY createdAt ASC
        """
    )
    fun observeByBookmarkId(
        bookmarkId: String,
        readableVersionId: String? = null,
    ): Flow<List<HighlightEntity>>
}
