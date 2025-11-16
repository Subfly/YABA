package dev.subfly.yabacore.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.subfly.yabacore.database.entities.oplog.OpLogEntryEntity

@Dao
interface OpLogDao {
    @Upsert
    suspend fun insert(entry: OpLogEntryEntity)

    @Upsert
    suspend fun insertAll(entries: List<OpLogEntryEntity>)

    @Query("SELECT * FROM op_log ORDER BY happenedAt ASC")
    suspend fun getAll(): List<OpLogEntryEntity>

    @Query(
        """
        SELECT * FROM op_log
        WHERE originDeviceId = :originDeviceId AND originSeq > :afterSeq
        ORDER BY originSeq ASC
        """
    )
    suspend fun getOpsAfter(originDeviceId: String, afterSeq: Long): List<OpLogEntryEntity>
}

