package dev.subfly.yaba.ui.detail.bookmark.note.layout

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.NavKey
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.ColorSelectionRoute
import dev.subfly.yaba.core.navigation.creation.InlineLinkActionSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineLinkSheetRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkMathSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineMentionActionSheetRoute
import dev.subfly.yaba.core.navigation.creation.InlineMentionSheetRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkTableCreationRoute
import dev.subfly.yaba.core.navigation.main.DocDetailRoute
import dev.subfly.yaba.core.navigation.main.CanvasDetailRoute
import dev.subfly.yaba.core.navigation.main.ImageDetailRoute
import dev.subfly.yaba.core.navigation.main.LinkDetailRoute
import dev.subfly.yaba.core.navigation.main.NoteDetailRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkEditorToolbar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.InlineActionChoice
import dev.subfly.yaba.util.InlineLinkSheetResult
import dev.subfly.yaba.util.InlineMentionSheetResult
import dev.subfly.yaba.util.InlineSheetAction
import dev.subfly.yaba.util.NotemarkMathSheetResult
import dev.subfly.yaba.util.NotemarkTableSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yaba.core.model.utils.BookmarkKind
import dev.subfly.yaba.core.model.utils.ReaderPreferences
import dev.subfly.yaba.core.model.utils.YabaColor
import dev.subfly.yaba.core.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.notemark.NotemarkDetailUIState
import dev.subfly.yaba.core.state.detail.notemark.detailWebShellPhase
import dev.subfly.yaba.core.webview.EditorFormattingState
import dev.subfly.yaba.core.webview.InlineLinkTapEvent
import dev.subfly.yaba.core.webview.InlineMentionTapEvent
import dev.subfly.yaba.core.webview.MathTapEvent
import dev.subfly.yaba.core.webview.WebComponentUris
import dev.subfly.yaba.core.webview.WebViewEditorBridge
import dev.subfly.yaba.core.webview.YabaEditorCommands
import dev.subfly.yaba.core.webview.YabaWebAppearance
import dev.subfly.yaba.core.webview.YabaWebFeature
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import dev.subfly.yaba.core.webview.YabaWebPlatform
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun NotemarkContentLayout(
    modifier: Modifier = Modifier,
    state: NotemarkDetailUIState,
    onShowDetail: () -> Unit,
    onEvent: (NotemarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val appState by appStateManager.state.collectAsState()
    val resultStore = LocalResultStore.current

    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlLauncher()
    val prepareEditorForCreationSheet = rememberPrepareEditorForCreationSheet()
    val notePlaceholderText = "Start your note..." // TODO: localize placeholder text

    var editorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    var editorFormatting by remember(state.bookmark?.id) { mutableStateOf(EditorFormattingState()) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isCreationSheetOpening by remember { mutableStateOf(false) }
    var previousShowCreationContent by remember { mutableStateOf(false) }
    var pendingLinkTap by remember { mutableStateOf<InlineLinkTapEvent?>(null) }
    var pendingMentionTap by remember { mutableStateOf<InlineMentionTapEvent?>(null) }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    val webAppearance =
        if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val webShellPhase = remember(
        state.isLoading,
        state.initialDocumentJson,
        state.webContentLoadFailed,
    ) { state.detailWebShellPhase() }

    suspend fun awaitEditorBridge(
        timeoutMs: Long = 4_000L,
        pollMs: Long = 75L,
    ): WebViewEditorBridge? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow().inWholeMilliseconds < timeoutMs) {
            val bridge = editorBridge
            if (bridge != null) return bridge
            delay(pollMs)
        }
        return editorBridge
    }

    suspend fun exportMarkdownWithRetry(): String {
        val bridge = awaitEditorBridge() ?: return ""
        bridge.unFocus()
        repeat(4) { attempt ->
            val markdown = bridge.exportNoteMarkdown()
            if (markdown.isNotBlank()) return markdown
            if (attempt < 3) delay(120)
        }
        return ""
    }

    suspend fun exportPdfBase64WithRetry(): String {
        val bridge = awaitEditorBridge() ?: return ""
        bridge.unFocus()
        repeat(4) { attempt ->
            val base64 = bridge.exportNotePdfBase64()
            if (base64.isNotBlank()) return base64
            if (attempt < 3) delay(120)
        }
        return ""
    }

    suspend fun openCreationSheet(route: NavKey) {
        if (appState.showCreationContent || isCreationSheetOpening) return
        isCreationSheetOpening = true
        try {
            prepareEditorForCreationSheet(editorBridge)
            creationNavigator.add(route)
            appStateManager.onShowCreationContent()
        } finally {
            isCreationSheetOpening = false
        }
    }

    fun openBookmarkByKind(kindCode: Int, bookmarkId: String) {
        val route = when (BookmarkKind.fromCode(kindCode)) {
            BookmarkKind.LINK -> LinkDetailRoute(bookmarkId = bookmarkId)
            BookmarkKind.NOTE -> NoteDetailRoute(bookmarkId = bookmarkId)
            BookmarkKind.IMAGE -> ImageDetailRoute(bookmarkId = bookmarkId)
            BookmarkKind.FILE -> DocDetailRoute(bookmarkId = bookmarkId)
            BookmarkKind.CANVAS -> CanvasDetailRoute(bookmarkId = bookmarkId)
        }
        navigator.add(route)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        val bridge = editorBridge ?: return@LifecycleEventEffect
        scope.launch {
            emitNotemarkSaveFromBridge(bridge, onEvent)
        }
    }

    LaunchedEffect(editorBridge, state.bookmark?.id) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                val bridge = editorBridge ?: return@withContext
                emitNotemarkSaveFromBridge(bridge, onEvent)
            }
        }
    }

    LaunchedEffect(appState.showCreationContent, editorBridge) {
        val show = appState.showCreationContent
        if (previousShowCreationContent && show.not()) {
            editorBridge?.focus()
        }
        previousShowCreationContent = show
    }

    LaunchedEffect(state.pendingTocNavigate, editorBridge) {
        val pending = state.pendingTocNavigate ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        bridge.navigateToTocItem(pending.first, pending.second)
        onEvent(NotemarkDetailEvent.OnClearTocNavigation)
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.NOTEMARK_TABLE_INSERT), editorBridge) {
        val r =
            resultStore.getResult<NotemarkTableSheetResult>(ResultStoreKeys.NOTEMARK_TABLE_INSERT)
                ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.NOTEMARK_TABLE_INSERT)
        bridge.dispatch(YabaEditorCommands.insertTablePayload(r.rows, r.cols, r.withHeaderRow))
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.NOTEMARK_MATH_INSERT), editorBridge) {
        val r = resultStore.getResult<NotemarkMathSheetResult>(ResultStoreKeys.NOTEMARK_MATH_INSERT)
            ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.NOTEMARK_MATH_INSERT)
        when {
            r.isEdit && r.editPos != null -> {
                if (r.isBlock) {
                    bridge.dispatch(YabaEditorCommands.updateBlockMathPayload(r.latex, r.editPos))
                } else {
                    bridge.dispatch(YabaEditorCommands.updateInlineMathPayload(r.latex, r.editPos))
                }
            }

            else -> {
                if (r.isBlock) {
                    bridge.dispatch(YabaEditorCommands.insertBlockMathPayload(r.latex))
                } else {
                    bridge.dispatch(YabaEditorCommands.insertInlineMathPayload(r.latex))
                }
            }
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.INLINE_LINK_INSERT), editorBridge) {
        val r = resultStore.getResult<InlineLinkSheetResult>(ResultStoreKeys.INLINE_LINK_INSERT)
            ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.INLINE_LINK_INSERT)
        when {
            r.action == InlineSheetAction.REMOVE && r.editPos != null -> {
                bridge.dispatch(YabaEditorCommands.removeLinkPayload(r.editPos))
            }

            r.editPos != null -> {
                bridge.dispatch(YabaEditorCommands.updateLinkPayload(r.text, r.url, r.editPos))
            }

            else -> {
                bridge.dispatch(YabaEditorCommands.insertLinkPayload(r.text, r.url))
            }
        }
        bridge.focus()
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.INLINE_MENTION_INSERT), editorBridge) {
        val r =
            resultStore.getResult<InlineMentionSheetResult>(ResultStoreKeys.INLINE_MENTION_INSERT)
                ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.INLINE_MENTION_INSERT)
        when {
            r.action == InlineSheetAction.REMOVE && r.editPos != null -> {
                bridge.dispatch(YabaEditorCommands.removeMentionPayload(r.editPos))
            }

            r.editPos != null -> {
                bridge.dispatch(
                    YabaEditorCommands.updateMentionPayload(
                        text = r.text,
                        bookmarkId = r.bookmarkId,
                        bookmarkKindCode = r.bookmarkKindCode,
                        bookmarkLabel = r.bookmarkLabel,
                        pos = r.editPos,
                    ),
                )
            }

            else -> {
                bridge.dispatch(
                    YabaEditorCommands.insertMentionPayload(
                        text = r.text,
                        bookmarkId = r.bookmarkId,
                        bookmarkKindCode = r.bookmarkKindCode,
                        bookmarkLabel = r.bookmarkLabel,
                    ),
                )
            }
        }
        bridge.focus()
    }

    LaunchedEffect(
        resultStore.getResult(ResultStoreKeys.INLINE_LINK_ACTION),
        pendingLinkTap,
        editorBridge
    ) {
        val action =
            resultStore.getResult<InlineActionChoice>(ResultStoreKeys.INLINE_LINK_ACTION)
                ?: return@LaunchedEffect
        val tap = pendingLinkTap ?: return@LaunchedEffect
        if (action == InlineActionChoice.REMOVE && editorBridge == null) {
            return@LaunchedEffect
        }
        resultStore.removeResult(ResultStoreKeys.INLINE_LINK_ACTION)
        when (action) {
            InlineActionChoice.OPEN -> {
                openUrl(tap.url)
                pendingLinkTap = null
            }

            InlineActionChoice.EDIT -> {
                openCreationSheet(
                    InlineLinkSheetRoute(
                        initialText = tap.text,
                        initialUrl = tap.url,
                        isEdit = true,
                        editPos = tap.documentPos,
                    ),
                )
                pendingLinkTap = null
            }

            InlineActionChoice.REMOVE -> {
                val bridge = editorBridge ?: return@LaunchedEffect
                bridge.dispatch(YabaEditorCommands.removeLinkPayload(tap.documentPos))
                pendingLinkTap = null
                bridge.focus()
            }
        }
    }

    LaunchedEffect(
        resultStore.getResult(ResultStoreKeys.INLINE_MENTION_ACTION),
        pendingMentionTap,
        editorBridge
    ) {
        val action =
            resultStore.getResult<InlineActionChoice>(ResultStoreKeys.INLINE_MENTION_ACTION)
                ?: return@LaunchedEffect
        val tap = pendingMentionTap ?: return@LaunchedEffect
        if (action == InlineActionChoice.REMOVE && editorBridge == null) {
            return@LaunchedEffect
        }
        resultStore.removeResult(ResultStoreKeys.INLINE_MENTION_ACTION)
        when (action) {
            InlineActionChoice.OPEN -> {
                openBookmarkByKind(tap.bookmarkKindCode, tap.bookmarkId)
                pendingMentionTap = null
            }

            InlineActionChoice.EDIT -> {
                openCreationSheet(
                    InlineMentionSheetRoute(
                        initialText = tap.text,
                        initialBookmarkId = tap.bookmarkId,
                        isEdit = true,
                        editPos = tap.documentPos,
                    ),
                )
                pendingMentionTap = null
            }

            InlineActionChoice.REMOVE -> {
                val bridge = editorBridge ?: return@LaunchedEffect
                bridge.dispatch(YabaEditorCommands.removeMentionPayload(tap.documentPos))
                pendingMentionTap = null
                bridge.focus()
            }
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_COLOR), editorBridge) {
        val picked = resultStore.getResult<YabaColor>(ResultStoreKeys.SELECTED_COLOR)
            ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.SELECTED_COLOR)
        when (picked) {
            YabaColor.NONE -> bridge.dispatch(YabaEditorCommands.UnsetTextHighlight)
            else -> bridge.dispatch(YabaEditorCommands.setTextHighlightPayload(picked))
        }
        bridge.focus()
    }

    LaunchedEffect(state.inlineImageDocumentSrc, editorBridge) {
        val src = state.inlineImageDocumentSrc ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        bridge.dispatch(YabaEditorCommands.insertImagePayload(src))
        onEvent(NotemarkDetailEvent.OnConsumedInlineImageInsert)
        bridge.focus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
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
                        ) { CircularWavyProgressIndicator() }
                    }

                    DetailWebShellPhase.Unavailable -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            NoContentView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .wrapContentSize(Alignment.Center),
                                iconName = "cancel-square",
                                labelRes = R.string.reader_not_available_title,
                            ) { Text(text = stringResource(R.string.reader_not_available_description)) }
                        }
                    }

                    DetailWebShellPhase.Bootstrapping,
                    DetailWebShellPhase.Ready -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            YabaWebView(
                                modifier = Modifier.fillMaxSize(),
                                baseUrl = WebComponentUris.getEditorUri(),
                                feature = YabaWebFeature.Editor(
                                    initialDocumentJson = state.initialDocumentJson.orEmpty(),
                                    assetsBaseUrl = state.assetsBaseUrl,
                                    placeholderText = notePlaceholderText,
                                    platform = YabaWebPlatform.Compose,
                                    appearance = webAppearance,
                                    readerPreferences = ReaderPreferences(),
                                    documentLoadGeneration = state.editorContentLoadGeneration,
                                ),
                                onHostEvent = { ev ->
                                    when (ev) {
                                        is YabaWebHostEvent.ReaderMetrics -> {
                                            ev.editorFormatting?.let { editorFormatting = it }
                                        }

                                        is YabaWebHostEvent.InitialContentLoad ->
                                            onEvent(NotemarkDetailEvent.OnWebInitialContentLoad(ev.result))

                                        is YabaWebHostEvent.TableOfContentsChanged ->
                                            onEvent(NotemarkDetailEvent.OnTocChanged(ev.toc))

                                        is YabaWebHostEvent.NoteEditorIdleForAutosave -> {
                                            scope.launch {
                                                val bridge = editorBridge ?: return@launch
                                                emitNotemarkSaveFromBridge(bridge, onEvent)
                                            }
                                        }

                                        else -> Unit
                                    }
                                },
                                onUrlClick = openUrl,
                                onScrollDirectionChanged = { _ -> },
                                onReaderBridgeReady = {},
                                onEditorBridgeReady = { editorBridge = it },
                                onAnnotationTap = {},
                                onMathTap = { ev: MathTapEvent ->
                                    scope.launch {
                                        openCreationSheet(
                                            NotemarkMathSheetRoute(
                                                isBlock = ev.isBlock,
                                                initialLatex = ev.latex,
                                                isEdit = true,
                                                editPos = ev.documentPos,
                                            ),
                                        )
                                    }
                                },
                                onInlineLinkTap = { ev ->
                                    pendingLinkTap = ev
                                    scope.launch {
                                        openCreationSheet(
                                            InlineLinkActionSheetRoute(
                                                text = ev.text,
                                                url = ev.url,
                                                editPos = ev.documentPos,
                                            ),
                                        )
                                    }
                                },
                                onInlineMentionTap = { ev ->
                                    pendingMentionTap = ev
                                    scope.launch {
                                        openCreationSheet(
                                            InlineMentionActionSheetRoute(
                                                text = ev.text,
                                                bookmarkId = ev.bookmarkId,
                                                bookmarkKindCode = ev.bookmarkKindCode,
                                                editPos = ev.documentPos,
                                            ),
                                        )
                                    }
                                },
                            )
                            if (webShellPhase == DetailWebShellPhase.Bootstrapping) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) { CircularWavyProgressIndicator() }
                            }
                        }
                    }
                }
            }

            if (webShellPhase == DetailWebShellPhase.Ready) {
                NotemarkEditorToolbar(
                    modifier = Modifier.fillMaxWidth(),
                    color = folderAccent,
                    formatting = editorFormatting,
                    onHighlightInactiveClick = {
                        scope.launch {
                            openCreationSheet(ColorSelectionRoute(selectedColor = YabaColor.NONE))
                        }
                    },
                    onHighlightActiveClick = {
                        scope.launch {
                            editorBridge?.dispatch(YabaEditorCommands.ToggleTextHighlight)
                        }
                    },
                    onDispatchCommand = { payload ->
                        scope.launch { editorBridge?.dispatch(payload) }
                    },
                    onOpenTableInsertSheet = {
                        scope.launch {
                            openCreationSheet(NotemarkTableCreationRoute())
                        }
                    },
                    onOpenMathSheet = { isBlock ->
                        scope.launch {
                            openCreationSheet(
                                NotemarkMathSheetRoute(
                                    isBlock = isBlock,
                                    initialLatex = "",
                                    isEdit = false,
                                    editPos = null,
                                ),
                            )
                        }
                    },
                    onOpenLinkSheet = {
                        scope.launch {
                            val selectedText = editorBridge?.getSelectedText().orEmpty()
                            openCreationSheet(
                                InlineLinkSheetRoute(
                                    initialText = selectedText,
                                ),
                            )
                        }
                    },
                    onOpenMentionSheet = {
                        scope.launch {
                            val selectedText = editorBridge?.getSelectedText().orEmpty()
                            openCreationSheet(
                                InlineMentionSheetRoute(
                                    initialText = selectedText,
                                ),
                            )
                        }
                    },
                    onPickImageFromGallery = {
                        scope.launch {
                            editorBridge?.unFocus()
                            onEvent(NotemarkDetailEvent.OnPickImageFromGallery)
                        }
                    },
                    onCaptureImageFromCamera = {
                        scope.launch {
                            editorBridge?.unFocus()
                            onEvent(NotemarkDetailEvent.OnCaptureImageFromCamera)
                        }
                    },
                    onSaveDocument = {
                        scope.launch {
                            val bridge = editorBridge ?: return@launch
                            emitNotemarkSaveFromBridge(bridge, onEvent)
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
            onBack = navigator::removeLastOrNull,
            onShowDetail = onShowDetail,
            overflowMenu = {
                Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                    IconButton(
                        onClick = { isMenuExpanded = !isMenuExpanded },
                        colors = menuIconButtonColors,
                        shapes = IconButtonDefaults.shapes(),
                    ) { YabaIcon(name = "more-horizontal-circle-02", color = Color.White) }

                    NotemarkContentDropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                        state = state,
                        onEvent = onEvent,
                        onShowRemindMePicker = onShowRemindMePicker,
                        onExportMarkdown = {
                            scope.launch {
                                val markdown = exportMarkdownWithRetry()
                                onEvent(NotemarkDetailEvent.OnExportMarkdownReady(markdown = markdown))
                            }
                        },
                        onExportPdf = {
                            scope.launch {
                                val b64 = exportPdfBase64WithRetry()
                                onEvent(NotemarkDetailEvent.OnExportPdfReady(pdfBase64 = b64))
                            }
                        },
                    )
                }
            },
            loadingIndicator = {},
        )
    }
}

private suspend fun emitNotemarkSaveFromBridge(
    bridge: WebViewEditorBridge,
    onEvent: (NotemarkDetailEvent) -> Unit,
) {
    val json = bridge.getDocumentJson()
    val used = bridge.getUsedInlineAssetSrcs()
    onEvent(NotemarkDetailEvent.OnSave(documentJson = json, usedInlineAssetSrcs = used))
}

/**
 * [LocalSoftwareKeyboardController] does not dismiss the IME while the WebView editor holds focus.
 * We unFocus first, then hide keyboard and give Compose a short frame budget before opening a sheet.
 * Waiting for IME insets to reach zero can race on Compose Multiplatform bottom sheets and produce
 * open-then-close glitches.
 */
@Composable
private fun rememberPrepareEditorForCreationSheet(): suspend (WebViewEditorBridge?) -> Unit {
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(keyboardController) {
        suspend { editorBridge ->
            editorBridge?.unFocus()
            keyboardController?.hide()
            delay(150)
        }
    }
}
