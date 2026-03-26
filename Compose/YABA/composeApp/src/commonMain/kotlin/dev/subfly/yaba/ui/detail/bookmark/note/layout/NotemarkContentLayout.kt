package dev.subfly.yaba.ui.detail.bookmark.note.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.ColorSelectionRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkMathSheetRoute
import dev.subfly.yaba.core.navigation.creation.NotemarkTableCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkEditorToolbar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.NotemarkMathSheetResult
import dev.subfly.yaba.util.NotemarkTableSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.model.utils.YabaColor
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.MathTapEvent
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.YabaEditorCommands
import dev.subfly.yabacore.webview.YabaWebAppearance
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebPlatform
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.reader_not_available_description
import yaba.composeapp.generated.resources.reader_not_available_title

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
    val resultStore = LocalResultStore.current

    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlLauncher()
    val awaitKeyboardClosedBeforeCreationSheet = rememberAwaitKeyboardClosedBeforeCreationSheet()
    val notePlaceholderText = "Start your note..." // TODO: localize placeholder text

    var editorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    var editorFormatting by remember(state.bookmark?.id) { mutableStateOf(EditorFormattingState()) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        val bridge = editorBridge ?: return@LifecycleEventEffect
        scope.launch {
            val json = bridge.getDocumentJson()
            onEvent(NotemarkDetailEvent.OnSave(documentJson = json))
        }
    }

    LaunchedEffect(editorBridge, state.bookmark?.id) {
        try {
            awaitCancellation()
        } finally {
            withContext(NonCancellable) {
                val bridge = editorBridge ?: return@withContext
                val json = bridge.getDocumentJson()
                onEvent(NotemarkDetailEvent.OnSave(documentJson = json))
            }
        }
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.NOTEMARK_TABLE_INSERT), editorBridge) {
        val r = resultStore.getResult<NotemarkTableSheetResult>(ResultStoreKeys.NOTEMARK_TABLE_INSERT)
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

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.SELECTED_COLOR), editorBridge) {
        val picked = resultStore.getResult<YabaColor>(ResultStoreKeys.SELECTED_COLOR) ?: return@LaunchedEffect
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

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    val webAppearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val ready by remember(state.isLoading, state.initialDocumentJson, state.webContentLoadFailed) {
        derivedStateOf {
            !state.isLoading &&
                state.initialDocumentJson != null &&
                state.webContentLoadFailed.not()
        }
    }
    val canRenderEditor by remember(state.initialDocumentJson, state.webContentLoadFailed) {
        derivedStateOf {
            state.initialDocumentJson != null &&
                state.webContentLoadFailed.not()
        }
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
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (canRenderEditor) {
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
                        ),
                        onHostEvent = { ev ->
                            when (ev) {
                                is YabaWebHostEvent.ReaderMetrics -> {
                                    ev.editorFormatting?.let { editorFormatting = it }
                                }

                                is YabaWebHostEvent.InitialContentLoad ->
                                    onEvent(NotemarkDetailEvent.OnWebInitialContentLoad(ev.result))

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
                                awaitKeyboardClosedBeforeCreationSheet(editorBridge)
                                creationNavigator.add(
                                    NotemarkMathSheetRoute(
                                        isBlock = ev.isBlock,
                                        initialLatex = ev.latex,
                                        isEdit = true,
                                        editPos = ev.documentPos,
                                    ),
                                )
                                appStateManager.onShowCreationContent()
                            }
                        },
                    )
                }
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularWavyProgressIndicator() }
                } else if (state.webContentLoadFailed || state.initialDocumentJson == null) {
                    NoContentView(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        iconName = "cancel-square",
                        labelRes = Res.string.reader_not_available_title,
                    ) { Text(text = stringResource(Res.string.reader_not_available_description)) }
                }
            }

            if (ready) {
                NotemarkEditorToolbar(
                    modifier = Modifier.fillMaxWidth(),
                    color = folderAccent,
                    formatting = editorFormatting,
                    onHighlightInactiveClick = {
                        scope.launch {
                            awaitKeyboardClosedBeforeCreationSheet(editorBridge)
                            creationNavigator.add(ColorSelectionRoute(selectedColor = YabaColor.NONE))
                            appStateManager.onShowCreationContent()
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
                            awaitKeyboardClosedBeforeCreationSheet(editorBridge)
                            creationNavigator.add(NotemarkTableCreationRoute())
                            appStateManager.onShowCreationContent()
                        }
                    },
                    onOpenMathSheet = { isBlock ->
                        scope.launch {
                            awaitKeyboardClosedBeforeCreationSheet(editorBridge)
                            creationNavigator.add(
                                NotemarkMathSheetRoute(
                                    isBlock = isBlock,
                                    initialLatex = "",
                                    isEdit = false,
                                    editPos = null,
                                ),
                            )
                            appStateManager.onShowCreationContent()
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
                            val json = bridge.getDocumentJson()
                            onEvent(NotemarkDetailEvent.OnSave(documentJson = json))
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
                    )
                }
            },
            loadingIndicator = {},
        )
    }
}

/**
 * [LocalSoftwareKeyboardController] does not dismiss the IME while the WebView editor holds focus.
 * We unFocus the WebView editor first, then hide, then wait until IME insets are gone (with a timeout so the sheet
 * still opens if insets never update).
 */
@Composable
private fun rememberAwaitKeyboardClosedBeforeCreationSheet(): suspend (WebViewEditorBridge?) -> Unit {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val ime = WindowInsets.ime
    return remember(density, keyboardController, ime) {
        suspend { editorBridge ->
            editorBridge?.unFocus()
            keyboardController?.hide()
            withTimeoutOrNull(4_000) {
                snapshotFlow { ime.getBottom(density) }.first { it == 0 }
            }
        }
    }
}
