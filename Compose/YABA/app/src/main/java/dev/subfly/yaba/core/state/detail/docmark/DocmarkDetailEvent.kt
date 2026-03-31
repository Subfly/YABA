package dev.subfly.yaba.core.state.detail.docmark

import dev.subfly.yaba.core.model.utils.ReaderFontSize
import dev.subfly.yaba.core.model.utils.ReaderLineHeight
import dev.subfly.yaba.core.model.utils.ReaderTheme
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.WebShellLoadResult

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
        val title: String,
        val message: String,
        val triggerAtEpochMillis: Long,
    ) : DocmarkDetailEvent
    data object OnCancelReminder : DocmarkDetailEvent

    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : DocmarkDetailEvent
}
