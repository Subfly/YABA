package dev.subfly.yaba.core.state.detail.imagemark


sealed interface ImagemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : ImagemarkDetailEvent
    data object OnDeleteBookmark : ImagemarkDetailEvent
    data object OnShareImage : ImagemarkDetailEvent
    data object OnExportImage : ImagemarkDetailEvent
    data object OnRequestNotificationPermission : ImagemarkDetailEvent
    data class OnScheduleReminder(
        val title: String,
        val message: String,
        val triggerAtEpochMillis: Long,
    ) : ImagemarkDetailEvent
    data object OnCancelReminder : ImagemarkDetailEvent
}
