package dev.subfly.yaba.core.state.detail.canvmark

import dev.subfly.yaba.core.webview.CanvasHostMetrics
import dev.subfly.yaba.core.webview.CanvasHostStyleState
import dev.subfly.yaba.core.webview.WebShellLoadResult

sealed interface CanvmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : CanvmarkDetailEvent
    data class OnSave(val sceneJson: String) : CanvmarkDetailEvent
    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : CanvmarkDetailEvent
    data class OnCanvasMetricsChanged(val metrics: CanvasHostMetrics) : CanvmarkDetailEvent
    data class OnCanvasStyleStateChanged(val style: CanvasHostStyleState) : CanvmarkDetailEvent
    data object OnToggleCanvasOptionsSheet : CanvmarkDetailEvent
    data object OnDismissCanvasOptionsSheet : CanvmarkDetailEvent
    data object OnPickImageFromGallery : CanvmarkDetailEvent
    data object OnCaptureImageFromCamera : CanvmarkDetailEvent
    data object OnConsumedPendingImageInsert : CanvmarkDetailEvent
    data object OnDeleteBookmark : CanvmarkDetailEvent
    data class OnScheduleReminder(
        val title: String,
        val message: String,
        val triggerAtEpochMillis: Long,
    ) : CanvmarkDetailEvent
    data object OnCancelReminder : CanvmarkDetailEvent
    data class OnExportImageReady(val bytes: ByteArray, val extension: String) : CanvmarkDetailEvent
}
