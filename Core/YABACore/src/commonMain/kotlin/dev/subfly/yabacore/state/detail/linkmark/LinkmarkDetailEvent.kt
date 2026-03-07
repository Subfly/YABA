package dev.subfly.yabacore.state.detail.linkmark

import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.notifications.PlatformNotificationText
import dev.subfly.yabacore.unfurl.ReadableUnfurl
import dev.subfly.yabacore.unfurl.ConverterAssetInput

sealed interface LinkmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : LinkmarkDetailEvent
    data class OnSaveReadableContent(val readable: ReadableUnfurl) : LinkmarkDetailEvent
    data object OnFetchReadableContent : LinkmarkDetailEvent
    data object OnUpdateReadableRequested : LinkmarkDetailEvent
    data class OnConverterSucceeded(
        val markdown: String,
        val assets: List<ConverterAssetInput>,
        val title: String?,
        val author: String?,
    ) : LinkmarkDetailEvent
    data class OnConverterFailed(val error: Throwable) : LinkmarkDetailEvent
    data class OnSelectReadableVersion(val versionId: String) : LinkmarkDetailEvent
    data class OnDeleteReadableVersion(val versionId: String) : LinkmarkDetailEvent
    data object OnDeleteBookmark : LinkmarkDetailEvent
    data object OnToggleReaderTheme : LinkmarkDetailEvent
    data object OnToggleReaderFontSize : LinkmarkDetailEvent
    data object OnToggleReaderLineHeight : LinkmarkDetailEvent
    data class OnSetReaderTheme(val theme: ReaderTheme) : LinkmarkDetailEvent
    data class OnSetReaderFontSize(val fontSize: ReaderFontSize) : LinkmarkDetailEvent
    data class OnSetReaderLineHeight(val lineHeight: ReaderLineHeight) : LinkmarkDetailEvent
    data class OnCreateHighlight(
        val readableVersionId: String,
        val startSectionKey: String,
        val startOffsetInSection: Int,
        val endSectionKey: String,
        val endOffsetInSection: Int,
        val colorRole: YabaColor = YabaColor.NONE,
        val note: String? = null,
        val quoteText: String? = null,
    ) : LinkmarkDetailEvent
    data class OnUpdateHighlight(
        val highlightId: String,
        val colorRole: YabaColor,
        val note: String?,
    ) : LinkmarkDetailEvent
    data class OnDeleteHighlight(val highlightId: String) : LinkmarkDetailEvent
    data class OnScrollToHighlight(val highlightId: String) : LinkmarkDetailEvent
    data object OnClearScrollToHighlight : LinkmarkDetailEvent

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
