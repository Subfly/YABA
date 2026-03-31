package dev.subfly.yaba.core.state.detail.linkmark

import dev.subfly.yaba.core.model.annotation.AnnotationReadableCreateRequest
import dev.subfly.yaba.core.model.utils.ReaderFontSize
import dev.subfly.yaba.core.model.utils.ReaderLineHeight
import dev.subfly.yaba.core.model.utils.ReaderTheme
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.unfurl.ReadableUnfurl
import dev.subfly.yaba.core.webview.WebConverterAsset
import dev.subfly.yaba.core.webview.WebLinkMetadata
import dev.subfly.yaba.core.webview.Toc
import dev.subfly.yaba.core.webview.WebShellLoadResult

sealed interface LinkmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : LinkmarkDetailEvent
    data class OnSaveReadableContent(val readable: ReadableUnfurl) : LinkmarkDetailEvent
    data object OnUpdateReadableRequested : LinkmarkDetailEvent
    data object OnUpdateLinkMetadataRequested : LinkmarkDetailEvent
    data class OnConverterSucceeded(
        val documentJson: String,
        val assets: List<WebConverterAsset>,
        val linkMetadata: WebLinkMetadata,
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
    data class OnTocChanged(val toc: Toc?) : LinkmarkDetailEvent
    data class OnNavigateToTocItem(val id: String, val extrasJson: String?) : LinkmarkDetailEvent
    data object OnClearTocNavigation : LinkmarkDetailEvent
    data object OnRequestNotificationPermission : LinkmarkDetailEvent
    data class OnScheduleReminder(
        val selectedDateMillis: Long,
        val hour: Int,
        val minute: Int,
        val title: String,
        val message: String,
    ) : LinkmarkDetailEvent
    data object OnCancelReminder : LinkmarkDetailEvent
    data class OnExportMarkdownReady(val markdown: String) : LinkmarkDetailEvent
    data class OnExportPdfReady(val pdfBase64: String) : LinkmarkDetailEvent
}
