package dev.subfly.yaba.core.components.webview

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
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
import androidx.compose.ui.viewinterop.AndroidView
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.WebLoadState
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.MathTapEvent
import dev.subfly.yabacore.webview.YabaWebBridgeScripts
import dev.subfly.yabacore.webview.YabaWebFeature
import dev.subfly.yabacore.webview.YabaWebHostEvent
import dev.subfly.yabacore.webview.YabaWebScrollDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

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

    LaunchedEffect(isPageReady) {
        if (!isPageReady) {
            activeBridge = null
            onBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        val ready = waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)
        if (!ready) {
            Log.w(YABA_WEBVIEW_LOG_TAG, "Viewer bridge not ready before timeout")
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = RichTextWebViewReaderBridge(webView)
        activeBridge = bridge
        onBridgeReadyState.value(bridge)
    }

    LaunchedEffect(isPageReady, feature.initialDocumentJson, feature.assetsBaseUrl) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        applyEditorDocumentJson(webView, context, feature.initialDocumentJson, feature.assetsBaseUrl)
        RichTextWebViewEditorBridge(webView).setEditable(false)
    }

    LaunchedEffect(isPageReady, feature.readerPreferences, feature.platform, feature.appearance) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        applyEditorReaderPreferences(webView, feature.readerPreferences, feature.platform, feature.appearance)
    }

    LaunchedEffect(isPageReady) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        installEditorAnnotationTap(webView)
    }

    LaunchedEffect(isPageReady, feature.annotations) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return@LaunchedEffect
        RichTextWebViewReaderBridge(webView).setAnnotations(feature.annotations)
    }

    LaunchedEffect(activeBridge) {
        val b = activeBridge ?: return@LaunchedEffect
        var lastCan = false
        var lastPage = 1
        var lastCount = 1
        while (isActive) {
            val can = b.getCanCreateAnnotation()
            val page = b.getCurrentPageNumber()
            val count = b.getPageCount().coerceAtLeast(1)
            if (can != lastCan || page != lastPage || count != lastCount) {
                lastCan = can
                lastPage = page
                lastCount = count
                onHostEventState.value(
                    YabaWebHostEvent.ReaderMetrics(
                        canCreateAnnotation = can,
                        currentPage = page,
                        pageCount = count,
                    ),
                )
            }
            delay(200)
        }
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
                webView.webChromeClient = denyPermissionsWebChromeClient { progress ->
                    onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(progress / 100f)))
                }
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
                    onUrlClick = onUrlClick,
                    onAnnotationTap = onAnnotationTap,
                    onMathTap = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
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
) {
    val context = LocalContext.current
    val onHostEventState = rememberUpdatedState(onHostEvent)
    val onEditorBridgeReadyState = rememberUpdatedState(onEditorBridgeReady)
    var isPageReady by remember { mutableStateOf(false) }
    var rendererCrashed by remember { mutableStateOf(false) }
    var activeEditorBridge by remember { mutableStateOf<WebViewEditorBridge?>(null) }
    val pendingPermissionRequestRef = remember { mutableStateOf<Pair<PermissionRequest, Array<String>>?>(null) }
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

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
            setBackgroundColor(Color.TRANSPARENT)
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }

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

    LaunchedEffect(isPageReady) {
        if (!isPageReady || rendererCrashed) {
            activeEditorBridge = null
            onEditorBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        val ready = waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)
        if (!ready) {
            Log.w(YABA_WEBVIEW_LOG_TAG, "Editor bridge not ready before timeout")
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = RichTextWebViewEditorBridge(webView)
        activeEditorBridge = bridge
        onEditorBridgeReadyState.value(bridge)
    }

    /** Same as [YabaReadableViewerFeatureHost]: host-driven palette + reader vars (transparent editor shell, correct on-bg). */
    LaunchedEffect(isPageReady, feature.readerPreferences, feature.platform, feature.appearance) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) {
            Log.w(YABA_WEBVIEW_LOG_TAG, "Editor bridge not ready before timeout")
            return@LaunchedEffect
        }
        applyEditorReaderPreferences(
            webView,
            feature.readerPreferences,
            feature.platform,
            feature.appearance,
        )
    }

    LaunchedEffect(isPageReady, feature.initialDocumentJson, feature.assetsBaseUrl) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) {
            Log.w(YABA_WEBVIEW_LOG_TAG, "Editor bridge not ready before timeout")
            return@LaunchedEffect
        }
        applyEditorDocumentJson(webView, context, feature.initialDocumentJson, feature.assetsBaseUrl)
        RichTextWebViewEditorBridge(webView).setEditable(true)
    }

    LaunchedEffect(isPageReady) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        installEditorAnnotationTap(webView)
    }

    LaunchedEffect(activeEditorBridge) {
        val b = activeEditorBridge ?: return@LaunchedEffect
        var lastCan = false
        var lastFmt = EditorFormattingState()
        while (isActive) {
            val can = b.getCanCreateAnnotation()
            val fmt = getEditorActiveFormatting(webView)
            if (can != lastCan || fmt != lastFmt) {
                lastCan = can
                lastFmt = fmt
                onHostEventState.value(
                    YabaWebHostEvent.ReaderMetrics(
                        canCreateAnnotation = can,
                        currentPage = 1,
                        pageCount = 1,
                        editorFormatting = fmt,
                    ),
                )
            }
            delay(200)
        }
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
                webView.webChromeClient = editorWebChromeClient(
                    onProgressChanged = { p ->
                        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                    },
                    pendingPermissionRequestRef = pendingPermissionRequestRef,
                    onLaunchPermissionRequest = { perms -> permissionLauncher.launch(perms) },
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
                    onUrlClick = onUrlClick,
                    onAnnotationTap = onAnnotationTap,
                    onMathTap = onMathTap,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
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
    val assetLoader = remember(context) { rememberAssetLoader(context, includeLocalStorage = false) }
    val loadUrl = remember(baseUrl, assetLoaderUrl) { assetLoaderUrl ?: baseUrl }

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
        }
    }

    LaunchedEffect(isPageReady, feature.input?.html, feature.input?.baseUrl) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        val request = feature.input ?: return@LaunchedEffect
        runHtmlConversion(webView, request.html, request.baseUrl).fold(
            onSuccess = { result ->
                onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
                onHostEventState.value(YabaWebHostEvent.HtmlConverterSuccess(result))
            },
            onFailure = { e ->
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
                webView.webChromeClient = denyPermissionsWebChromeClient { p ->
                    onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                }
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
                    onAnnotationTap = null,
                    onMathTap = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
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

    val webView = remember(context) {
        WebView(context).apply {
            applyHardenedWebSettings(this)
        }
    }

    LaunchedEffect(isPageReady, feature.input?.pdfUrl, feature.input?.renderScale) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        val request = feature.input ?: return@LaunchedEffect
        runPdfExtraction(webView, context, request.pdfUrl, request.renderScale).fold(
            onSuccess = { result ->
                onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
                onHostEventState.value(YabaWebHostEvent.PdfConverterSuccess(result))
            },
            onFailure = { e ->
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
                webView.webChromeClient = denyPermissionsWebChromeClient { p ->
                    onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                }
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
                    onAnnotationTap = null,
                    onMathTap = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
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

    LaunchedEffect(isPageReady) {
        if (!isPageReady) {
            activeBridge = null
            onReaderBridgeReadyState.value(null)
            return@LaunchedEffect
        }
        val ready = waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY)
        if (!ready) {
            Log.w(YABA_WEBVIEW_LOG_TAG, "PDF bridge not ready before timeout")
            return@LaunchedEffect
        }
        onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.BridgeReady))
        val bridge = PdfWebViewReaderBridge(webView)
        activeBridge = bridge
        onReaderBridgeReadyState.value(bridge)
    }

    LaunchedEffect(isPageReady, feature.pdfUrl) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        applyPdfUrl(webView, context, feature.pdfUrl)
    }

    LaunchedEffect(isPageReady, feature.platform, feature.appearance) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        applyPdfTheme(webView, feature.platform, feature.appearance)
    }

    LaunchedEffect(isPageReady) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        installPdfAnnotationTap(webView)
    }

    LaunchedEffect(isPageReady, feature.annotations) {
        if (!isPageReady || rendererCrashed) return@LaunchedEffect
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return@LaunchedEffect
        PdfWebViewReaderBridge(webView).setAnnotations(feature.annotations)
    }

    LaunchedEffect(activeBridge) {
        val b = activeBridge ?: return@LaunchedEffect
        var lastCan = false
        var lastPage = 1
        var lastCount = 1
        while (isActive) {
            val can = b.getCanCreateAnnotation()
            val page = b.getCurrentPageNumber()
            val count = b.getPageCount().coerceAtLeast(1)
            if (can != lastCan || page != lastPage || count != lastCount) {
                lastCan = can
                lastPage = page
                lastCount = count
                onHostEventState.value(
                    YabaWebHostEvent.ReaderMetrics(
                        canCreateAnnotation = can,
                        currentPage = page,
                        pageCount = count,
                    ),
                )
            }
            delay(200)
        }
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
                webView.webChromeClient = denyPermissionsWebChromeClient { p ->
                    onHostEventState.value(YabaWebHostEvent.LoadState(WebLoadState.Loading(p / 100f)))
                }
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
                    onAnnotationTap = onAnnotationTap,
                    onMathTap = null,
                )
            }
            if (rendererCrashed) return@AndroidView
            val lastLoadedUrl = webView.tag as? String
            if (lastLoadedUrl != loadUrl) {
                isPageReady = false
                activeBridge = null
                onReaderBridgeReadyState.value(null)
                webView.loadUrl(loadUrl)
                webView.tag = loadUrl
            }
        },
    )
}
