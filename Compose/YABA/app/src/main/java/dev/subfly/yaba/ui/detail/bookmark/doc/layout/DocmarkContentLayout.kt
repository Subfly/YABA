package dev.subfly.yaba.ui.detail.bookmark.doc.layout

import androidx.compose.ui.res.stringResource

import dev.subfly.yaba.R

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.subfly.yaba.core.components.YabaIcon
import dev.subfly.yaba.core.components.webview.YabaWebView
import dev.subfly.yaba.core.navigation.creation.AnnotationCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkReaderFloatingToolbar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.core.model.utils.DocmarkType
import dev.subfly.yaba.core.state.detail.DetailWebShellPhase
import dev.subfly.yaba.core.state.detail.docmark.DocmarkDetailEvent
import dev.subfly.yaba.core.state.detail.docmark.detailWebShellPhase
import dev.subfly.yaba.core.state.detail.docmark.DocmarkDetailUIState
import dev.subfly.yaba.core.webview.WebComponentUris
import dev.subfly.yaba.core.webview.WebViewReaderBridge
import dev.subfly.yaba.core.webview.YabaWebAppearance
import dev.subfly.yaba.core.webview.YabaWebFeature
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import dev.subfly.yaba.core.webview.YabaWebPlatform
import dev.subfly.yaba.core.webview.YabaWebScrollDirection
import kotlinx.coroutines.launch

