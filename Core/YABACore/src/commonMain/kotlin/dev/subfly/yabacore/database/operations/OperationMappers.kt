@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package dev.subfly.yabacore.database.operations

import dev.subfly.yabacore.database.entities.oplog.OpLogEntryEntity
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json

private val payloadSerializer = PolymorphicSerializer(OperationPayload::class)
private val operationJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun Operation.toEntity(): OpLogEntryEntity =
    OpLogEntryEntity(
        opId = opId,
        originDeviceId = originDeviceId,
        originSeq = originSeq,
        happenedAt = happenedAt,
        entityType = entityType.name,
        entityId = entityId,
        opKind = kind.name,
        payloadJson = operationJson.encodeToString(payloadSerializer, payload),
    )

fun OpLogEntryEntity.toOperation(): Operation =
    Operation(
        opId = opId,
        originDeviceId = originDeviceId,
        originSeq = originSeq,
        entityType = OperationEntityType.valueOf(entityType),
        entityId = entityId,
        kind = OperationKind.valueOf(opKind),
        happenedAt = happenedAt,
        payload = operationJson.decodeFromString(payloadSerializer, payloadJson),
    )
