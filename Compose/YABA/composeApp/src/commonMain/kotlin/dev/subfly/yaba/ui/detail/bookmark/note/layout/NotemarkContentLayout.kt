package dev.subfly.yaba.ui.detail.bookmark.note.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.note.components.NotemarkEditorToolbar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailEvent
import dev.subfly.yabacore.state.detail.notemark.NotemarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.YabaWebAppearance
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlLauncher()

    var editorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    var hasSelection by remember { mutableStateOf(false) }
    var editorFormatting by remember(state.bookmark?.id) { mutableStateOf(EditorFormattingState()) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    var frozenInitialDocumentJson by remember(state.bookmark?.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(state.isLoading, state.editorDocumentJson, state.bookmark?.id) {
        if (!state.isLoading && frozenInitialDocumentJson == null) {
            frozenInitialDocumentJson = state.editorDocumentJson
        }
    }

    DisposableEffect(Unit) {
        onDispose { onEvent(NotemarkDetailEvent.OnFlushPendingSave) }
    }

    LaunchedEffect(editorBridge, state.bookmark?.id) {
        val bridge = editorBridge ?: return@LaunchedEffect
        while (true) {
            delay(400)
            val json = bridge.getDocumentJson()
            onEvent(NotemarkDetailEvent.OnEditorDocumentJsonChanged(json))
        }
    }

    LaunchedEffect(state.scrollToHighlightId) {
        val highlightId = state.scrollToHighlightId ?: return@LaunchedEffect
        val bridge = editorBridge ?: return@LaunchedEffect
        bridge.scrollToHighlight(highlightId)
        onEvent(NotemarkDetailEvent.OnClearScrollToHighlight)
    }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    val webAppearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val ready by remember(state.isLoading, frozenInitialDocumentJson) {
        derivedStateOf { !state.isLoading && frozenInitialDocumentJson != null }
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
                        initialDocumentJson = frozenInitialDocumentJson ?: "",
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
                        creationNavigator.add(
                            HighlightCreationRoute(
                                bookmarkId = bookmarkId,
                                selectionDraft = null,
                                highlightId = highlightId,
                            ),
                        )
                        appStateManager.onShowCreationContent()
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
                        )
                    }
                },
            )
        }
    }
}
