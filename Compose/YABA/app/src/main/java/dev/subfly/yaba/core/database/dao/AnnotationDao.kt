package dev.subfly.yaba.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import dev.subfly.yaba.core.database.entities.AnnotationEntity
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
        ORDER BY createdAt ASC
        """,
    )
    suspend fun getByBookmarkId(bookmarkId: String): List<AnnotationEntity>

    @Query(
        """
        SELECT * FROM annotations 
        WHERE bookmarkId = :bookmarkId 
        ORDER BY createdAt ASC
        """,
    )
    fun observeByBookmarkId(bookmarkId: String): Flow<List<AnnotationEntity>>
}
