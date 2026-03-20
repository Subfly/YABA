package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yabacore.model.highlight.HighlightQuoteSnapshot
import dev.subfly.yabacore.model.highlight.HighlightSourceContext
import dev.subfly.yabacore.model.highlight.ReadableAnchor
import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaMarkdownReaderBridgeScripts
import dev.subfly.yabacore.webview.YabaPdfReaderBridgeScripts
import dev.subfly.yabacore.webview.YabaWebAppearance
import dev.subfly.yabacore.webview.YabaWebBridgeScripts
import dev.subfly.yabacore.webview.YabaWebPlatform
import dev.subfly.yabacore.webview.escapeForJsSingleQuotedString
import dev.subfly.yabacore.webview.toJsAppearanceLiteral
import dev.subfly.yabacore.webview.toJsPlatformLiteral
import dev.subfly.yabacore.webview.toJsReaderFontSizeLiteral
import dev.subfly.yabacore.webview.toJsReaderLineHeightLiteral
import dev.subfly.yabacore.webview.toJsReaderThemeLiteral
import org.json.JSONArray
import org.json.JSONObject

@Suppress("FunctionName")
internal fun MarkdownWebViewReaderBridge(
    webView: WebView,
): WebViewReaderBridge = object : WebViewReaderBridge {
    override suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft? {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return null
        val raw = evaluateJs(webView, YabaMarkdownReaderBridgeScripts.getSelectionSnapshotScript())
        val jsonStr = decodeJsStringResult(raw)
        if (jsonStr == "null" || jsonStr.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonStr)
            val startSectionKey = json.optString("startSectionKey", "")
            val startOffsetInSection = json.optInt("startOffsetInSection", 0)
            val endSectionKey = json.optString("endSectionKey", "")
            val endOffsetInSection = json.optInt("endOffsetInSection", 0)
            val selectedText = json.optString("selectedText", "")
            val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
            val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }
            if (startSectionKey.isBlank() || endSectionKey.isBlank() || selectedText.isBlank()) return@runCatching null
            ReadableSelectionDraft(
                sourceContext = HighlightSourceContext.readable(bookmarkId, readableVersionId),
                anchor = ReadableAnchor(
                    readableVersionId = readableVersionId,
                    startSectionKey = startSectionKey,
                    startOffsetInSection = startOffsetInSection,
                    endSectionKey = endSectionKey,
                    endOffsetInSection = endOffsetInSection,
                ),
                quote = HighlightQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateHighlight(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaMarkdownReaderBridgeScripts.getCanCreateHighlightScript()).trim() == "true"
    }

    override suspend fun setHighlights(highlights: List<HighlightUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        pushHighlightsEditor(webView, highlights)
    }

    override suspend fun scrollToHighlight(highlightId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaMarkdownReaderBridgeScripts.scrollToHighlightScript(highlightId))
    }
}

internal suspend fun applyMarkdownContent(
    webView: WebView,
    context: android.content.Context,
    markdown: String,
    assetsBaseUrl: String?,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    val resolvedAssetsBaseUrl = toInternalStorageAssetLoaderBaseUrl(context, assetsBaseUrl) ?: assetsBaseUrl
    val opts = YabaMarkdownReaderBridgeScripts.setMarkdownOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl)
    evaluateJs(
        webView,
        YabaMarkdownReaderBridgeScripts.setMarkdownScript(markdown, opts),
    )
}

internal suspend fun applyMarkdownReaderPreferences(
    webView: WebView,
    readerPreferences: ReaderPreferences,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    evaluateJs(
        webView,
        YabaMarkdownReaderBridgeScripts.applyReaderPreferencesScript(
            readerTheme = readerPreferences.theme.toJsReaderThemeLiteral(),
            readerFontSize = readerPreferences.fontSize.toJsReaderFontSizeLiteral(),
            readerLineHeight = readerPreferences.lineHeight.toJsReaderLineHeightLiteral(),
            platform = platform.toJsPlatformLiteral(),
            appearance = appearance.toJsAppearanceLiteral(),
        ),
    )
}

internal suspend fun installMarkdownHighlightTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaMarkdownReaderBridgeScripts.installHighlightTapScript())
}

