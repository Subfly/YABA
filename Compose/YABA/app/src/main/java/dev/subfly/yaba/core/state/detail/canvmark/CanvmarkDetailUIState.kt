package dev.subfly.yaba.core.state.detail.canvmark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.computeDetailWebShellPhase
import dev.subfly.yaba.core.webview.CanvasHostMetrics

@Immutable
data class CanvmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    val initialSceneJson: String? = null,
    val isLoading: Boolean = false,
    val webContentLoadFailed: Boolean = false,
    val metrics: CanvasHostMetrics = CanvasHostMetrics(),
    val pendingImageDataUrl: String? = null,
)

fun CanvmarkDetailUIState.detailWebShellPhase(): DetailWebShellPhase =
    computeDetailWebShellPhase(
        isLoading = isLoading,
        hasWebPayload = initialSceneJson != null,
        webContentLoadFailed = webContentLoadFailed,
    )
