package dev.subfly.yabacore.database.entities.oplog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
@Entity(
    tableName = "op_log",
    indices = [
        Index(value = ["originDeviceId", "originSeq"], unique = true),
        Index(value = ["entityType", "entityId"]),
    ],
)
data class OpLogEntryEntity(
    @PrimaryKey val opId: Uuid,
    val originDeviceId: String,
    val originSeq: Long,
    val happenedAt: Instant,
    val entityType: String,
    val entityId: String,
    val opKind: String,
    val payloadJson: String,
)

