package dev.subfly.yabacore.state.detail.docmark

import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.notifications.PlatformNotificationText
import dev.subfly.yabacore.webview.Toc
import dev.subfly.yabacore.webview.WebShellLoadResult

sealed interface DocmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : DocmarkDetailEvent
    data object OnDeleteBookmark : DocmarkDetailEvent
    data object OnShareDocument : DocmarkDetailEvent
    data object OnExportDocument : DocmarkDetailEvent
    data object OnToggleReaderTheme : DocmarkDetailEvent
    data object OnToggleReaderFontSize : DocmarkDetailEvent
    data object OnToggleReaderLineHeight : DocmarkDetailEvent
    data class OnSetReaderTheme(val theme: ReaderTheme) : DocmarkDetailEvent
    data class OnSetReaderFontSize(val fontSize: ReaderFontSize) : DocmarkDetailEvent
    data class OnSetReaderLineHeight(val lineHeight: ReaderLineHeight) : DocmarkDetailEvent
    data class OnDeleteAnnotation(val annotationId: String) : DocmarkDetailEvent
    data class OnScrollToAnnotation(val annotationId: String) : DocmarkDetailEvent
    data object OnClearScrollToAnnotation : DocmarkDetailEvent
    data class OnTocChanged(val toc: Toc?) : DocmarkDetailEvent
    data class OnNavigateToTocItem(val id: String, val extrasJson: String?) : DocmarkDetailEvent
    data object OnClearTocNavigation : DocmarkDetailEvent
    data object OnRequestNotificationPermission : DocmarkDetailEvent
    data class OnScheduleReminder(
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
        val triggerAtEpochMillis: Long,
    ) : DocmarkDetailEvent
    data object OnCancelReminder : DocmarkDetailEvent

    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : DocmarkDetailEvent
}
