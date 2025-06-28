package dev.subfly.yabasync

import kotlinx.serialization.Serializable

@Serializable
data class DeleteLog(
    val logId: String,
    val entityId: String,
    val entityType: EntityType,
    val actionType: ActionType,
    val timestamp: Long,
    val fieldChangesJSON: String? = null
)

enum class EntityType {
    BOOKMARK, COLLECTION, ALL
}

enum class ActionType {
    CREATED, UPDATED, DELETED, DELETED_ALL
} 