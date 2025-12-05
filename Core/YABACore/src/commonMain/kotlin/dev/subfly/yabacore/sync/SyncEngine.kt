package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.dao.OpLogDao
import dev.subfly.yabacore.database.dao.ReplicaCursorDao
import dev.subfly.yabacore.database.dao.ReplicaInfoDao
import dev.subfly.yabacore.database.entities.oplog.ReplicaCursorEntity
import dev.subfly.yabacore.database.entities.oplog.ReplicaInfoEntity
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.Operation
import dev.subfly.yabacore.database.operations.toOperation

class SyncEngine(
    private val opLogDao: OpLogDao,
    private val replicaInfoDao: ReplicaInfoDao,
    private val replicaCursorDao: ReplicaCursorDao,
    private val opApplier: OpApplier,
    private val deviceIdProvider: suspend () -> String,
) {
    suspend fun prepareSyncRequest(): SyncRequest {
        val localInfo = ensureReplicaInfo()
        val cursors = replicaCursorDao.getAll().associate {
            it.remoteDeviceId to it.lastSeqSeen
        }
        return SyncRequest(
            deviceId = localInfo.deviceId,
            cursors = cursors,
        )
    }

    suspend fun prepareSyncResponse(request: SyncRequest): SyncResponse {
        val localInfo = ensureReplicaInfo()
        val lastSeqSeen = request.cursors[localInfo.deviceId] ?: -1
        val operations = opLogDao.getOpsAfter(
            localInfo.deviceId,
            lastSeqSeen
        ).map { it.toOperation() }
        return SyncResponse(
            deviceId = localInfo.deviceId,
            operations = operations,
        )
    }

    suspend fun applySyncResponse(response: SyncResponse) {
        if (response.operations.isEmpty()) {
            return
        }

        opApplier.applyRemote(response.operations)
        response.operations.groupBy { it.originDeviceId }.forEach { (deviceId, ops) ->
            val lastSeq = ops.maxOf { it.originSeq }
            replicaCursorDao.upsert(
                ReplicaCursorEntity(
                    remoteDeviceId = deviceId,
                    lastSeqSeen = lastSeq,
                ),
            )
        }
    }

    fun extractFileChanges(operations: List<Operation>): List<FileSyncDescriptor> =
        operations.mapNotNull { it.toFileSyncDescriptor() }

    private suspend fun ensureReplicaInfo(): ReplicaInfoEntity {
        val info = replicaInfoDao.get()
        if (info != null) return info
        val newInfo =
            ReplicaInfoEntity(
                deviceId = deviceIdProvider(),
                nextOriginSeq = 0,
            )
        replicaInfoDao.upsert(newInfo)
        return newInfo
    }
}
