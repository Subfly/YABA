package dev.subfly.yabacore.state.detail.docmark

import dev.subfly.yabacore.notifications.PlatformNotificationText

sealed interface DocmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : DocmarkDetailEvent
    data object OnDeleteBookmark : DocmarkDetailEvent
    data object OnSharePdf : DocmarkDetailEvent
    data object OnExportPdf : DocmarkDetailEvent
    data class OnDeleteHighlight(val highlightId: String) : DocmarkDetailEvent
    data class OnScrollToHighlight(val highlightId: String) : DocmarkDetailEvent
    data object OnClearScrollToHighlight : DocmarkDetailEvent
    data object OnRequestNotificationPermission : DocmarkDetailEvent
    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : DocmarkDetailEvent
    data object OnCancelReminder : DocmarkDetailEvent
}
