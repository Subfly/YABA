package dev.subfly.yaba.ui.detail.bookmark.doc.layout

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
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import dev.subfly.yaba.core.navigation.creation.AnnotationCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkReaderFloatingToolbar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yabacore.model.utils.DocmarkType
import dev.subfly.yabacore.state.detail.docmark.DocmarkDetailEvent
import dev.subfly.yabacore.state.detail.docmark.DocmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
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
    val showNoDocumentPlaceholder = !state.isLoading && !hasDocumentPath
    val canRenderReader = hasDocumentPath && state.webContentLoadFailed.not()
    val showReader = canRenderReader && state.isLoading.not()
    var isMenuExpanded by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }
    var isToolbarVisible by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var pageCount by remember { mutableIntStateOf(1) }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    LaunchedEffect(hasDocumentPath) {
        if (!hasDocumentPath) {
            hasSelection = false
            currentPage = 1
            pageCount = 1
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (canRenderReader) {
                if (showReader) {
                    LaunchedEffect(state.scrollToAnnotationId) {
                        val annotationId = state.scrollToAnnotationId ?: return@LaunchedEffect
                        val bridge = readerBridge ?: return@LaunchedEffect
                        bridge.scrollToAnnotation(annotationId)
                        onEvent(DocmarkDetailEvent.OnClearScrollToAnnotation)
                    }

                    DocmarkReaderFloatingToolbar(
                        modifier = Modifier.padding(bottom = 8.dp),
                        docmarkType = state.docmarkType,
                        readerPreferences = state.readerPreferences,
                        color = folderAccent,
                        isVisible = isToolbarVisible || hasSelection,
                        hasSelection = hasSelection,
                        canGoPrev = currentPage > 1,
                        canGoNext = currentPage < pageCount,
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
                            val bookmarkId = state.bookmark?.id ?: return@DocmarkReaderFloatingToolbar
                            val readableVersionId =
                                state.selectedReadableVersionId ?: return@DocmarkReaderFloatingToolbar
                            scope.launch {
                                val draft = bridge.getSelectionSnapshot(bookmarkId, readableVersionId)
                                creationNavigator.add(
                                    AnnotationCreationRoute(
                                        bookmarkId = bookmarkId,
                                        selectionDraft = draft,
                                        annotationId = null,
                                    ),
                                )
                                appStateManager.onShowCreationContent()
                            }
                        },
                    )
                }
                key(state.docmarkType) {
                    when (state.docmarkType) {
                        DocmarkType.PDF ->
                            YabaWebView(
                                modifier = Modifier.fillMaxSize(),
                                baseUrl = WebComponentUris.getPdfViewerUri(),
                                feature = YabaWebFeature.PdfViewer(
                                    pdfUrl = state.documentAbsolutePath ?: "",
                                    platform = YabaWebPlatform.Compose,
                                    appearance = appearance,
                                    annotations = state.annotations,
                                ),
                                onHostEvent = { ev ->
                                    when (ev) {
                                        is YabaWebHostEvent.ReaderMetrics -> {
                                            hasSelection = ev.canCreateAnnotation
                                            currentPage = ev.currentPage
                                            pageCount = ev.pageCount.coerceAtLeast(1)
                                        }

                                        is YabaWebHostEvent.InitialContentLoad ->
                                            onEvent(DocmarkDetailEvent.OnWebInitialContentLoad(ev.result))

                                        else -> Unit
                                    }
                                },
                                onScrollDirectionChanged = { direction ->
                                    if (direction == YabaWebScrollDirection.Down) isToolbarVisible = false
                                    if (direction == YabaWebScrollDirection.Up) isToolbarVisible = true
                                },
                                onReaderBridgeReady = { bridge -> readerBridge = bridge },
                                onAnnotationTap = { annotationId ->
                                    val bookmarkId = state.bookmark?.id ?: return@YabaWebView
                                    creationNavigator.add(
                                        AnnotationCreationRoute(
                                            bookmarkId = bookmarkId,
                                            selectionDraft = null,
                                            annotationId = annotationId,
                                        ),
                                    )
                                    appStateManager.onShowCreationContent()
                                },
                            )

                        DocmarkType.EPUB ->
                            YabaWebView(
                                modifier = Modifier.fillMaxSize(),
                                baseUrl = WebComponentUris.getEpubViewerUri(),
                                feature = YabaWebFeature.EpubViewer(
                                    epubUrl = state.documentAbsolutePath ?: "",
                                    readerPreferences = state.readerPreferences,
                                    platform = YabaWebPlatform.Compose,
                                    appearance = appearance,
                                    annotations = state.annotations,
                                ),
                                onHostEvent = { ev ->
                                    when (ev) {
                                        is YabaWebHostEvent.ReaderMetrics -> {
                                            hasSelection = ev.canCreateAnnotation
                                            currentPage = ev.currentPage
                                            pageCount = ev.pageCount.coerceAtLeast(1)
                                        }

                                        is YabaWebHostEvent.InitialContentLoad ->
                                            onEvent(DocmarkDetailEvent.OnWebInitialContentLoad(ev.result))

                                        else -> Unit
                                    }
                                },
                                onScrollDirectionChanged = { direction ->
                                    if (direction == YabaWebScrollDirection.Down) isToolbarVisible = false
                                    if (direction == YabaWebScrollDirection.Up) isToolbarVisible = true
                                },
                                onReaderBridgeReady = { bridge -> readerBridge = bridge },
                                onAnnotationTap = { annotationId ->
                                    val bookmarkId = state.bookmark?.id ?: return@YabaWebView
                                    creationNavigator.add(
                                        AnnotationCreationRoute(
                                            bookmarkId = bookmarkId,
                                            selectionDraft = null,
                                            annotationId = annotationId,
                                        ),
                                    )
                                    appStateManager.onShowCreationContent()
                                },
                            )
                    }
                }
            }
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularWavyProgressIndicator() }
            } else if (showNoDocumentPlaceholder || state.webContentLoadFailed) {
                NoContentView(
                    modifier = Modifier
                        .fillMaxSize()
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
