package dev.subfly.yabacore.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.subfly.yabacore.database.entities.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Upsert
    suspend fun upsert(entity: AnnotationEntity)

    @Query("DELETE FROM annotations")
    suspend fun deleteAll()

    @Query("DELETE FROM annotations WHERE id = :annotationId")
    suspend fun deleteById(annotationId: String)

    @Query("DELETE FROM annotations WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("SELECT * FROM annotations WHERE id = :annotationId LIMIT 1")
    suspend fun getById(annotationId: String): AnnotationEntity?

    @Query(
        """
        SELECT * FROM annotations 
        WHERE bookmarkId = :bookmarkId 
        AND (:readableVersionId IS NULL OR readableVersionId = :readableVersionId)
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getByBookmarkId(
        bookmarkId: String,
        readableVersionId: String? = null,
    ): List<AnnotationEntity>

    @Query(
        """
        SELECT * FROM annotations 
        WHERE bookmarkId = :bookmarkId 
        AND (:readableVersionId IS NULL OR readableVersionId = :readableVersionId)
        ORDER BY createdAt ASC
        """,
    )
    fun observeByBookmarkId(
        bookmarkId: String,
        readableVersionId: String? = null,
    ): Flow<List<AnnotationEntity>>
}
