package dev.subfly.yabacore.database.entities.oplog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
@Entity(
    tableName = "op_log",
    indices = [
        Index(value = ["originDeviceId", "originSeq"], unique = true),
        Index(value = ["entityType", "entityId"]),
    ],
)
data class OpLogEntryEntity(
    @PrimaryKey val opId: String,
    val originDeviceId: String,
    val originSeq: Long,
    val happenedAt: Long,
    val entityType: String,
    val entityId: String,
    val opKind: String,
    val payloadJson: String,
)

