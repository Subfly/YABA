package dev.subfly.yaba.core.state.detail.canvmark

import dev.subfly.yaba.core.webview.CanvasHostMetrics
import dev.subfly.yaba.core.webview.WebShellLoadResult

sealed interface CanvmarkDetailEvent {
    data class OnInit(val bookmarkId: String) : CanvmarkDetailEvent
    data class OnSave(val sceneJson: String) : CanvmarkDetailEvent
    data class OnWebInitialContentLoad(val result: WebShellLoadResult) : CanvmarkDetailEvent
    data class OnCanvasMetricsChanged(val metrics: CanvasHostMetrics) : CanvmarkDetailEvent
    data object OnPickImageFromGallery : CanvmarkDetailEvent
    data object OnCaptureImageFromCamera : CanvmarkDetailEvent
    data object OnConsumedPendingImageInsert : CanvmarkDetailEvent
    data object OnDeleteBookmark : CanvmarkDetailEvent
}
