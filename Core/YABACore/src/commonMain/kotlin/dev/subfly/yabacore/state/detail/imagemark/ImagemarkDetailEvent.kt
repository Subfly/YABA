package dev.subfly.yabacore.state.detail.imagemark

import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface ImagemarkDetailEvent {
    data class OnInit(val bookmarkId: String) : ImagemarkDetailEvent
    data object OnDeleteBookmark : ImagemarkDetailEvent
    data object OnShareImage : ImagemarkDetailEvent
    data object OnExportImage : ImagemarkDetailEvent
    data object OnRequestNotificationPermission : ImagemarkDetailEvent
    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : ImagemarkDetailEvent
    data object OnCancelReminder : ImagemarkDetailEvent
}
