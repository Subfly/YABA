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

    @Upsert
    suspend fun upsertAll(entities: List<ReadableVersionEntity>)

    @Query("DELETE FROM readable_versions")
    suspend fun deleteAll()

    @Query("DELETE FROM readable_versions WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId ORDER BY contentVersion DESC")
    suspend fun getByBookmarkId(bookmarkId: String): List<ReadableVersionEntity>

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId ORDER BY contentVersion DESC")
    fun observeByBookmarkId(bookmarkId: String): Flow<List<ReadableVersionEntity>>

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId AND contentVersion = :version LIMIT 1")
    suspend fun getByBookmarkIdAndVersion(bookmarkId: String, version: Int): ReadableVersionEntity?

    @Query("SELECT * FROM readable_versions WHERE bookmarkId = :bookmarkId ORDER BY contentVersion DESC LIMIT 1")
    suspend fun getLatestByBookmarkId(bookmarkId: String): ReadableVersionEntity?

    @Query("SELECT MAX(contentVersion) FROM readable_versions WHERE bookmarkId = :bookmarkId")
    suspend fun getMaxVersionByBookmarkId(bookmarkId: String): Int?
}
