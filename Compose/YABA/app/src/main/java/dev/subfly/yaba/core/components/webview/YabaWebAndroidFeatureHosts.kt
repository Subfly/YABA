package dev.subfly.yaba.core.components.webview

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.subfly.yaba.core.webview.InlineLinkTapEvent
import dev.subfly.yaba.core.webview.InlineMentionTapEvent
import dev.subfly.yaba.core.webview.MathTapEvent
import dev.subfly.yaba.core.webview.WebLoadState
import dev.subfly.yaba.core.webview.WebShellLoadResult
import dev.subfly.yaba.core.webview.WebViewEditorBridge
import dev.subfly.yaba.core.webview.WebViewCanvasBridge
import dev.subfly.yaba.core.webview.WebViewReaderBridge
import dev.subfly.yaba.core.webview.WebChromeInsets
import dev.subfly.yaba.core.webview.YabaWebFeature
import dev.subfly.yaba.core.webview.YabaWebHostEvent
import dev.subfly.yaba.core.webview.YabaWebScrollDirection
import dev.subfly.yaba.core.webview.effectiveWebViewTopChromeInsetPx
import kotlin.math.abs

@Composable
private fun BindNativeHostBridge(
    webView: WebView,
    loadUrl: String,
    expectedBridgeFeature: String,
    onReset: () -> Unit,
    onBridgeReady: () -> Unit,
    onHostEvent: (YabaWebHostEvent) -> Unit = {},
    onAnnotationTap: (String) -> Unit = {},
    onMathTap: (MathTapEvent) -> Unit = {},
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit = {},
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit = {},
) {
    DisposableEffect(webView, loadUrl, expectedBridgeFeature) {
        onReset()
        val handler = createNativeHostMessageHandler(
            expectedBridgeFeature = expectedBridgeFeature,
            onBridgeReady = onBridgeReady,
            onHostEvent = onHostEvent,
            onAnnotationTap = onAnnotationTap,
            onMathTap = onMathTap,
            onInlineLinkTap = onInlineLinkTap,
            onInlineMentionTap = onInlineMentionTap,
        )
        webView.attachYabaAndroidHostBridge(handler)
        onDispose {
            webView.detachYabaAndroidHostBridge()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun YabaReadableViewerFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.ReadableViewer,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
) {
    val context = LocalContext.current

    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onScrollDirectionChangedState = rememberUpdatedState(onScrollDirectionChanged)
    val onBridgeReadyState = rememberUpdatedState(onReaderBridgeReady)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val gestureStartYRef = remember { floatArrayOf(0f) }
    val lastGestureDirectionRef = remember { intArrayOf(0) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }
    val onAnnotationTapState = rememberUpdatedState(onAnnotationTap)
    val onInlineLinkTapState = rememberUpdatedState(onInlineLinkTap)
    val onInlineMentionTapState = rememberUpdatedState(onInlineMentionTap)

    val density = LocalDensity.current
    val overlayTopAppBarPx = with(density) { 42.dp.roundToPx() }
    val effectiveChromeInsetPx = remember(overlayTopAppBarPx) {
        effectiveWebViewTopChromeInsetPx(overlayTopAppBarPx)
    }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, motionEvent ->
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureStartYRef[0] = motionEvent.y
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - gestureStartYRef[0]
                        val threshold = touchSlop * 2
                        if (abs(deltaY) < threshold) return@setOnTouchListener false
                        val gestureDirection = if (deltaY < 0f) 1 else -1
                        if (gestureDirection == lastGestureDirectionRef[0]) return@setOnTouchListener false
                        if (gestureDirection > 0) {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Down)
                        } else {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Up)
                        }
                        lastGestureDirectionRef[0] = gestureDirection
                        false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    else -> false
                }
            }
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "viewer",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
        onHostEvent = { onHostEventState.value(it) },
        onAnnotationTap = { onAnnotationTapState.value(it) },
        onInlineLinkTap = { onInlineLinkTapState.value(it) },
        onInlineMentionTap = { onInlineMentionTapState.value(it) },
    )

    DisposableEffect(Unit) {
        onDispose {
            activeBridge = null
            onBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            activeBridge = null
            onBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) {
            activeBridge = null
            onBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = RichTextWebViewReaderBridge(webView)
        activeBridge = bridge
        onBridgeReadyState.value(bridge)
    }

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.initialDocumentJson,
        feature.assetsBaseUrl,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorDocumentJson(
            webView,
            context,
            feature.initialDocumentJson,
            feature.assetsBaseUrl
        )
        RichTextWebViewEditorBridge(webView).setEditable(false)
    }

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.readerPreferences,
        feature.platform,
        feature.appearance,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorReaderPreferences(
            webView,
            feature.readerPreferences,
            feature.platform,
            feature.appearance
        )
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, effectiveChromeInsetPx, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorWebChromeInsets(
            webView,
            WebChromeInsets(topChromeInsetPx = effectiveChromeInsetPx)
        )
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        installEditorAnnotationTap(webView)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.annotations, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        RichTextWebViewReaderBridge(webView).setAnnotations(feature.annotations)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (rendererCrashed.not()) {
                webView.webChromeClient =
                    denyPermissionsWebChromeClient(
                        onProgressChanged = { progress ->
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        progress / 100f
                                    )
                                )
                            )
                        },
                    )
                webView.webViewClient =
                    yabaWebViewClient(
                        assetLoader = assetLoader,
                        onPageStarted = {
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        0f
                                    )
                                )
                            )
                        },
                        onPageFinished = {
                            isPageReady = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                        },
                        onRenderProcessGone = {
                            rendererCrashed = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                            true
                        },
                        onUrlClick = onUrlClick,
                    )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                activeBridge = null
                onBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YabaEditorFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.Editor,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onEditorBridgeReady: (WebViewEditorBridge?) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onMathTap: (MathTapEvent) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onEditorBridgeReadyState = rememberUpdatedState(onEditorBridgeReady)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeEditorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    val pendingPermissionRequestRef =
        remember { mutableStateOf<Pair<PermissionRequest, Array<String>>?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val pending = pendingPermissionRequestRef.value
        pendingPermissionRequestRef.value = null
        if (pending != null) {
            val (request, permissions) = pending
            val allGranted = permissions.all { permission -> result[permission] == true }
            if (allGranted) request.grant(request.resources) else request.deny()
        }
    }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }
    val onAnnotationTapState = rememberUpdatedState(onAnnotationTap)
    val onMathTapState = rememberUpdatedState(onMathTap)
    val onInlineLinkTapState = rememberUpdatedState(onInlineLinkTap)
    val onInlineMentionTapState = rememberUpdatedState(onInlineMentionTap)

    val density = LocalDensity.current
    val overlayTopAppBarPx = with(density) { 42.dp.roundToPx() }
    val effectiveChromeInsetPx = remember(overlayTopAppBarPx) {
        effectiveWebViewTopChromeInsetPx(overlayTopAppBarPx)
    }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
            setBackgroundColor(Color.TRANSPARENT)
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "editor",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
        onHostEvent = { onHostEventState.value(it) },
        onAnnotationTap = { onAnnotationTapState.value(it) },
        onMathTap = { onMathTapState.value(it) },
        onInlineLinkTap = { onInlineLinkTapState.value(it) },
        onInlineMentionTap = { onInlineMentionTapState.value(it) },
    )

    DisposableEffect(Unit) {
        onDispose {
            activeEditorBridge = null
            onEditorBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            activeEditorBridge = null
            onEditorBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) {
            activeEditorBridge = null
            onEditorBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = RichTextWebViewEditorBridge(webView)
        activeEditorBridge = bridge
        onEditorBridgeReadyState.value(bridge)
    }

    /** Same as [YabaReadableViewerFeatureHost]: host-driven palette + reader vars (transparent editor shell, correct on-bg). */
    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.readerPreferences,
        feature.platform,
        feature.appearance,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorReaderPreferences(
            webView,
            feature.readerPreferences,
            feature.platform,
            feature.appearance,
        )
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, effectiveChromeInsetPx, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorWebChromeInsets(
            webView,
            WebChromeInsets(topChromeInsetPx = effectiveChromeInsetPx)
        )
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.placeholderText, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorPlaceholder(webView, feature.placeholderText)
    }

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.initialDocumentJson,
        feature.assetsBaseUrl,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEditorDocumentJson(
            webView,
            context,
            feature.initialDocumentJson,
            feature.assetsBaseUrl
        )
        RichTextWebViewEditorBridge(webView).setEditable(true)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        installEditorAnnotationTap(webView)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (!rendererCrashed) {
                webView.webChromeClient =
                    editorWebChromeClient(
                        onProgressChanged = { p ->
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                        },
                        pendingPermissionRequestRef = pendingPermissionRequestRef,
                        onLaunchPermissionRequest = { perms -> permissionLauncher.launch(perms) },
                    )
                webView.webViewClient =
                    yabaWebViewClient(
                        assetLoader = assetLoader,
                        onPageStarted = {
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        0f
                                    )
                                )
                            )
                        },
                        onPageFinished = {
                            isPageReady = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                        },
                        onRenderProcessGone = {
                            rendererCrashed = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                            true
                        },
                        onUrlClick = onUrlClick,
                    )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                activeEditorBridge = null
                onEditorBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YabaCanvasFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.Canvas,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onUrlClick: (String) -> Boolean,
    onCanvasBridgeReady: (WebViewCanvasBridge?) -> Unit,
) {
    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onCanvasBridgeReadyState = rememberUpdatedState(onCanvasBridgeReady)
    val context = LocalContext.current
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeBridge by remember { mutableStateOf<WebViewCanvasBridge?>(null) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader =
        remember(context) { rememberAssetLoader(context, includeLocalStorage = false) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }
    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
            setBackgroundColor(Color.TRANSPARENT)
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "canvas",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
        onHostEvent = { onHostEventState.value(it) },
    )

    DisposableEffect(Unit) {
        onDispose {
            activeBridge = null
            onCanvasBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            activeBridge = null
            onCanvasBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) {
            activeBridge = null
            onCanvasBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = CanvasWebViewBridge(webView)
        activeBridge = bridge
        onCanvasBridgeReadyState.value(bridge)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.initialSceneJson, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        CanvasWebViewBridge(webView).setSceneJson(feature.initialSceneJson)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (!rendererCrashed) {
                webView.webChromeClient =
                    denyPermissionsWebChromeClient(
                        onProgressChanged = { p ->
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                        },
                    )
                webView.webViewClient =
                    yabaWebViewClient(
                        assetLoader = assetLoader,
                        onPageStarted = {
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        0f
                                    )
                                )
                            )
                        },
                        onPageFinished = {
                            isPageReady = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                        },
                        onRenderProcessGone = {
                            rendererCrashed = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                            true
                        },
                        onUrlClick = onUrlClick,
                    )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                activeBridge = null
                onCanvasBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YabaHtmlConverterFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.HtmlConverter,
    onHostEvent: (YabaWebHostEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader =
        remember(context) { rememberAssetLoader(context, includeLocalStorage = false) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "converter",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
    )

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.input?.html,
        feature.input?.baseUrl,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        val request = feature.input ?: return@LaunchedEffect
        runHtmlConversion(webView, request.html, request.baseUrl).fold(
            onSuccess = { result ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Loaded))
                onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
                onHostEventState.value(YabaWebHostEvent.HtmlConverterSuccess(result))
            },
            onFailure = { e ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Error))
                onHostEventState.value(YabaWebHostEvent.HtmlConverterFailure(e))
            },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (!rendererCrashed) {
                webView.webChromeClient = denyPermissionsWebChromeClient(
                    onProgressChanged = { p ->
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                    },
                )
                webView.webViewClient = yabaWebViewClient(
                    assetLoader = assetLoader,
                    onPageStarted = {
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(0f)))
                    },
                    onPageFinished = {
                        isPageReady = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                    },
                    onRenderProcessGone = {
                        rendererCrashed = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                        true
                    },
                    onUrlClick = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YabaPdfExtractorFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.PdfExtractor,
    onHostEvent: (YabaWebHostEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "converter",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
    )

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.input?.pdfUrl,
        feature.input?.renderScale,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        val request = feature.input ?: return@LaunchedEffect
        runPdfExtraction(webView, context, request.pdfUrl, request.renderScale).fold(
            onSuccess = { result ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Loaded))
                onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
                onHostEventState.value(YabaWebHostEvent.PdfConverterSuccess(result))
            },
            onFailure = { e ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Error))
                onHostEventState.value(YabaWebHostEvent.PdfConverterFailure(e))
            },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (!rendererCrashed) {
                webView.webChromeClient = denyPermissionsWebChromeClient(
                    onProgressChanged = { p ->
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                    },
                )
                webView.webViewClient = yabaWebViewClient(
                    assetLoader = assetLoader,
                    onPageStarted = {
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(0f)))
                    },
                    onPageFinished = {
                        isPageReady = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                    },
                    onRenderProcessGone = {
                        rendererCrashed = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                        true
                    },
                    onUrlClick = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun YabaPdfViewerFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.PdfViewer,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onScrollDirectionChangedState = rememberUpdatedState(onScrollDirectionChanged)
    val onReaderBridgeReadyState = rememberUpdatedState(onReaderBridgeReady)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val gestureStartYRef = remember { floatArrayOf(0f) }
    val lastGestureDirectionRef = remember { intArrayOf(0) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }
    val onAnnotationTapState = rememberUpdatedState(onAnnotationTap)
    val onInlineLinkTapState = rememberUpdatedState(onInlineLinkTap)
    val onInlineMentionTapState = rememberUpdatedState(onInlineMentionTap)

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this, allowZoom = true)
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, motionEvent ->
                if (motionEvent.pointerCount > 1) {
                    lastGestureDirectionRef[0] = 0
                    return@setOnTouchListener false
                }
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureStartYRef[0] = motionEvent.y
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - gestureStartYRef[0]
                        val threshold = touchSlop * 2
                        if (abs(deltaY) < threshold) return@setOnTouchListener false
                        val gestureDirection = if (deltaY < 0f) 1 else -1
                        if (gestureDirection == lastGestureDirectionRef[0]) return@setOnTouchListener false
                        if (gestureDirection > 0) {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Down)
                        } else {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Up)
                        }
                        lastGestureDirectionRef[0] = gestureDirection
                        false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    else -> false
                }
            }
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "pdf",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
        onHostEvent = { onHostEventState.value(it) },
        onAnnotationTap = { onAnnotationTapState.value(it) },
        onInlineLinkTap = { onInlineLinkTapState.value(it) },
        onInlineMentionTap = { onInlineMentionTapState.value(it) },
    )

    DisposableEffect(Unit) {
        onDispose {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = PdfWebViewReaderBridge(webView)
        activeBridge = bridge
        onReaderBridgeReadyState.value(bridge)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.pdfUrl, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyPdfUrl(webView, context, feature.pdfUrl)
    }

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.platform,
        feature.appearance,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyPdfTheme(webView, feature.platform, feature.appearance)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        installPdfAnnotationTap(webView)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.annotations, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        PdfWebViewReaderBridge(webView).setAnnotations(feature.annotations)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (rendererCrashed.not()) {
                webView.webChromeClient =
                    denyPermissionsWebChromeClient(
                        onProgressChanged = { p ->
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                        },
                    )
                webView.webViewClient =
                    yabaWebViewClient(
                        assetLoader = assetLoader,
                        onPageStarted = {
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        0f
                                    )
                                )
                            )
                        },
                        onPageFinished = {
                            isPageReady = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                        },
                        onRenderProcessGone = {
                            rendererCrashed = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                            true
                        },
                        onUrlClick = null,
                    )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                activeBridge = null
                onReaderBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YabaEpubExtractorFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.EpubExtractor,
    onHostEvent: (YabaWebHostEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "converter",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
    )

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.input?.epubDataUrl, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        val request = feature.input ?: return@LaunchedEffect
        runEpubExtraction(webView, context, request.epubDataUrl).fold(
            onSuccess = { result ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Loaded))
                onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
                onHostEventState.value(YabaWebHostEvent.EpubConverterSuccess(result))
            },
            onFailure = { e ->
                onHostEventState.value(YabaWebHostEvent.InitialContentLoad(WebShellLoadResult.Error))
                onHostEventState.value(YabaWebHostEvent.EpubConverterFailure(e))
            },
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (!rendererCrashed) {
                webView.webChromeClient = denyPermissionsWebChromeClient(
                    onProgressChanged = { p ->
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                    },
                )
                webView.webViewClient = yabaWebViewClient(
                    assetLoader = assetLoader,
                    onPageStarted = {
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(0f)))
                    },
                    onPageFinished = {
                        isPageReady = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                    },
                    onRenderProcessGone = {
                        rendererCrashed = true
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                        true
                    },
                    onUrlClick = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
