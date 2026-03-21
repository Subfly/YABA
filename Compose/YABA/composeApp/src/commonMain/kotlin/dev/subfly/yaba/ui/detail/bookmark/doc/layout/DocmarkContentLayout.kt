package dev.subfly.yaba.ui.detail.bookmark.doc.layout

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
import dev.subfly.yaba.ui.detail.bookmark.components.BookmarkDetailContentTopBar
import dev.subfly.yaba.ui.detail.bookmark.util.bookmarkDetailIconButtonColors
import dev.subfly.yaba.ui.detail.bookmark.components.bookmarkFolderAccentColor
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.doc.components.DocmarkReaderFloatingToolbar
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
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
    val hasReaderContent = !state.isLoading && !state.pdfAbsolutePath.isNullOrBlank()
    var isMenuExpanded by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }
    var isToolbarVisible by remember(hasReaderContent) { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var pageCount by remember { mutableIntStateOf(1) }

    val folderAccent by remember(state.bookmark) {
        derivedStateOf { bookmarkFolderAccentColor(state.bookmark) }
    }
    val menuIconButtonColors = bookmarkDetailIconButtonColors(folderAccent)

    LaunchedEffect(hasReaderContent) {
        if (!hasReaderContent) {
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
            if (hasReaderContent) {
                LaunchedEffect(state.scrollToHighlightId) {
                    val highlightId = state.scrollToHighlightId ?: return@LaunchedEffect
                    val bridge = readerBridge ?: return@LaunchedEffect
                    bridge.scrollToHighlight(highlightId)
                    onEvent(DocmarkDetailEvent.OnClearScrollToHighlight)
                }

                DocmarkReaderFloatingToolbar(
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = folderAccent,
                    isVisible = isToolbarVisible || hasSelection,
                    hasSelection = hasSelection,
                    canGoPrev = currentPage > 1,
                    canGoNext = currentPage < pageCount,
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
                    onHighlightClick = {
                        val bridge = readerBridge ?: return@DocmarkReaderFloatingToolbar
                        val bookmarkId = state.bookmark?.id ?: return@DocmarkReaderFloatingToolbar
                        val readableVersionId =
                            state.selectedReadableVersionId ?: return@DocmarkReaderFloatingToolbar
                        scope.launch {
                            val draft = bridge.getSelectionSnapshot(bookmarkId, readableVersionId)
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
                    baseUrl = WebComponentUris.getPdfViewerUri(),
                    feature = YabaWebFeature.PdfViewer(
                        pdfUrl = state.pdfAbsolutePath ?: "",
                        platform = YabaWebPlatform.Compose,
                        appearance = appearance,
                        highlights = state.highlights,
                    ),
                    onHostEvent = { ev ->
                        when (ev) {
                            is YabaWebHostEvent.ReaderMetrics -> {
                                hasSelection = ev.canCreateHighlight
                                currentPage = ev.currentPage
                                pageCount = ev.pageCount.coerceAtLeast(1)
                            }

                            else -> Unit
                        }
                    },
                    onScrollDirectionChanged = { direction ->
                        if (direction == YabaWebScrollDirection.Down) isToolbarVisible = false
                        if (direction == YabaWebScrollDirection.Up) isToolbarVisible = true
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
            } else if (!state.isLoading) {
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
                loadingIndicator = {
                    AnimatedContent(state.isLoading) { loading ->
                        if (loading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .background(color = MaterialTheme.colorScheme.surface),
                            ) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
            )
        }
    }
}
