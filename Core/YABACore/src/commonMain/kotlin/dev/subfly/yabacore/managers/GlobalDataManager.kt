@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package dev.subfly.yabacore.managers

import dev.subfly.yabacore.database.operations.DeleteAllPayload
import dev.subfly.yabacore.database.operations.OpApplier
import dev.subfly.yabacore.database.operations.OperationDraft
import dev.subfly.yabacore.database.operations.OperationEntityType
import dev.subfly.yabacore.database.operations.OperationKind
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

/**
 * Global data operations (e.g., wipe all data).
 */
object GlobalDataManager {
    private val opApplier get() = OpApplier
    private val clock = Clock.System

    /**
     * Wipe all local data and bookmark files, and clear platform notifications via the provided
     * callback. Emits a single ALL/BULK_DELETE operation so peers can replicate if needed.
     */
    suspend fun wipeAll(
        clearNotifications: suspend () -> Unit = {},
    ) {
        // Clear platform notifications first (Darwin can pass its own)
        clearNotifications()

        val draft = OperationDraft(
            entityType = OperationEntityType.ALL,
            entityId = "ALL",
            kind = OperationKind.BULK_DELETE,
            happenedAt = clock.now(),
            payload = DeleteAllPayload,
        )

        opApplier.applyLocal(listOf(draft))
    }
}

