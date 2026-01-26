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

    @Query("DELETE FROM readable_assets")
    suspend fun deleteAll()

    @Query("DELETE FROM readable_assets WHERE id = :assetId")
    suspend fun deleteById(assetId: String)

    @Query("DELETE FROM readable_assets WHERE bookmarkId = :bookmarkId")
    suspend fun deleteByBookmarkId(bookmarkId: String)

    @Query("SELECT * FROM readable_assets WHERE bookmarkId = :bookmarkId")
    suspend fun getByBookmarkId(bookmarkId: String): List<ReadableAssetEntity>

    @Query("SELECT * FROM readable_assets WHERE bookmarkId = :bookmarkId")
    fun observeByBookmarkId(bookmarkId: String): Flow<List<ReadableAssetEntity>>
}