internal fun YabaEpubViewerFeatureHost(
    modifier: Modifier,
    baseUrl: String,
    feature: YabaWebFeature.EpubViewer,
    onHostEvent: (YabaWebHostEvent) -> Unit,
    onScrollDirectionChanged: (YabaWebScrollDirection) -> Unit,
    onReaderBridgeReady: (WebViewReaderBridge?) -> Unit,
    onAnnotationTap: (String) -> Unit,
    onInlineLinkTap: (InlineLinkTapEvent) -> Unit,
    onInlineMentionTap: (InlineMentionTapEvent) -> Unit,
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onScrollDirectionChangedState = rememberUpdatedState(onScrollDirectionChanged)
    val onReaderBridgeReadyState = rememberUpdatedState(onReaderBridgeReady)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeBridge by remember { mutableStateOf<WebViewReaderBridge?>(null) }
    val gestureStartYRef = remember { floatArrayOf(0f) }
    val lastGestureDirectionRef = remember { intArrayOf(0) }
    val touchSlop = remember(context) { ViewConfiguration.get(context).scaledTouchSlop }

    val assetLoaderUrl = remember(baseUrl) { toAssetLoaderUrl(baseUrl) }
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = true) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    var bridgeReadyFromWeb by remember(loadUrl) { mutableStateOf(false) }
    val onAnnotationTapState = rememberUpdatedState(onAnnotationTap)
    val onInlineLinkTapState = rememberUpdatedState(onInlineLinkTap)
    val onInlineMentionTapState = rememberUpdatedState(onInlineMentionTap)

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this, allowZoom = true)
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, motionEvent ->
                if (motionEvent.pointerCount > 1) {
                    lastGestureDirectionRef[0] = 0
                    return@setOnTouchListener false
                }
                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        gestureStartYRef[0] = motionEvent.y
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaY = motionEvent.y - gestureStartYRef[0]
                        val threshold = touchSlop * 2
                        if (abs(deltaY) < threshold) return@setOnTouchListener false
                        val gestureDirection = if (deltaY < 0f) 1 else -1
                        if (gestureDirection == lastGestureDirectionRef[0]) return@setOnTouchListener false
                        if (gestureDirection > 0) {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Down)
                        } else {
                            onScrollDirectionChangedState.value(YabaWebScrollDirection.Up)
                        }
                        lastGestureDirectionRef[0] = gestureDirection
                        false
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        lastGestureDirectionRef[0] = 0
                        false
                    }

                    else -> false
                }
            }
        }
    }

    BindNativeHostBridge(
        webView = webView,
        loadUrl = loadUrl,
        expectedBridgeFeature = "epub",
        onReset = { bridgeReadyFromWeb = false },
        onBridgeReady = { bridgeReadyFromWeb = true },
        onHostEvent = { onHostEventState.value(it) },
        onAnnotationTap = { onAnnotationTapState.value(it) },
        onInlineLinkTap = { onInlineLinkTapState.value(it) },
        onInlineMentionTap = { onInlineMentionTapState.value(it) },
    )

    DisposableEffect(Unit) {
        onDispose {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(rendererCrashed) {
        if (rendererCrashed) {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
        }
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = EpubWebViewReaderBridge(webView)
        activeBridge = bridge
        onReaderBridgeReadyState.value(bridge)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.epubUrl, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEpubUrl(webView, context, feature.epubUrl)
    }

    LaunchedEffect(
        isPageReady,
        bridgeReadyFromWeb,
        feature.readerPreferences,
        feature.platform,
        feature.appearance,
        rendererCrashed
    ) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        applyEpubReaderPreferences(
            webView,
            feature.readerPreferences,
            feature.platform,
            feature.appearance
        )
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        installEpubAnnotationTap(webView)
    }

    LaunchedEffect(isPageReady, bridgeReadyFromWeb, feature.annotations, rendererCrashed) {
        if (!isPageReady || rendererCrashed || !bridgeReadyFromWeb) return@LaunchedEffect
        EpubWebViewReaderBridge(webView).setAnnotations(feature.annotations)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                if (webView.parent != null) {
                    (webView.parent as ViewGroup).removeView(webView)
                }
                addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = {
            if (rendererCrashed.not()) {
                webView.webChromeClient =
                    denyPermissionsWebChromeClient(
                        onProgressChanged = { p ->
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                        },
                    )
                webView.webViewClient =
                    yabaWebViewClient(
                        assetLoader = assetLoader,
                        onPageStarted = {
                            onHostEventState.value(
                                YabaWebHostEvent.LoadState(
                                    WebLoadState.Loading(
                                        0f
                                    )
                                )
                            )
                        },
                        onPageFinished = {
                            isPageReady = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.PageFinished))
                        },
                        onRenderProcessGone = {
                            rendererCrashed = true
                            onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.RendererCrashed))
                            true
                        },
                        onUrlClick = null,
                    )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                bridgeReadyFromWeb = false
                activeBridge = null
                onReaderBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}
