package dev.subfly.yabacore.state.detail.linkmark

import dev.subfly.yabacore.model.annotation.AnnotationReadableCreateRequest
import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.notifications.PlatformNotificationText
import dev.subfly.yabacore.unfurl.ReadableUnfurl
import dev.subfly.yabacore.webview.WebConverterAsset
import dev.subfly.yabacore.webview.WebShellLoadResult

sealed interface LinkmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : LinkmarkDetailEvent
    data class OnSaveReadableContent(val readable: ReadableUnfurl) : LinkmarkDetailEvent
    data object OnFetchReadableContent : LinkmarkDetailEvent
    data object OnUpdateReadableRequested : LinkmarkDetailEvent
    data class OnConverterSucceeded(
        val documentJson: String,
        val assets: List<WebConverterAsset>,
    ) : LinkmarkDetailEvent
    data class OnConverterFailed(val error: Throwable) : LinkmarkDetailEvent
    data class OnReaderWebInitialContentLoad(val result: WebShellLoadResult) : LinkmarkDetailEvent
    data class OnSelectReadableVersion(val versionId: String) : LinkmarkDetailEvent
    data class OnDeleteReadableVersion(val versionId: String) : LinkmarkDetailEvent
    data object OnDeleteBookmark : LinkmarkDetailEvent
    data object OnToggleReaderTheme : LinkmarkDetailEvent
    data object OnToggleReaderFontSize : LinkmarkDetailEvent
    data object OnToggleReaderLineHeight : LinkmarkDetailEvent
    data class OnSetReaderTheme(val theme: ReaderTheme) : LinkmarkDetailEvent
    data class OnSetReaderFontSize(val fontSize: ReaderFontSize) : LinkmarkDetailEvent
    data class OnSetReaderLineHeight(val lineHeight: ReaderLineHeight) : LinkmarkDetailEvent
    data class OnCreateAnnotation(
        val annotationId: String,
        val readableVersionId: String,
        val colorRole: YabaColor = YabaColor.NONE,
        val note: String? = null,
        val quoteText: String? = null,
    ) : LinkmarkDetailEvent
    data class OnUpdateAnnotation(
        val annotationId: String,
        val colorRole: YabaColor,
        val note: String?,
    ) : LinkmarkDetailEvent
    data class OnDeleteAnnotation(val annotationId: String) : LinkmarkDetailEvent
    data class OnAnnotationReadableCreateCommitted(
        val annotationId: String,
        val request: AnnotationReadableCreateRequest,
        val documentJson: String,
    ) : LinkmarkDetailEvent
    data class OnAnnotationReadableDeleteCommitted(
        val annotationId: String,
        val documentJson: String,
    ) : LinkmarkDetailEvent
    data class OnScrollToAnnotation(val annotationId: String) : LinkmarkDetailEvent
    data object OnClearScrollToAnnotation : LinkmarkDetailEvent

    data object OnRequestNotificationPermission : LinkmarkDetailEvent
    data class OnScheduleReminder(
        val selectedDateMillis: Long,
        val hour: Int,
        val minute: Int,
        val title: PlatformNotificationText,
        val message: PlatformNotificationText,
    ) : LinkmarkDetailEvent
    data object OnCancelReminder : LinkmarkDetailEvent
}
