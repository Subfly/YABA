package dev.subfly.yabacore.sync

import dev.subfly.yabacore.operations.Operation

data class SyncRequest(
    val deviceId: String,
    val cursors: Map<String, Long> = emptyMap(),
)

data class SyncResponse(
    val deviceId: String,
    val operations: List<Operation>,
)
