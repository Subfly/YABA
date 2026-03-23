package dev.subfly.yabacore.state.detail.docmark

import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface DocmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : DocmarkDetailEvent
    data object OnDeleteBookmark : DocmarkDetailEvent
    data object OnSharePdf : DocmarkDetailEvent
    data object OnExportPdf : DocmarkDetailEvent
    data class OnDeleteAnnotation(val annotationId: String) : DocmarkDetailEvent
    data class OnScrollToAnnotation(val annotationId: String) : DocmarkDetailEvent
    data object OnClearScrollToAnnotation : DocmarkDetailEvent
    data object OnRequestNotificationPermission : DocmarkDetailEvent
    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : DocmarkDetailEvent
    data object OnCancelReminder : DocmarkDetailEvent
}
