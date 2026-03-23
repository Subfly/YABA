package dev.subfly.yaba.ui.detail.bookmark.link.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkReaderFloatingToolbar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.LocalResultStore
import dev.subfly.yaba.util.ResultStoreKeys
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.common.IdGenerator
import dev.subfly.yabacore.model.highlight.HighlightReadableCreateRequest
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.webview.WebConverterInput
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaWebAppearance
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebPlatform
import dev.subfly.yabacore.webview.YabaWebScrollDirection
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.reader_not_available_description
import yaba.composeapp.generated.resources.reader_not_available_title

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
internal fun LinkmarkContentLayout(
    modifier: Modifier = Modifier,
    state: LinkmarkDetailUIState,
    onShowDetail: () -> Unit,
    onEvent: (LinkmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val resultStore = LocalResultStore.current

    val appState by appStateManager.state.collectAsState()
    val openUrl = rememberUrlLauncher()
    val scope = rememberCoroutineScope()

    var readerBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val appearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val hasReaderContent by remember(state.isLoading, state.readableDocumentJson) {
        derivedStateOf {
            state.isLoading.not() && state.readableDocumentJson.isNullOrBlank().not()
        }
    }
    var isReaderToolbarVisible by remember(
        state.readableDocumentJson,
        state.isLoading
    ) { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }
    var previousShowCreationContent by remember { mutableStateOf(false) }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    val converterInput by remember(state.converterHtml) {
        derivedStateOf {
            state.converterHtml?.let { html ->
                WebConverterInput(html = html, baseUrl = state.converterBaseUrl)
            }
        }
    }

    LaunchedEffect(appState.showCreationContent) {
        val show = appState.showCreationContent
        if (previousShowCreationContent && show.not()) {
            val createReq = resultStore.getResult<HighlightReadableCreateRequest>(
                ResultStoreKeys.HIGHLIGHT_READABLE_CREATE_REQUEST,
            )
            val deleteId = resultStore.getResult<String>(
                ResultStoreKeys.HIGHLIGHT_READABLE_DELETE_REQUEST,
            )
            val bridge = readerBridge

            when {
                createReq != null && bridge != null -> {
                    resultStore.removeResult(ResultStoreKeys.HIGHLIGHT_READABLE_CREATE_REQUEST)
                    val highlightId = IdGenerator.newId()
                    if (bridge.applyHighlightToSelection(highlightId)) {
                        val json = bridge.getDocumentJson()
                        onEvent(
                            LinkmarkDetailEvent.OnHighlightReadableCreateCommitted(
                                highlightId = highlightId,
                                request = createReq,
                                documentJson = json,
                            ),
                        )
                    }
                }
                deleteId != null && bridge != null -> {
                    resultStore.removeResult(ResultStoreKeys.HIGHLIGHT_READABLE_DELETE_REQUEST)
                    bridge.removeHighlightFromDocument(deleteId)
                    val json = bridge.getDocumentJson()
                    onEvent(
                        LinkmarkDetailEvent.OnHighlightReadableDeleteCommitted(
                            highlightId = deleteId,
                            documentJson = json,
                        ),
                    )
                }
            }
        }
        previousShowCreationContent = show
    }

    YabaWebView(
        modifier = Modifier.size(0.dp),
        baseUrl = WebComponentUris.getConverterUri(),
        feature = YabaWebFeature.HtmlConverter(input = converterInput),
        onHostEvent = { ev ->
            when (ev) {
                is YabaWebHostEvent.HtmlConverterSuccess ->
                    onEvent(
                        LinkmarkDetailEvent.OnConverterSucceeded(
                            documentJson = ev.result.documentJson,
                            assets = ev.result.assets,
                        ),
                    )

                is YabaWebHostEvent.HtmlConverterFailure ->
                    onEvent(LinkmarkDetailEvent.OnConverterFailed(error = ev.error))

                else -> Unit
            }
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasReaderContent) {
                LaunchedEffect(state.scrollToHighlightId) {
                    val highlightId = state.scrollToHighlightId ?: return@LaunchedEffect
                    val bridge = readerBridge ?: return@LaunchedEffect
                    bridge.scrollToHighlight(highlightId)
                    onEvent(LinkmarkDetailEvent.OnClearScrollToHighlight)
                }

                LinkmarkReaderFloatingToolbar(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = folderAccent,
                    isVisible = isReaderToolbarVisible || hasSelection,
                    readerPreferences = state.readerPreferences,
                    hasSelection = hasSelection,
                    onEvent = onEvent,
                    onHighlightClick = {
                        val bridge = readerBridge ?: return@LinkmarkReaderFloatingToolbar
                        val bookmarkId = state.bookmark?.id ?: return@LinkmarkReaderFloatingToolbar
                        val versionId = state.selectedReadableVersionId
                            ?: state.readableVersions.firstOrNull()?.versionId
                            ?: return@LinkmarkReaderFloatingToolbar
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
                )

                YabaWebView(
                    modifier = Modifier.fillMaxSize(),
                    baseUrl = WebComponentUris.getViewerUri(),
                    feature = YabaWebFeature.ReadableViewer(
                        initialDocumentJson = state.readableDocumentJson ?: "",
                        assetsBaseUrl = state.assetsBaseUrl,
                        readerPreferences = state.readerPreferences,
                        platform = YabaWebPlatform.Compose,
                        appearance = appearance,
                        highlights = state.highlights,
                    ),
                    onHostEvent = { ev ->
                        when (ev) {
                            is YabaWebHostEvent.ReaderMetrics ->
                                hasSelection = ev.canCreateHighlight

                            else -> Unit
                        }
                    },
                    onUrlClick = openUrl,
                    onScrollDirectionChanged = { direction ->
                        if (direction == YabaWebScrollDirection.Down) isReaderToolbarVisible = false
                        if (direction == YabaWebScrollDirection.Up) isReaderToolbarVisible = true
                    },
                    onReaderBridgeReady = { bridge -> readerBridge = bridge },
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
            } else if (!state.isLoading && !state.isUpdatingReadable) {
                NoContentView(
                    modifier = Modifier.fillMaxSize()
                        .wrapContentSize(Alignment.Center),
                    iconName = "cancel-square",
                    labelRes = Res.string.reader_not_available_title,
                ) {
                    Text(text = stringResource(Res.string.reader_not_available_description))
                }
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

                        LinkmarkContentDropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            state = state,
                            onEvent = onEvent,
                            onShowRemindMePicker = onShowRemindMePicker,
                        )
                    }
                },
                loadingIndicator = {
                    AnimatedContent(state.isLoading || state.isUpdatingReadable) { loading ->
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .background(color = MaterialTheme.colorScheme.surface)
                            ) { LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
            )
        }
    }
}
