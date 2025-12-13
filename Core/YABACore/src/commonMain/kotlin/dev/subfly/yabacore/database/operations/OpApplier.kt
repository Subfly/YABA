package dev.subfly.yabacore.database.operations

import androidx.room.Transactor
import androidx.room.useWriterConnection
import dev.subfly.yabacore.common.CoreConstants
import dev.subfly.yabacore.database.DatabaseProvider
import dev.subfly.yabacore.database.DeviceIdProvider
import dev.subfly.yabacore.database.YabaDatabase
import dev.subfly.yabacore.database.entities.BookmarkEntity
import dev.subfly.yabacore.database.entities.FolderEntity
import dev.subfly.yabacore.database.entities.LinkBookmarkEntity
import dev.subfly.yabacore.database.entities.TagBookmarkCrossRef
import dev.subfly.yabacore.database.entities.TagEntity
import dev.subfly.yabacore.database.entities.oplog.EntityClockEntity
import dev.subfly.yabacore.database.entities.oplog.ReplicaInfoEntity
import dev.subfly.yabacore.filesystem.BookmarkFileManager
import dev.subfly.yabacore.filesystem.model.BookmarkFileAssetKind
import dev.subfly.yabacore.model.utils.BookmarkKind
import dev.subfly.yabacore.model.utils.LinkType
import dev.subfly.yabacore.model.utils.YabaColor
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
object OpApplier {
    private val database
        get() = DatabaseProvider.database
    private val folderDao
        get() = DatabaseProvider.folderDao
    private val tagDao
        get() = DatabaseProvider.tagDao
    private val bookmarkDao
        get() = DatabaseProvider.bookmarkDao
    private val linkBookmarkDao
        get() = DatabaseProvider.linkBookmarkDao
    private val tagBookmarkDao
        get() = DatabaseProvider.tagBookmarkDao
    private val opLogDao
        get() = DatabaseProvider.opLogDao
    private val clockDao
        get() = DatabaseProvider.entityClockDao
    private val replicaInfoDao
        get() = DatabaseProvider.replicaInfoDao

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
                    )
                        .also { applyOperation(it) }
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
                deviceId = DeviceIdProvider.get(),
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
            OperationEntityType.FILE -> applyFileOperation(operation)
            OperationEntityType.ALL -> applyDeleteAllOperation()
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
            OperationKind.DELETE, OperationKind.BULK_DELETE ->
                folderDao.deleteById(folderId.asString())

            else -> {
                val entity = FolderEntity(
                    id = folderId.asString(),
                    parentId = payload.parentId,
                    label = payload.label,
                    description = payload.description,
                    icon = payload.icon,
                    color = YabaColor.fromCode(payload.colorCode),
                    order = payload.order,
                    createdAt = payload.createdAtEpochMillis,
                    editedAt = payload.editedAtEpochMillis,
                )
                folderDao.upsert(entity)
            }
        }
    }

    private suspend fun applyTagOperation(operation: Operation) {
        val payload = operation.payload as? TagPayload ?: return
        val tagId = operation.entityId.toUuidOrNull() ?: return
        when (operation.kind) {
            OperationKind.DELETE, OperationKind.BULK_DELETE -> tagDao.deleteById(tagId.asString())
            else -> {
                val entity = TagEntity(
                    id = tagId.asString(),
                    label = payload.label,
                    icon = payload.icon,
                    color = YabaColor.fromCode(payload.colorCode),
                    order = payload.order,
                    createdAt = payload.createdAtEpochMillis,
                    editedAt = payload.editedAtEpochMillis,
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
                bookmarkDao.deleteByIds(listOf(bookmarkId.asString()))

            else -> {
                val folderId = payload.folderId.toUuidOrNull() ?: return
                val entity = BookmarkEntity(
                    id = bookmarkId.asString(),
                    folderId = folderId.asString(),
                    kind = BookmarkKind.fromCode(payload.kindCode),
                    label = payload.label,
                    createdAt = payload.createdAtEpochMillis,
                    editedAt = payload.editedAtEpochMillis,
                    viewCount = payload.viewCount,
                    isPrivate = payload.isPrivate,
                    isPinned = payload.isPinned,
                )
                bookmarkDao.upsert(entity)
                payload.link?.let { linkPayload ->
                    val linkEntity = LinkBookmarkEntity(
                        bookmarkId = bookmarkId.asString(),
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
                    TagBookmarkCrossRef(
                        tagId = tagId.asString(),
                        bookmarkId = bookmarkId.asString()
                    )
                )

            OperationKind.TAG_REMOVE ->
                tagBookmarkDao.delete(
                    bookmarkId = bookmarkId.asString(),
                    tagId = tagId.asString()
                )

            else -> {}
        }
    }

    private fun applyFileOperation(operation: Operation) {
        // Placeholder: file payloads are tracked in the op log for
        // transport layers to act on. No direct database mutation.
        if (operation.payload !is FilePayload) return
    }

    private suspend fun applyDeleteAllOperation() {
        // Clear bookmark assets on disk in one shot (delete the bookmarks root)
        BookmarkFileManager.deleteRelativePath(
            relativePath = CoreConstants.FileSystem.BOOKMARKS_DIR,
            assetKind = BookmarkFileAssetKind.UNKNOWN,
        )

        // Bulk delete tables
        tagBookmarkDao.deleteAll()
        linkBookmarkDao.deleteAll()
        bookmarkDao.deleteAll()
        tagDao.deleteAll()
        folderDao.deleteAll()

        // Recreate replica info so we can continue syncing after wipe
        ensureReplicaInfo()
    }

    private fun isIncomingNewer(operation: Operation, clock: EntityClockEntity): Boolean =
        when {
            operation.originSeq > clock.lastSeq -> true
            operation.originSeq < clock.lastSeq -> false
            else -> operation.originDeviceId > clock.lastDeviceId
        }

    private fun String.toUuidOrNull(): Uuid? = runCatching { Uuid.parse(this) }.getOrNull()
    private fun Uuid.asString(): String = toString()
}

private suspend fun <R> YabaDatabase.runWriterTransaction(
    block: suspend () -> R,
): R = useWriterConnection { transactor ->
    transactor.withTransaction(Transactor.SQLiteTransactionType.IMMEDIATE) { block() }
}
