@file:OptIn(ExperimentalUuidApi::class)

package dev.subfly.yabacore.database.operations

import dev.subfly.yabacore.database.entities.oplog.OpLogEntryEntity
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

private val payloadSerializer = PolymorphicSerializer(OperationPayload::class)

private val payloadModule = SerializersModule {
    polymorphic(OperationPayload::class) {
        subclass(FolderPayload::class)
        subclass(TagPayload::class)
        subclass(BookmarkPayload::class)
        subclass(LinkBookmarkPayload::class)
        subclass(TagLinkPayload::class)
        subclass(FilePayload::class)
        subclass(DeleteAllPayload::class)
    }
}

private val operationJson = Json {
    serializersModule = payloadModule
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun Operation.toEntity(): OpLogEntryEntity =
    OpLogEntryEntity(
        opId = opId.toString(),
        originDeviceId = originDeviceId,
        originSeq = originSeq,
        happenedAt = happenedAt.toEpochMilliseconds(),
        entityType = entityType.name,
        entityId = entityId,
        opKind = kind.name,
        payloadJson = operationJson.encodeToString(payloadSerializer, payload),
    )

fun OpLogEntryEntity.toOperation(): Operation =
    Operation(
        opId = opId.toUuid(),
        originDeviceId = originDeviceId,
        originSeq = originSeq,
        entityType = OperationEntityType.valueOf(entityType),
        entityId = entityId,
        kind = OperationKind.valueOf(opKind),
        happenedAt = happenedAt.toInstant(),
        payload = operationJson.decodeFromString(payloadSerializer, payloadJson),
    )

private fun String.toUuid(): Uuid = Uuid.parse(this)
private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)
