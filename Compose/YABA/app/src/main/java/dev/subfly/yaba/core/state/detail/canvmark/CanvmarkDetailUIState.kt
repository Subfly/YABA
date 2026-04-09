package dev.subfly.yaba.core.state.detail.canvmark

import androidx.compose.runtime.Immutable
import dev.subfly.yaba.core.model.ui.BookmarkPreviewUiModel
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.computeDetailWebShellPhase
import dev.subfly.yaba.core.webview.CanvasHostMetrics
import dev.subfly.yaba.core.webview.CanvasHostStyleState

@Immutable
data class CanvmarkDetailUIState(
    val bookmark: BookmarkPreviewUiModel? = null,
    /** Last known scene JSON: from disk at bootstrap, refreshed after successful save. */
    val initialSceneJson: String? = null,
    /** Incremented only when the scene is first loaded from disk; not on save. */
    val canvasContentLoadGeneration: Int = 0,
    val isLoading: Boolean = false,
    val webContentLoadFailed: Boolean = false,
    val metrics: CanvasHostMetrics = CanvasHostMetrics(),
    val canvasStyle: CanvasHostStyleState = CanvasHostStyleState(),
    val optionsSheetVisible: Boolean = false,
    val pendingImageDataUrl: String? = null,
    val reminderDateEpochMillis: Long? = null,
)

fun CanvmarkDetailUIState.detailWebShellPhase(): DetailWebShellPhase =
    computeDetailWebShellPhase(
        isLoading = isLoading,
        hasWebPayload = initialSceneJson != null,
        webContentLoadFailed = webContentLoadFailed,
    )
