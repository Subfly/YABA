package dev.subfly.yabacore.sync

import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.entities.oplog.ReplicaCursorEntity
import dev.subfly.yabacore.database.entities.oplog.ReplicaInfoEntity
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.Operation
import dev.subfly.yabacore.database.operations.OperationEntityType
import dev.subfly.yabacore.database.operations.OperationKind
import dev.subfly.yabacore.database.operations.toOperation

object SyncEngine {
    private val opApplier
        get() = OpApplier
    private val opLogDao
        get() = DatabaseProvider.opLogDao
    private val replicaInfoDao
        get() = DatabaseProvider.replicaInfoDao
    private val replicaCursorDao
        get() = DatabaseProvider.replicaCursorDao
    suspend fun prepareSyncRequest(): SyncRequest {
        val localInfo = ensureReplicaInfo()
        val cursors = replicaCursorDao.getAll().associate { it.remoteDeviceId to it.lastSeqSeen }
        return SyncRequest(
                deviceId = localInfo.deviceId,
                cursors = cursors,
        )
    }

    suspend fun prepareSyncResponse(request: SyncRequest): SyncResponse {
        val localInfo = ensureReplicaInfo()
        val lastSeqSeen = request.cursors[localInfo.deviceId] ?: -1
        val operations =
                opLogDao.getOpsAfter(localInfo.deviceId, lastSeqSeen).map { it.toOperation() }
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

    fun containsDeleteAll(operations: List<Operation>): Boolean =
            operations.any {
                it.entityType == OperationEntityType.ALL && it.kind == OperationKind.BULK_DELETE
            }

    private suspend fun ensureReplicaInfo(): ReplicaInfoEntity {
        val info = replicaInfoDao.get()
        if (info != null) return info
        val newInfo =
                ReplicaInfoEntity(
                        deviceId = DeviceIdProvider.get(),
                        nextOriginSeq = 0,
                )
        replicaInfoDao.upsert(newInfo)
        return newInfo
    }
}