private suspend fun pushHighlightsEditor(webView: WebView, highlights: List<HighlightUiModel>) {
    val arr = JSONArray()
    for (h in highlights) {
        arr.put(
            JSONObject().apply {
                put("id", h.id)
                put("startSectionKey", h.startSectionKey)
                put("startOffsetInSection", h.startOffsetInSection)
                put("endSectionKey", h.endSectionKey)
                put("endOffsetInSection", h.endOffsetInSection)
                put("colorRole", h.colorRole.name)
            },
        )
    }
    val jsonStr = arr.toString()
    val escaped = escapeForJsSingleQuotedString(jsonStr)
    evaluateJs(
        webView,
        YabaMarkdownReaderBridgeScripts.setHighlightsJsonParseScript(escaped),
    )
}

@Suppress("FunctionName")
internal fun PdfWebViewReaderBridge(
    webView: WebView,
): WebViewReaderBridge = object : WebViewReaderBridge {
    override suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft? {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY)) return null
        val raw = evaluateJs(webView, YabaPdfReaderBridgeScripts.getSelectionSnapshotScript())
        val jsonStr = decodeJsStringResult(raw)
        if (jsonStr == "null" || jsonStr.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonStr)
            val startSectionKey = json.optString("startSectionKey", "")
            val startOffsetInSection = json.optInt("startOffsetInSection", 0)
            val endSectionKey = json.optString("endSectionKey", "")
            val endOffsetInSection = json.optInt("endOffsetInSection", 0)
            val selectedText = json.optString("selectedText", "")
            val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
            val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }
            if (startSectionKey.isBlank() || endSectionKey.isBlank() || selectedText.isBlank()) return@runCatching null
            ReadableSelectionDraft(
                sourceContext = HighlightSourceContext.readable(bookmarkId, readableVersionId),
                anchor = ReadableAnchor(
                    readableVersionId = readableVersionId,
                    startSectionKey = startSectionKey,
                    startOffsetInSection = startOffsetInSection,
                    endSectionKey = endSectionKey,
                    endOffsetInSection = endOffsetInSection,
                ),
                quote = HighlightQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateHighlight(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.getCanCreateHighlightScript()).trim() == "true"
    }

    override suspend fun setHighlights(highlights: List<HighlightUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
        pushHighlightsPdf(webView, highlights)
    }

    override suspend fun scrollToHighlight(highlightId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaPdfReaderBridgeScripts.scrollToHighlightScript(highlightId))
    }

    override suspend fun getPageCount(): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return 0
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.GET_PAGE_COUNT_SCRIPT).trim().toIntOrNull() ?: 0
    }

    override suspend fun getCurrentPageNumber(): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return 1
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.GET_CURRENT_PAGE_NUMBER_SCRIPT).trim().toIntOrNull() ?: 1
    }

    override suspend fun nextPage(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.NEXT_PAGE_SCRIPT).trim() == "true"
    }

    override suspend fun prevPage(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.PREV_PAGE_SCRIPT).trim() == "true"
    }
}

internal suspend fun applyPdfUrl(webView: WebView, context: android.content.Context, pdfUrl: String) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY)) return
    val resolvedPdfUrl = toInternalStorageAssetLoaderFileUrl(context, pdfUrl) ?: pdfUrl
    evaluateJs(webView, YabaPdfReaderBridgeScripts.setPdfUrlScript(resolvedPdfUrl))
}

internal suspend fun applyPdfTheme(webView: WebView, platform: YabaWebPlatform, appearance: YabaWebAppearance) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY)) return
    evaluateJs(
        webView,
        YabaPdfReaderBridgeScripts.applyThemeScript(
            platform.toJsPlatformLiteral(),
            appearance.toJsAppearanceLiteral(),
        ),
    )
}

internal suspend fun installPdfHighlightTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaPdfReaderBridgeScripts.installHighlightTapScript())
}

private suspend fun pushHighlightsPdf(webView: WebView, highlights: List<HighlightUiModel>) {
    val arr = JSONArray()
    highlights.forEach { highlight ->
        arr.put(
            JSONObject().apply {
                put("id", highlight.id)
                put("startSectionKey", highlight.startSectionKey)
                put("startOffsetInSection", highlight.startOffsetInSection)
                put("endSectionKey", highlight.endSectionKey)
                put("endOffsetInSection", highlight.endOffsetInSection)
                put("colorRole", highlight.colorRole.name)
            },
        )
    }
    val escaped = escapeForJsSingleQuotedString(arr.toString())
    evaluateJs(
        webView,
        YabaPdfReaderBridgeScripts.setHighlightsStringArgScript(escaped),
    )
}
