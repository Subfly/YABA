package dev.subfly.yabasync

sealed class SyncProgress {
    object Initializing : SyncProgress()
    object ExchangingDeleteLogs : SyncProgress()
    object SendingData : SyncProgress()
    object ReceivingData : SyncProgress()
    object MergingData : SyncProgress()
    object Completed : SyncProgress()
    data class Error(val message: String) : SyncProgress()
} 