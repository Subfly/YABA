package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.ReadableVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadableVersionDao {
    @Upsert
    suspend fun upsert(entity: ReadableVersionEntity)

    @Query("DELETE FROM readable_versions")
    suspend fun deleteAll()

    @Query("DELETE FROM readable_versions WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("DELETE FROM readable_versions WHERE id = :versionId")
    suspend fun deleteById(versionId: String)

    @Query("SELECT * FROM readable_versions WHERE id = :versionId LIMIT 1")
    suspend fun getById(versionId: String): ReadableVersionEntity?

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId ORDER BY createdAt DESC")
    suspend fun getByBookmarkId(bookmarkId: String): List<ReadableVersionEntity>

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId ORDER BY createdAt DESC")
    fun observeByBookmarkId(bookmarkId: String): Flow<List<ReadableVersionEntity>>
}
