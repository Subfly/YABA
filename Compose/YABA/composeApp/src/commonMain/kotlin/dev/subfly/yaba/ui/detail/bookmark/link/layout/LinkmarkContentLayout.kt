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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.subfly.yaba.core.components.NoContentView
import dev.subfly.yaba.core.components.webview.WebViewReaderBridge
import dev.subfly.yaba.core.components.webview.ConverterInput
import dev.subfly.yaba.core.components.webview.YabaWebAppearance
import dev.subfly.yaba.core.components.webview.YabaWebPlatform
import dev.subfly.yaba.core.components.webview.YabaWebScrollDirection
import dev.subfly.yaba.core.components.webview.YabaWebViewConverter
import dev.subfly.yaba.core.components.webview.YabaWebViewViewer
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkContentDropdownMenu
import dev.subfly.yaba.ui.detail.bookmark.link.components.LinkmarkReaderFloatingToolbar
import dev.subfly.yaba.core.navigation.creation.HighlightCreationRoute
import dev.subfly.yaba.util.LocalAppStateManager
import dev.subfly.yaba.util.LocalContentNavigator
import dev.subfly.yaba.util.LocalCreationContentNavigator
import dev.subfly.yaba.util.rememberUrlLauncher
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailEvent
import dev.subfly.yabacore.state.detail.linkmark.LinkmarkDetailUIState
import dev.subfly.yabacore.ui.icon.YabaIcon
import dev.subfly.yabacore.ui.webview.WebComponentUris
import dev.subfly.yabacore.unfurl.ConverterAssetInput
import org.jetbrains.compose.resources.stringResource
import yaba.composeapp.generated.resources.Res
import yaba.composeapp.generated.resources.bookmark_detail_title
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

    val scope = rememberCoroutineScope()
    val openUrl = rememberUrlLauncher()

    var readerBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val appearance = if (isSystemInDarkTheme()) YabaWebAppearance.Dark else YabaWebAppearance.Light

    val hasReaderContent = !state.isLoading && !state.readableMarkdown.isNullOrBlank()
    var isReaderToolbarVisible by remember(
        state.readableMarkdown,
        state.isLoading
    ) { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var hasSelection by remember { mutableStateOf(false) }

    LaunchedEffect(readerBridge, isReaderToolbarVisible, hasReaderContent) {
        if (!hasReaderContent || !isReaderToolbarVisible) {
            hasSelection = false
            return@LaunchedEffect
        }
        val bridge = readerBridge ?: return@LaunchedEffect
        while (true) {
            hasSelection = bridge.getCanCreateHighlight()
            delay(200)
        }
    }

    val converterInput = state.converterHtml?.let { html ->
        ConverterInput(html = html, baseUrl = state.converterBaseUrl)
    }

    YabaWebViewConverter(
        modifier = Modifier.size(0.dp),
        baseUrl = WebComponentUris.getConverterUri(),
        input = converterInput,
        onConverterResult = { result ->
            onEvent(
                LinkmarkDetailEvent.OnConverterSucceeded(
                    markdown = result.markdown,
                    assets = result.assets.map { a ->
                        ConverterAssetInput(
                            placeholder = a.placeholder,
                            url = a.url,
                            alt = a.alt,
                        )
                    },
                    title = state.bookmark?.label?.takeIf { it.isNotBlank() },
                    author = null,
                ),
            )
        },
        onConverterError = { error ->
            onEvent(LinkmarkDetailEvent.OnConverterFailed(error = error))
        },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text(text = stringResource(Res.string.bookmark_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = navigator::removeLastOrNull) {
                            YabaIcon(name = "arrow-left-01")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onShowDetail,
                            shapes = IconButtonDefaults.shapes(),
                            content = { YabaIcon(name = "information-circle") }
                        )
                        Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                            IconButton(
                                onClick = { isMenuExpanded = !isMenuExpanded },
                                shapes = IconButtonDefaults.shapes(),
                            ) { YabaIcon(name = "more-horizontal-circle-02") }

                            LinkmarkContentDropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                                state = state,
                                onEvent = onEvent,
                                onShowRemindMePicker = onShowRemindMePicker,
                            )
                        }
                    }
                )
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
            }
        }
    ) { paddings ->
        Box(modifier = Modifier.fillMaxSize().padding(paddings)) {
            if (hasReaderContent) {
                LaunchedEffect(state.scrollToHighlightId) {
                    val highlightId = state.scrollToHighlightId ?: return@LaunchedEffect
                    val bridge = readerBridge ?: return@LaunchedEffect
                    bridge.scrollToHighlight(highlightId)
                    onEvent(LinkmarkDetailEvent.OnClearScrollToHighlight)
                }

                LinkmarkReaderFloatingToolbar(
                    isVisible = isReaderToolbarVisible,
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

                YabaWebViewViewer(
                    modifier = Modifier.fillMaxSize(),
                    baseUrl = WebComponentUris.getViewerUri(),
                    markdown = state.readableMarkdown ?: "",
                    assetsBaseUrl = state.assetsBaseUrl,
                    platform = YabaWebPlatform.Compose,
                    appearance = appearance,
                    readerPreferences = state.readerPreferences,
                    onUrlClick = openUrl,
                    onScrollDirectionChanged = { direction ->
                        if (direction == YabaWebScrollDirection.Down) isReaderToolbarVisible = false
                        if (direction == YabaWebScrollDirection.Up) isReaderToolbarVisible = true
                    },
                    onBridgeReady = { bridge -> readerBridge = bridge },
                    onHighlightTap = { highlightId ->
                        val bookmarkId = state.bookmark?.id ?: return@YabaWebViewViewer
                        creationNavigator.add(
                            HighlightCreationRoute(
                                bookmarkId = bookmarkId,
                                selectionDraft = null,
                                highlightId = highlightId,
                            ),
                        )
                        appStateManager.onShowCreationContent()
                    },
                    highlights = state.highlights,
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
    }
}
