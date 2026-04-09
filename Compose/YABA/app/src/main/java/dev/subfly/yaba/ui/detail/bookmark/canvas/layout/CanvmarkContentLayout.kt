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
import androidx.compose.runtime.collectAsState
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
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.R
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailEvent
import dev.subfly.yaba.core.state.detail.canvmark.detailWebShellPhase
import dev.subfly.yaba.core.state.detail.canvmark.CanvmarkDetailUIState
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.navigation.creation.InlineLinkActionSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineLinkSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineMentionActionSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineMentionSheetRoute
import dev.subfly.yaba.core.navigation.main.CanvasDetailRoute
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.core.webview.CanvasInlineApplyJson
import dev.subfly.yaba.core.webview.CanvasLinkTapEvent
import dev.subfly.yaba.core.webview.CanvasMentionTapEvent
import dev.subfly.yaba.core.webview.CanvasSelectionLinkContext
import dev.subfly.yaba.core.webview.WebComponentUris
import dev.subfly.yaba.core.webview.parseYabaMentionLinkParams
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
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.InlineActionChoice
import dev.subfly.yaba.util.InlineLinkSheetResult
import dev.subfly.yaba.util.InlineMentionSheetResult
import dev.subfly.yaba.util.InlineSheetAction
import dev.subfly.yaba.util.PrivateBookmarkPasswordReason
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberPrivateBookmarkProtectedAction
import dev.subfly.yaba.util.rememberUrlLauncher
import kotlin.time.TimeSource
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
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
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val appState by appStateManager.state.collectAsState()
    val resultStore = LocalResultStore.current
    val openUrl = rememberUrlLauncher()

    val scope = rememberCoroutineScope()
    var canvasBridge by remember { mutableStateOf<WebViewCanvasBridge?>(null) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isCreationSheetOpening by remember { mutableStateOf(false) }
    var pendingCanvasLinkTap by remember { mutableStateOf<CanvasLinkTapEvent?>(null) }
    var pendingCanvasMentionTap by remember { mutableStateOf<CanvasMentionTapEvent?>(null) }
    var pendingCanvasExport by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    val folderAccent = remember(state.bookmark) { bookmarkFolderAccentColor(state.bookmark) }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)
    val webAppearance =
        if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val webShellPhase = remember(
        state.isLoading,
        state.initialSceneJson,
        state.webContentLoadFailed,
    ) { state.detailWebShellPhase() }

    suspend fun awaitCanvasBridge(
        timeoutMs: Long = 4_000L,
        pollMs: Long = 75L,
    ): WebViewCanvasBridge? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow().inWholeMilliseconds < timeoutMs) {
            val bridge = canvasBridge
            if (bridge != null) return bridge
            delay(pollMs)
        }
        return canvasBridge
    }

    suspend fun exportCanvasImageWithRetry(format: String, exportBackground: Boolean): ByteArray? {
        val bridge = awaitCanvasBridge() ?: return null
        val jsonReq = """{"format":"$format","exportBackground":$exportBackground}"""
        repeat(4) { attempt ->
            val bytes = bridge.exportCanvasImage(jsonReq)
            if (bytes != null && bytes.isNotEmpty()) return bytes
            if (attempt < 3) delay(120)
        }
        return null
    }

    val runCanvasExportIfAllowed = rememberPrivateBookmarkProtectedAction(
        model = state.bookmark,
        reason = PrivateBookmarkPasswordReason.EDIT_BOOKMARK,
    ) {
        val req =
            pendingCanvasExport.also { pendingCanvasExport = null }
                ?: return@rememberPrivateBookmarkProtectedAction
        val (fmt, bg) = req
        scope.launch {
            val bytes = exportCanvasImageWithRetry(fmt, bg) ?: return@launch
            val ext = if (fmt == "svg") "svg" else "png"
            onEvent(CanvmarkDetailEvent.OnExportImageReady(bytes = bytes, extension = ext))
        }
    }

    fun openBookmarkByKind(kindCode: Int, bookmarkId: String) {
        val route =
            when (BookmarkKind.fromCode(kindCode)) {
                BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = bookmarkId)
                BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = bookmarkId)
                BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = bookmarkId)
                BookmarkKind.FILE -> DocDetailRoute(bookmarkId = bookmarkId)
                BookmarkKind.CANVAS -> CanvasDetailRoute(bookmarkId = bookmarkId)
            }
        navigator.add(route)
    }

    fun openCreationSheet(route: NavKey) {
        if (appState.showCreationContent || isCreationSheetOpening) return
        isCreationSheetOpening = true
        try {
            creationNavigator.add(route)
            appStateManager.onShowCreationContent()
        } finally {
            isCreationSheetOpening = false
        }
    }

    suspend fun openCanvasLinkSheetFromToolbar() {
        val bridge = canvasBridge ?: return
        val ctx = CanvasSelectionLinkContext.parse(bridge.getCanvasSelectionLinkContext())
        val link = ctx.link
        val mention = parseYabaMentionLinkParams(link)
        val selectedId = ctx.selectedIds.firstOrNull()
        val isMentionLink = mention != null
        openCreationSheet(
            InlineLinkSheetRoute(
                initialText = ctx.primaryText,
                initialUrl =
                    when {
                        link == null -> ""
                        isMentionLink -> ""
                        else -> link
                    },
                isEdit = ctx.hasSelection && link != null && !isMentionLink,
                canvasElementId =
                    if (ctx.hasSelection) {
                        selectedId
                    } else {
                        null
                    },
            ),
        )
    }

    suspend fun openCanvasMentionSheetFromToolbar() {
        val bridge = canvasBridge ?: return
        val ctx = CanvasSelectionLinkContext.parse(bridge.getCanvasSelectionLinkContext())
        val link = ctx.link
        val mention = parseYabaMentionLinkParams(link)
        val selectedId = ctx.selectedIds.firstOrNull()
        openCreationSheet(
            InlineMentionSheetRoute(
                initialText = ctx.primaryText,
                initialBookmarkId = mention?.bookmarkId,
                isEdit = ctx.hasSelection && mention != null,
                canvasElementId =
                    if (ctx.hasSelection) {
                        selectedId
                    } else {
                        null
                    },
            ),
        )
    }

    suspend fun consumePendingCanvasInlineInsertResults(bridge: WebViewCanvasBridge) {
        val linkResult =
            resultStore.getResult<InlineLinkSheetResult>(ResultStoreKeys.INLINE_LINK_INSERT)
        if (linkResult != null) {
            resultStore.removeResult(ResultStoreKeys.INLINE_LINK_INSERT)
            when (linkResult.action) {
                InlineSheetAction.REMOVE -> {
                    val id = linkResult.canvasElementId
                    if (id != null) {
                        bridge.applyCanvasInline(CanvasInlineApplyJson.clearLink(id))
                    }
                }

                InlineSheetAction.INSERT_OR_UPDATE -> {
                    val id = linkResult.canvasElementId
                    if (id != null) {
                        bridge.applyCanvasInline(CanvasInlineApplyJson.setUrlOnElement(id, linkResult.url))
                    } else {
                        bridge.applyCanvasInline(
                            CanvasInlineApplyJson.insertTextWithUrl(linkResult.text, linkResult.url),
                        )
                    }
                }
            }
        }

        val mentionResult =
            resultStore.getResult<InlineMentionSheetResult>(ResultStoreKeys.INLINE_MENTION_INSERT)
        if (mentionResult != null) {
            resultStore.removeResult(ResultStoreKeys.INLINE_MENTION_INSERT)
            when (mentionResult.action) {
                InlineSheetAction.REMOVE -> {
                    val id = mentionResult.canvasElementId
                    if (id != null) {
                        bridge.applyCanvasInline(CanvasInlineApplyJson.clearLink(id))
                    }
                }

                InlineSheetAction.INSERT_OR_UPDATE -> {
                    val id = mentionResult.canvasElementId
                    if (id != null) {
                        bridge.applyCanvasInline(
                            CanvasInlineApplyJson.setMentionOnElement(
                                elementId = id,
                                text = mentionResult.text,
                                bookmarkId = mentionResult.bookmarkId,
                                bookmarkKindCode = mentionResult.bookmarkKindCode,
                                bookmarkLabel = mentionResult.bookmarkLabel,
                            ),
                        )
                    } else {
                        bridge.applyCanvasInline(
                            CanvasInlineApplyJson.insertTextWithMention(
                                displayText = mentionResult.text,
                                bookmarkId = mentionResult.bookmarkId,
                                bookmarkKindCode = mentionResult.bookmarkKindCode,
                                bookmarkLabel = mentionResult.bookmarkLabel,
                            ),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(appState.showCreationContent, canvasBridge) {
        val bridge = canvasBridge ?: return@LaunchedEffect
        if (appState.showCreationContent) return@LaunchedEffect
        consumePendingCanvasInlineInsertResults(bridge)
    }

    LaunchedEffect(
        resultStore.getResult(ResultStoreKeys.INLINE_LINK_ACTION),
        pendingCanvasLinkTap,
        canvasBridge,
    ) {
        val action =
            resultStore.getResult<InlineActionChoice>(ResultStoreKeys.INLINE_LINK_ACTION)
                ?: return@LaunchedEffect
        val tap = pendingCanvasLinkTap ?: return@LaunchedEffect
        if (action == InlineActionChoice.REMOVE && canvasBridge == null) {
            return@LaunchedEffect
        }
        resultStore.removeResult(ResultStoreKeys.INLINE_LINK_ACTION)
        when (action) {
            InlineActionChoice.OPEN -> {
                openUrl(tap.url)
                pendingCanvasLinkTap = null
            }

            InlineActionChoice.EDIT -> {
                openCreationSheet(
                    InlineLinkSheetRoute(
                        initialText = tap.text,
                        initialUrl = tap.url,
                        isEdit = true,
                        canvasElementId = tap.elementId,
                    ),
                )
                pendingCanvasLinkTap = null
            }

            InlineActionChoice.REMOVE -> {
                val bridge = canvasBridge ?: return@LaunchedEffect
                bridge.applyCanvasInline(CanvasInlineApplyJson.clearLink(tap.elementId))
                pendingCanvasLinkTap = null
            }
        }
    }

    LaunchedEffect(
        resultStore.getResult(ResultStoreKeys.INLINE_MENTION_ACTION),
        pendingCanvasMentionTap,
        canvasBridge,
    ) {
        val action =
            resultStore.getResult<InlineActionChoice>(ResultStoreKeys.INLINE_MENTION_ACTION)
                ?: return@LaunchedEffect
        val tap = pendingCanvasMentionTap ?: return@LaunchedEffect
        if (action == InlineActionChoice.REMOVE && canvasBridge == null) {
            return@LaunchedEffect
        }
        resultStore.removeResult(ResultStoreKeys.INLINE_MENTION_ACTION)
        when (action) {
            InlineActionChoice.OPEN -> {
                openBookmarkByKind(tap.bookmarkKindCode, tap.bookmarkId)
                pendingCanvasMentionTap = null
            }

            InlineActionChoice.EDIT -> {
                openCreationSheet(
                    InlineMentionSheetRoute(
                        initialText = tap.text,
                        initialBookmarkId = tap.bookmarkId,
                        isEdit = true,
                        canvasElementId = tap.elementId,
                    ),
                )
                pendingCanvasMentionTap = null
            }

            InlineActionChoice.REMOVE -> {
                val bridge = canvasBridge ?: return@LaunchedEffect
                bridge.applyCanvasInline(CanvasInlineApplyJson.clearLink(tap.elementId))
                pendingCanvasMentionTap = null
            }
        }
    }

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
                                    sceneLoadGeneration = state.canvasContentLoadGeneration,
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

                                        is YabaWebHostEvent.CanvasLinkTap -> {
                                            pendingCanvasLinkTap = event.tap
                                            scope.launch {
                                                openCreationSheet(
                                                    InlineLinkActionSheetRoute(
                                                        text = event.tap.text,
                                                        url = event.tap.url,
                                                        editPos = -1,
                                                        canvasElementId = event.tap.elementId,
                                                    ),
                                                )
                                            }
                                        }

                                        is YabaWebHostEvent.CanvasMentionTap -> {
                                            pendingCanvasMentionTap = event.tap
                                            scope.launch {
                                                openCreationSheet(
                                                    InlineMentionActionSheetRoute(
                                                        text = event.tap.text,
                                                        bookmarkId = event.tap.bookmarkId,
                                                        bookmarkKindCode = event.tap.bookmarkKindCode,
                                                        editPos = -1,
                                                        canvasElementId = event.tap.elementId,
                                                    ),
                                                )
                                            }
                                        }

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
                    onOpenLinkSheet = {
                        scope.launch { openCanvasLinkSheetFromToolbar() }
                    },
                    onOpenMentionSheet = {
                        scope.launch { openCanvasMentionSheetFromToolbar() }
                    },
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
                        onShowRemindMePicker = onShowRemindMePicker,
                        onExportPng = { exportBackground ->
                            pendingCanvasExport = "png" to exportBackground
                            runCanvasExportIfAllowed()
                        },
                        onExportSvg = { exportBackground ->
                            pendingCanvasExport = "svg" to exportBackground
                            runCanvasExportIfAllowed()
                        },
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
