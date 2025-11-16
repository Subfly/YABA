package dev.subfly.yabacore.database.operations

import androidx.room.Transactor
import androidx.room.useWriterConnection
import dev.subfly.yabacore.database.YabaDatabase
import dev.subfly.yabacore.database.dao.BookmarkDao
import dev.subfly.yabacore.database.dao.EntityClockDao
import dev.subfly.yabacore.database.dao.FolderDao
import dev.subfly.yabacore.database.dao.LinkBookmarkDao
import dev.subfly.yabacore.database.dao.OpLogDao
import dev.subfly.yabacore.database.dao.ReplicaInfoDao
import dev.subfly.yabacore.database.dao.TagBookmarkDao
import dev.subfly.yabacore.database.dao.TagDao
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.entities.oplog.EntityClockEntity
import dev.subfly.yabacore.database.entities.oplog.ReplicaInfoEntity
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
class OpApplier(
    private val database: YabaDatabase,
    private val folderDao: FolderDao = database.folderDao(),
    private val tagDao: TagDao = database.tagDao(),
    private val bookmarkDao: BookmarkDao = database.bookmarkDao(),
    private val linkBookmarkDao: LinkBookmarkDao = database.linkBookmarkDao(),
    private val tagBookmarkDao: TagBookmarkDao = database.tagBookmarkDao(),
    private val opLogDao: OpLogDao = database.opLogDao(),
    private val clockDao: EntityClockDao = database.entityClockDao(),
    private val replicaInfoDao: ReplicaInfoDao = database.replicaInfoDao(),
    private val deviceIdProvider: suspend () -> String,
) {
    suspend fun applyLocal(drafts: List<OperationDraft>): List<Operation> {
        if (drafts.isEmpty()) return emptyList()
        return database.runWriterTransaction {
            val replicaInfo = ensureReplicaInfo()
            var nextSeq = replicaInfo.nextOriginSeq
            val operations =
                drafts.map { draft ->
                    Operation(
                        opId = Uuid.random(),
                        originDeviceId = replicaInfo.deviceId,
                        originSeq = nextSeq++,
                        entityType = draft.entityType,
                        entityId = draft.entityId,
                        kind = draft.kind,
                        happenedAt = draft.happenedAt,
                        payload = draft.payload,
                    ).also { applyOperation(it) }
                }
            replicaInfoDao.upsert(replicaInfo.copy(nextOriginSeq = nextSeq))
            opLogDao.insertAll(operations.map { it.toEntity() })
            operations
        }
    }

    suspend fun applyRemote(operations: List<Operation>) {
        if (operations.isEmpty()) return
        database.runWriterTransaction {
            operations.forEach { operation ->
                applyOperation(operation)
                opLogDao.insert(operation.toEntity())
            }
        }
    }

    private suspend fun ensureReplicaInfo(): ReplicaInfoEntity {
        val current = replicaInfoDao.get()
        if (current != null) return current
        val newInfo =
            ReplicaInfoEntity(
                deviceId = deviceIdProvider(),
                nextOriginSeq = 0,
            )
        replicaInfoDao.upsert(newInfo)
        return newInfo
    }

    private suspend fun applyOperation(operation: Operation) {
        val clockKeyType = operation.entityType.name
        val clock = clockDao.get(clockKeyType, operation.entityId)
        if (clock != null && !isIncomingNewer(operation, clock)) {
            return
        }

        when (operation.entityType) {
            OperationEntityType.FOLDER -> applyFolderOperation(operation)
            OperationEntityType.TAG -> applyTagOperation(operation)
            OperationEntityType.BOOKMARK -> applyBookmarkOperation(operation)
            OperationEntityType.TAG_LINK -> applyTagLinkOperation(operation)
        }

        clockDao.upsert(
            EntityClockEntity(
                entityType = clockKeyType,
                entityId = operation.entityId,
                lastDeviceId = operation.originDeviceId,
                lastSeq = operation.originSeq,
            ),
        )
    }

    private suspend fun applyFolderOperation(operation: Operation) {
        val payload = operation.payload as? FolderPayload ?: return
        val folderId = operation.entityId.toUuidOrNull() ?: return
        when (operation.kind) {
            OperationKind.DELETE, OperationKind.BULK_DELETE -> folderDao.deleteById(folderId)
            else -> {
                val entity =
                    FolderEntity(
                        id = folderId,
                        parentId = payload.parentId?.toUuidOrNull(),
                        label = payload.label,
                        description = payload.description,
                        icon = payload.icon,
                        color = YabaColor.fromCode(payload.colorCode),
                        order = payload.order,
                        createdAt = payload.createdAtEpochMillis.toInstant(),
                        editedAt = payload.editedAtEpochMillis.toInstant(),
                    )
                folderDao.upsert(entity)
            }
        }
    }

    private suspend fun applyTagOperation(operation: Operation) {
        val payload = operation.payload as? TagPayload ?: return
        val tagId = operation.entityId.toUuidOrNull() ?: return
        when (operation.kind) {
            OperationKind.DELETE, OperationKind.BULK_DELETE -> tagDao.deleteById(tagId)
            else -> {
                val entity =
                    TagEntity(
                        id = tagId,
                        label = payload.label,
                        icon = payload.icon,
                        color = YabaColor.fromCode(payload.colorCode),
                        order = payload.order,
                        createdAt = payload.createdAtEpochMillis.toInstant(),
                        editedAt = payload.editedAtEpochMillis.toInstant(),
                    )
                tagDao.upsert(entity)
            }
        }
    }

    private suspend fun applyBookmarkOperation(operation: Operation) {
        val payload = operation.payload as? BookmarkPayload ?: return
        val bookmarkId = operation.entityId.toUuidOrNull() ?: return
        when (operation.kind) {
            OperationKind.DELETE, OperationKind.BULK_DELETE ->
                bookmarkDao.deleteByIds(listOf(bookmarkId))

            else -> {
                val entity =
                    BookmarkEntity(
                        id = bookmarkId,
                        folderId = payload.folderId.toUuidOrNull() ?: return,
                        kind = BookmarkKind.fromCode(payload.kindCode),
                        label = payload.label,
                        createdAt = payload.createdAtEpochMillis.toInstant(),
                        editedAt = payload.editedAtEpochMillis.toInstant(),
                    )
                bookmarkDao.upsert(entity)
                payload.link?.let { linkPayload ->
                    val linkEntity =
                        LinkBookmarkEntity(
                            bookmarkId = bookmarkId,
                            description = linkPayload.description,
                            url = linkPayload.url,
                            domain = linkPayload.domain,
                            linkType = LinkType.fromCode(linkPayload.linkTypeCode),
                            previewImageUrl = linkPayload.previewImageUrl,
                            previewIconUrl = linkPayload.previewIconUrl,
                            videoUrl = linkPayload.videoUrl,
                        )
                    linkBookmarkDao.upsert(linkEntity)
                }
            }
        }
    }

    private suspend fun applyTagLinkOperation(operation: Operation) {
        val payload = operation.payload as? TagLinkPayload ?: return
        val tagId = payload.tagId.toUuidOrNull() ?: return
        val bookmarkId = payload.bookmarkId.toUuidOrNull() ?: return
        when (operation.kind) {
            OperationKind.TAG_ADD ->
                tagBookmarkDao.insert(
                    TagBookmarkCrossRef(tagId = tagId, bookmarkId = bookmarkId)
                )

            OperationKind.TAG_REMOVE ->
                tagBookmarkDao.delete(bookmarkId = bookmarkId, tagId = tagId)

            else -> {}
        }
    }

    private fun isIncomingNewer(operation: Operation, clock: EntityClockEntity): Boolean =
        when {
            operation.originSeq > clock.lastSeq -> true
            operation.originSeq < clock.lastSeq -> false
            else -> operation.originDeviceId > clock.lastDeviceId
        }

    private fun String.toUuidOrNull(): Uuid? = runCatching { Uuid.parse(this) }.getOrNull()

    private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
}

private suspend fun <R> YabaDatabase.runWriterTransaction(
    block: suspend () -> R,
): R = useWriterConnection { transactor ->
    transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) { block() }
}