private data class ReaderMetricsUiState(
    val hasSelection: Boolean = false,
    val currentPage: Int = 1,
    val pageCount: Int = 1,
)

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
internal fun DocmarkContentLayout(
    modifier: Modifier = Modifier,
    state: DocmarkDetailUIState,
    onShowDetail: () -> Unit,
    onEvent: (DocmarkDetailEvent) -> Unit,
    onShowRemindMePicker: () -> Unit = {},
) {
    val navigator = LocalContentNavigator.current
    val creationNavigator = LocalCreationContentNavigator.current
    val appStateManager = LocalAppStateManager.current
    val scope = rememberCoroutineScope()

    var readerBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val appearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light
    val hasDocumentPath = !state.documentAbsolutePath.isNullOrBlank()
    val webShellPhase = remember(
        state.isLoading,
        state.documentAbsolutePath,
        state.webContentLoadFailed,
    ) { state.detailWebShellPhase() }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var readerMetrics by remember { mutableStateOf(ReaderMetricsUiState()) }
    var isToolbarVisible by remember { mutableStateOf(true) }

    val documentPath = state.documentAbsolutePath ?: ""
    val bookmarkId = state.bookmark?.id

    val folderAccent = remember(state.bookmark) { bookmarkFolderAccentColor(state.bookmark) }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)
    val webBaseUrl = remember(state.docmarkType) {
        when (state.docmarkType) {
            DocmarkType.PDF -> WebComponentUris.getPdfViewerUri()
            DocmarkType.EPUB -> WebComponentUris.getEpubViewerUri()
        }
    }
    val webFeature = remember(
        state.docmarkType,
        documentPath,
        state.readerPreferences,
        appearance,
        state.annotations,
    ) {
        when (state.docmarkType) {
            DocmarkType.PDF ->
                YabaWebFeature.PdfViewer(
                    pdfUrl = documentPath,
                    platform = YabaWebPlatform.Android,
                    appearance = appearance,
                    annotations = state.annotations,
                )

            DocmarkType.EPUB ->
                YabaWebFeature.EpubViewer(
                    epubUrl = documentPath,
                    readerPreferences = state.readerPreferences,
                    platform = YabaWebPlatform.Android,
                    appearance = appearance,
                    annotations = state.annotations,
                )
        }
    }

    LaunchedEffect(hasDocumentPath) {
        if (!hasDocumentPath) {
            readerMetrics = ReaderMetricsUiState()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        ) {
                            Text(text = stringResource(R.string.reader_not_available_description))
                        }
                    }
                }

                DetailWebShellPhase.Bootstrapping,
                DetailWebShellPhase.Ready -> {
                    if (webShellPhase == DetailWebShellPhase.Ready) {
                        LaunchedEffect(state.scrollToAnnotationId, readerBridge) {
                            val annotationId = state.scrollToAnnotationId ?: return@LaunchedEffect
                            val bridge = readerBridge ?: return@LaunchedEffect
                            bridge.scrollToAnnotation(annotationId)
                            onEvent(DocmarkDetailEvent.OnClearScrollToAnnotation)
                        }

                        LaunchedEffect(state.pendingTocNavigate, readerBridge) {
                            val pending = state.pendingTocNavigate ?: return@LaunchedEffect
                            val bridge = readerBridge ?: return@LaunchedEffect
                            bridge.navigateToTocItem(pending.first, pending.second)
                            onEvent(DocmarkDetailEvent.OnClearTocNavigation)
                        }

                        DocmarkReaderFloatingToolbar(
                            modifier = Modifier.padding(bottom = 8.dp),
                            docmarkType = state.docmarkType,
                            readerPreferences = state.readerPreferences,
                            color = folderAccent,
                            isVisible = isToolbarVisible || readerMetrics.hasSelection,
                            hasSelection = readerMetrics.hasSelection,
                            canGoPrev = readerMetrics.currentPage > 1,
                            canGoNext = readerMetrics.currentPage < readerMetrics.pageCount,
                            onEvent = onEvent,
                            onPrevPage = {
                                scope.launch {
                                    readerBridge?.prevPage()
                                }
                            },
                            onNextPage = {
                                scope.launch {
                                    readerBridge?.nextPage()
                                }
                            },
                            onAnnotationClick = {
                                val bridge = readerBridge ?: return@DocmarkReaderFloatingToolbar
                                val resolvedBookmarkId =
                                    bookmarkId ?: return@DocmarkReaderFloatingToolbar
                                scope.launch {
                                    val draft = bridge.getSelectionSnapshot(resolvedBookmarkId)
                                    creationNavigator.add(
                                        AnnotationCreationRoute(
                                            bookmarkId = resolvedBookmarkId,
                                            selectionDraft = draft,
                                            annotationId = null,
                                        ),
                                    )
                                    appStateManager.onShowCreationContent()
                                }
                            },
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        YabaWebView(
                            modifier = Modifier.fillMaxSize(),
                            baseUrl = webBaseUrl,
                            feature = webFeature,
                            onHostEvent = { ev ->
                                when (ev) {
                                    is YabaWebHostEvent.ReaderMetrics -> {
                                        val nextMetrics =
                                            ReaderMetricsUiState(
                                                hasSelection = ev.canCreateAnnotation,
                                                currentPage = ev.currentPage,
                                                pageCount = ev.pageCount.coerceAtLeast(1),
                                            )
                                        if (readerMetrics != nextMetrics) {
                                            readerMetrics = nextMetrics
                                        }
                                    }

                                    is YabaWebHostEvent.InitialContentLoad ->
                                        onEvent(DocmarkDetailEvent.OnWebInitialContentLoad(ev.result))

                                    is YabaWebHostEvent.TableOfContentsChanged ->
                                        onEvent(DocmarkDetailEvent.OnTocChanged(ev.toc))

                                    else -> Unit
                                }
                            },
                            onScrollDirectionChanged = { direction ->
                                isToolbarVisible = when (direction) {
                                    YabaWebScrollDirection.Down -> false
                                    YabaWebScrollDirection.Up -> true
                                }
                            },
                            onReaderBridgeReady = { bridge -> readerBridge = bridge },
                            onAnnotationTap = { annotationId ->
                                val resolvedBookmarkId = bookmarkId ?: return@YabaWebView
                                creationNavigator.add(
                                    AnnotationCreationRoute(
                                        bookmarkId = resolvedBookmarkId,
                                        selectionDraft = null,
                                        annotationId = annotationId,
                                    ),
                                )
                                appStateManager.onShowCreationContent()
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

                        DocmarkContentDropdownMenu(
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
}
