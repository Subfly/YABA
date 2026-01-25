package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.ReadableAssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadableAssetDao {
    @Upsert
    suspend fun upsert(entity: ReadableAssetEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ReadableAssetEntity>)

    @Query("DELETE FROM readable_assets")
    suspend fun deleteAll()

    @Query("DELETE FROM readable_assets WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("SELECT * FROM readable_assets WHERE bookmarkId = :bookmarkId")
    suspend fun getByBookmarkId(bookmarkId: String): List<ReadableAssetEntity>

    @Query("SELECT * FROM readable_assets WHERE bookmarkId = :bookmarkId")
    fun observeByBookmarkId(bookmarkId: String): Flow<List<ReadableAssetEntity>>

    @Query("SELECT * FROM readable_assets WHERE bookmarkId = :bookmarkId AND contentVersion = :version")
    suspend fun getByBookmarkIdAndVersion(bookmarkId: String, version: Int): List<ReadableAssetEntity>

    @Query("SELECT * FROM readable_assets WHERE id = :assetId LIMIT 1")
    suspend fun getById(assetId: String): ReadableAssetEntity?
}
