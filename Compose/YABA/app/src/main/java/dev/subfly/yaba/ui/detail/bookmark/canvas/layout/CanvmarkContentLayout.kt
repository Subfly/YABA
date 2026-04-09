package dev.subfly.yaba.ui.detail.bookmark.canvas.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.subfly.yaba.R
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailEvent
import dev.subfly.yaba.core.state.detail.canvmark.detailWebShellPhase
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailUIState
import dev.subfly.yaba.core.webview.WebComponentUris
import dev.subfly.yaba.core.webview.WebViewCanvasBridge
import dev.subfly.yaba.core.webview.YabaWebAppearance
import dev.subfly.yaba.core.webview.YabaWebFeature
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import dev.subfly.yaba.core.webview.YabaWebPlatform
import dev.subfly.yaba.ui.detail.bookmark.canvas.components.CanvmarkCanvasOptionsSheet
import dev.subfly.yaba.ui.detail.bookmark.canvas.components.CanvmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.canvas.components.CanvmarkEditorToolbar
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailTopBarScrim
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalContentNavigator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CanvmarkContentLayout(
    modifier: Modifier = Modifier,
    state: CanvmarkDetailUIState,
    onShowDetail: () -> Unit,
    onEvent: (CanvmarkDetailEvent) -> Unit,
) {
    val navigator = LocalContentNavigator.current

    val scope = rememberCoroutineScope()
    var canvasBridge by remember { mutableStateOf<WebViewCanvasBridge?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    val folderAccent = remember(state.bookmark) { bookmarkFolderAccentColor(state.bookmark) }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)
    val webAppearance =
        if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val webShellPhase = remember(
        state.isLoading,
        state.initialSceneJson,
        state.webContentLoadFailed,
    ) { state.detailWebShellPhase() }

    LaunchedEffect(canvasBridge, state.bookmark?.id) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                val bridge = canvasBridge ?: return@withContext
                val json = bridge.getSceneJson()
                onEvent(CanvmarkDetailEvent.OnSave(sceneJson = json))
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        val bridge = canvasBridge ?: return@LifecycleEventEffect
        scope.launch {
            val json = bridge.getSceneJson()
            onEvent(CanvmarkDetailEvent.OnSave(sceneJson = json))
        }
    }

    LaunchedEffect(state.pendingImageDataUrl, canvasBridge) {
        val dataUrl = state.pendingImageDataUrl ?: return@LaunchedEffect
        val bridge = canvasBridge ?: return@LaunchedEffect
        bridge.insertImageFromDataUrl(dataUrl)
        onEvent(CanvmarkDetailEvent.OnConsumedPendingImageInsert)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .fillMaxWidth()) {
                when (webShellPhase) {
                    DetailWebShellPhase.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularWavyProgressIndicator()
                        }
                    }

                    DetailWebShellPhase.Unavailable -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            NoContentView(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center),
                                iconName = "cancel-square",
                                labelRes = R.string.reader_not_available_title,
                            ) {
                                Text(text = stringResource(R.string.reader_not_available_description))
                            }
                        }
                    }

                    DetailWebShellPhase.Bootstrapping,
                    DetailWebShellPhase.Ready -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            YabaWebView(
                                modifier = Modifier.fillMaxSize(),
                                baseUrl = WebComponentUris.getCanvasUri(),
                                feature = YabaWebFeature.Canvas(
                                    initialSceneJson = state.initialSceneJson.orEmpty(),
                                    platform = YabaWebPlatform.Compose,
                                    appearance = webAppearance,
                                ),
                                onHostEvent = { event ->
                                    when (event) {
                                        is YabaWebHostEvent.InitialContentLoad ->
                                            onEvent(CanvmarkDetailEvent.OnWebInitialContentLoad(event.result))

                                        is YabaWebHostEvent.CanvasIdleForAutosave -> {
                                            scope.launch {
                                                val json = canvasBridge?.getSceneJson().orEmpty()
                                                if (json.isNotBlank()) {
                                                    onEvent(CanvmarkDetailEvent.OnSave(json))
                                                }
                                            }
                                        }

                                        is YabaWebHostEvent.CanvasMetrics ->
                                            onEvent(CanvmarkDetailEvent.OnCanvasMetricsChanged(event.metrics))

                                        is YabaWebHostEvent.CanvasStyleState ->
                                            onEvent(CanvmarkDetailEvent.OnCanvasStyleStateChanged(event.style))

                                        else -> Unit
                                    }
                                },
                                onCanvasBridgeReady = { bridge ->
                                    canvasBridge = bridge
                                },
                            )
                            if (webShellPhase == DetailWebShellPhase.Bootstrapping) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularWavyProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }

            if (webShellPhase == DetailWebShellPhase.Ready) {
                CanvmarkEditorToolbar(
                    modifier = Modifier.fillMaxWidth(),
                    color = folderAccent,
                    metrics = state.metrics,
                    optionsSheetVisible = state.optionsSheetVisible,
                    onToolSelected = { tool ->
                        scope.launch { canvasBridge?.setActiveTool(tool) }
                    },
                    onUndo = { scope.launch { canvasBridge?.undo() } },
                    onRedo = { scope.launch { canvasBridge?.redo() } },
                    onToggleOptionsSheet = { onEvent(CanvmarkDetailEvent.OnToggleCanvasOptionsSheet) },
                    onPickImageFromGallery = { onEvent(CanvmarkDetailEvent.OnPickImageFromGallery) },
                    onCaptureImageFromCamera = { onEvent(CanvmarkDetailEvent.OnCaptureImageFromCamera) },
                    onToggleGridMode = { scope.launch { canvasBridge?.toggleGridMode() } },
                    onToggleObjectsSnapMode = { scope.launch { canvasBridge?.toggleObjectsSnapMode() } },
                    onSaveDocument = {
                        scope.launch {
                            val bridge = canvasBridge ?: return@launch
                            val json = bridge.getSceneJson()
                            onEvent(CanvmarkDetailEvent.OnSave(sceneJson = json))
                        }
                    },
                )
            }
        }

        BookmarkDetailContentTopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = folderAccent,
            scrim = BookmarkDetailTopBarScrim.Subtle,
            onBack = navigator::removeLastOrNull,
            onShowDetail = onShowDetail,
            overflowMenu = {
                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    IconButton(
                        onClick = { isMenuExpanded = !isMenuExpanded },
                        colors = menuIconButtonColors,
                        shapes = IconButtonDefaults.shapes(),
                    ) { YabaIcon(name = "more-horizontal-circle-02", color = Color.White) }

                    CanvmarkContentDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        state = state,
                        onEvent = onEvent,
                    )
                }
            },
            loadingIndicator = {},
        )

        if (webShellPhase == DetailWebShellPhase.Bootstrapping ||
            webShellPhase == DetailWebShellPhase.Ready
        ) {
            val optionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val canShowOptionsSheet =
                state.metrics.activeTool == "selection" && state.metrics.hasSelection

            LaunchedEffect(state.optionsSheetVisible, canShowOptionsSheet) {
                if (state.optionsSheetVisible && canShowOptionsSheet) {
                    optionsSheetState.show()
                } else {
                    optionsSheetState.hide()
                }
            }

            if (optionsSheetState.isVisible || state.optionsSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { onEvent(CanvmarkDetailEvent.OnDismissCanvasOptionsSheet) },
                    sheetState = optionsSheetState,
                    scrimColor = Color.Transparent,
                    sheetGesturesEnabled = false,
                    dragHandle = { BottomSheetDefaults.DragHandle() },
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    if (canShowOptionsSheet) {
                        CanvmarkCanvasOptionsSheet(
                            style = state.canvasStyle,
                            onApplyPatch = { patch ->
                                scope.launch {
                                    canvasBridge?.applySelectionStyle(patch.toJsonString())
                                }
                            },
                            onLayer = { action ->
                                scope.launch {
                                    canvasBridge?.canvasLayer(action)
                                }
                            },
                            onDeleteSelected = {
                                scope.launch { canvasBridge?.deleteSelected() }
                            },
                        )
                    }
                }
            }
        }
    }
}
