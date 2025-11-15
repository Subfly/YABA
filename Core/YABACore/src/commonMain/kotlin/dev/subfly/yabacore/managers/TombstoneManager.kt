package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.dao.TombstoneDao
import dev.subfly.yabacore.database.entities.TombstoneEntity
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@OptIn(ExperimentalUuidApi::class)
class TombstoneManager(
        private val tombstoneDao: TombstoneDao,
) {
    fun getAllFlow(): Flow<List<TombstoneEntity>> = tombstoneDao.getAllFlow()
    fun getByTypeFlow(entityType: String): Flow<List<TombstoneEntity>> =
            tombstoneDao.getByTypeFlow(entityType)

    suspend fun create(
            entityType: String,
            entityId: String,
            deviceId: String? = null
    ): TombstoneEntity {
        val entity =
                TombstoneEntity(
                        tombstoneId = Uuid.random().toString(),
                        entityType = entityType,
                        entityId = entityId,
                        timestamp = Clock.System.now(),
                        deviceId = deviceId,
                )
        tombstoneDao.insert(entity)
        return entity
    }

    suspend fun createBulk(
            items: List<Pair<String, String>>,
            deviceId: String? = null
    ): List<TombstoneEntity> {
        val now = Clock.System.now()
        val entities =
                items.map { (type, id) ->
                    TombstoneEntity(
                            tombstoneId = Uuid.random().toString(),
                            entityType = type,
                            entityId = id,
                            timestamp = now,
                            deviceId = deviceId,
                    )
                }
        tombstoneDao.insertAll(entities)
        return entities
    }

    suspend fun cleanupOlderThan(cutoff: Instant) {
        tombstoneDao.deleteOlderThan(cutoff.toEpochMilliseconds())
    }
}
