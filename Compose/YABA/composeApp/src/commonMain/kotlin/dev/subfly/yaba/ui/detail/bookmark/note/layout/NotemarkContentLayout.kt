package dev.subfly.yaba.ui.detail.bookmark.note.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
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
import dev.subfly.yaba.util.NotemarkTableSheetResult
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.webview.EditorFormattingState
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

    val appState by appStateManager.state.collectAsState()
    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlLauncher()
    val awaitKeyboardClosedBeforeCreationSheet = rememberAwaitKeyboardClosedBeforeCreationSheet()

    var editorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    var hasSelection by remember { mutableStateOf(false) }
    var editorFormatting by remember(state.bookmark?.id) { mutableStateOf(EditorFormattingState()) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var previousShowCreationContent by remember { mutableStateOf(false) }

    LaunchedEffect(appState.showCreationContent) {
        if (previousShowCreationContent && !appState.showCreationContent) {
            val bridge = editorBridge ?: return@LaunchedEffect
            bridge.focus()
        }
        previousShowCreationContent = appState.showCreationContent
    }

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

    LaunchedEffect(state.scrollToHighlightId) {
        val highlightId = state.scrollToHighlightId ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        bridge.scrollToHighlight(highlightId)
        onEvent(NotemarkDetailEvent.OnClearScrollToHighlight)
    }

    LaunchedEffect(resultStore.getResult(ResultStoreKeys.NOTEMARK_TABLE_INSERT), editorBridge) {
        val r = resultStore.getResult<NotemarkTableSheetResult>(ResultStoreKeys.NOTEMARK_TABLE_INSERT)
            ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        resultStore.removeResult(ResultStoreKeys.NOTEMARK_TABLE_INSERT)
        bridge.dispatch(YabaEditorCommands.insertTablePayload(r.rows, r.cols, r.withHeaderRow))
    }

    LaunchedEffect(state.pendingInsertedImageSrc, editorBridge) {
        val src = state.pendingInsertedImageSrc ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        bridge.dispatch(YabaEditorCommands.insertImagePayload(src))
        onEvent(NotemarkDetailEvent.OnConsumedPendingInsertedImage)
        bridge.focus()
    }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    val webAppearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val ready by remember(state.isLoading, state.initialDocumentJson) {
        derivedStateOf { !state.isLoading && state.initialDocumentJson != null }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (ready) {
                YabaWebView(
                    modifier = Modifier.fillMaxSize(),
                    baseUrl = WebComponentUris.getEditorUri(),
                    feature = YabaWebFeature.Editor(
                        initialDocumentJson = state.initialDocumentJson.orEmpty(),
                        assetsBaseUrl = state.assetsBaseUrl,
                        highlights = state.highlights,
                        platform = YabaWebPlatform.Compose,
                        appearance = webAppearance,
                        readerPreferences = ReaderPreferences(),
                    ),
                    onHostEvent = { ev ->
                        when (ev) {
                            is YabaWebHostEvent.ReaderMetrics -> {
                                hasSelection = ev.canCreateHighlight
                                ev.editorFormatting?.let { editorFormatting = it }
                            }

                            else -> Unit
                        }
                    },
                    onUrlClick = openUrl,
                    onScrollDirectionChanged = { _ -> },
                    onReaderBridgeReady = {},
                    onEditorBridgeReady = { editorBridge = it },
                    onHighlightTap = { highlightId ->
                        val bookmarkId = state.bookmark?.id ?: return@YabaWebView
                        scope.launch {
                            awaitKeyboardClosedBeforeCreationSheet(editorBridge)
                            creationNavigator.add(
                                HighlightCreationRoute(
                                    bookmarkId = bookmarkId,
                                    selectionDraft = null,
                                    highlightId = highlightId,
                                ),
                            )
                            appStateManager.onShowCreationContent()
                        }
                    },
                )
            } else if (!state.isLoading) {
                NoContentView(
                    modifier = Modifier
                        .fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    iconName = "cancel-square",
                    labelRes = Res.string.reader_not_available_title,
                ) { Text(text = stringResource(Res.string.reader_not_available_description)) }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        ) {
            BookmarkDetailContentTopBar(
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
                loadingIndicator = {
                    AnimatedContent(state.isLoading) { loading ->
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .background(color = MaterialTheme.colorScheme.surface),
                            ) { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        } else { Box(modifier = Modifier.fillMaxWidth()) }
                    }
                },
                title = {
                    if (ready) {
                        NotemarkEditorToolbar(
                            color = folderAccent,
                            canCreateHighlight = hasSelection,
                            formatting = editorFormatting,
                            onHighlightClick = {
                                val bridge = editorBridge ?: return@NotemarkEditorToolbar
                                val bookmarkId =
                                    state.bookmark?.id ?: return@NotemarkEditorToolbar
                                val versionId =
                                    state.readableVersionId ?: return@NotemarkEditorToolbar
                                scope.launch {
                                    val draft = bridge.getSelectionSnapshot(bookmarkId, versionId)
                                    awaitKeyboardClosedBeforeCreationSheet(bridge)
                                    creationNavigator.add(
                                        HighlightCreationRoute(
                                            bookmarkId = bookmarkId,
                                            selectionDraft = draft,
                                            highlightId = null,
                                        ),
                                    )
                                    appStateManager.onShowCreationContent()
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
                        )
                    }
                },
            )
        }
    }
}

/**
 * [LocalSoftwareKeyboardController] does not dismiss the IME while the WebView editor holds focus.
 * We unFocus TipTap first, then hide, then wait until IME insets are gone (with a timeout so the sheet
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
