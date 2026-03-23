package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yabacore.model.highlight.HighlightQuoteSnapshot
import dev.subfly.yabacore.model.highlight.HighlightSourceContext
import dev.subfly.yabacore.model.highlight.HighlightType
import dev.subfly.yabacore.model.highlight.PdfHighlightExtras
import dev.subfly.yabacore.model.highlight.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.HighlightUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaEditorBridgeScripts
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
internal fun RichTextWebViewReaderBridge(
    webView: WebView,
): WebViewReaderBridge = object : WebViewReaderBridge {
    override suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft? {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return null
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.getSelectionSnapshotScript())
        val jsonStr = decodeJsStringResult(raw)
        if (jsonStr == "null" || jsonStr.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonStr)
            val selectedText = json.optString("selectedText", "")
            val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
            val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }
            if (selectedText.isBlank()) return@runCatching null
            ReadableSelectionDraft(
                sourceContext = HighlightSourceContext.readable(bookmarkId, readableVersionId),
                quote = HighlightQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
                pdfAnchor = null,
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateHighlight(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaEditorBridgeScripts.getCanCreateHighlightScript()).trim() == "true"
    }

    override suspend fun setHighlights(highlights: List<HighlightUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        pushHighlightsEditor(webView, highlights)
    }

    override suspend fun scrollToHighlight(highlightId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaEditorBridgeScripts.scrollToHighlightScript(highlightId))
    }

    override suspend fun getDocumentJson(): String {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return ""
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.getDocumentJsonScript())
        return decodeJsStringResult(raw)
    }

    override suspend fun applyHighlightToSelection(highlightId: String): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return false
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.applyHighlightToSelectionScript(highlightId))
        return decodeJsStringResult(raw).trim() == "true"
    }

    override suspend fun removeHighlightFromDocument(highlightId: String): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return 0
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.removeHighlightFromDocumentScript(highlightId))
        return decodeJsStringResult(raw).trim().toIntOrNull() ?: 0
    }
}

@Suppress("FunctionName")
internal fun RichTextWebViewEditorBridge(
    webView: WebView,
): WebViewEditorBridge {
    val reader = RichTextWebViewReaderBridge(webView)
    return object : WebViewEditorBridge {
        override suspend fun getDocumentJson(): String = reader.getDocumentJson()

        override suspend fun setEditable(editable: Boolean) {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            evaluateJs(webView, YabaEditorBridgeScripts.setEditableScript(editable))
        }

        override suspend fun unFocus() {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            evaluateJs(webView, YabaEditorBridgeScripts.unFocusScript())
        }

        override suspend fun focus() {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            evaluateJs(webView, YabaEditorBridgeScripts.focusScript())
        }

        override suspend fun dispatch(payloadJson: String) {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            val escaped = escapeForJsSingleQuotedString(payloadJson)
            evaluateJs(webView, YabaEditorBridgeScripts.dispatchScript(escaped))
        }

        override suspend fun getSelectionSnapshot(
            bookmarkId: String,
            readableVersionId: String,
        ): ReadableSelectionDraft? = reader.getSelectionSnapshot(bookmarkId, readableVersionId)

        override suspend fun getCanCreateHighlight(): Boolean = reader.getCanCreateHighlight()

        override suspend fun setHighlights(highlights: List<HighlightUiModel>) =
            reader.setHighlights(highlights)

        override suspend fun scrollToHighlight(highlightId: String) =
            reader.scrollToHighlight(highlightId)

        override suspend fun applyHighlightToSelection(highlightId: String): Boolean =
            reader.applyHighlightToSelection(highlightId)

        override suspend fun removeHighlightFromDocument(highlightId: String): Int =
            reader.removeHighlightFromDocument(highlightId)
    }
}

internal suspend fun applyReaderHtmlContent(
    webView: WebView,
    context: android.content.Context,
    readerHtml: String,
    assetsBaseUrl: String?,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    val resolvedAssetsBaseUrl = toInternalStorageAssetLoaderBaseUrl(context, assetsBaseUrl) ?: assetsBaseUrl
    val opts = YabaEditorBridgeScripts.setReaderHtmlOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl)
    evaluateJs(
        webView,
        YabaEditorBridgeScripts.setReaderHtmlScript(readerHtml, opts),
    )
}

internal suspend fun applyEditorDocumentJson(
    webView: WebView,
    context: android.content.Context,
    documentJson: String,
    assetsBaseUrl: String?,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    val resolvedAssetsBaseUrl = toInternalStorageAssetLoaderBaseUrl(context, assetsBaseUrl) ?: assetsBaseUrl
    val opts = YabaEditorBridgeScripts.setDocumentJsonOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl)
    evaluateJs(
        webView,
        YabaEditorBridgeScripts.setDocumentJsonScript(documentJson, opts),
    )
}

internal suspend fun applyEditorReaderPreferences(
    webView: WebView,
    readerPreferences: ReaderPreferences,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    evaluateJs(
        webView,
        YabaEditorBridgeScripts.applyReaderPreferencesScript(
            readerTheme = readerPreferences.theme.toJsReaderThemeLiteral(),
            readerFontSize = readerPreferences.fontSize.toJsReaderFontSizeLiteral(),
            readerLineHeight = readerPreferences.lineHeight.toJsReaderLineHeightLiteral(),
            platform = platform.toJsPlatformLiteral(),
            appearance = appearance.toJsAppearanceLiteral(),
        ),
    )
}

internal suspend fun installEditorHighlightTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaEditorBridgeScripts.installHighlightTapScript())
}

internal suspend fun getEditorActiveFormatting(webView: WebView): EditorFormattingState {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return EditorFormattingState()
    val raw = evaluateJs(webView, YabaEditorBridgeScripts.getActiveFormattingScript())
    val jsonStr = decodeJsStringResult(raw)
    if (jsonStr.isBlank()) return EditorFormattingState()
    return runCatching {
        val json = JSONObject(jsonStr)
        EditorFormattingState(
            bold = json.optBoolean("bold"),
            italic = json.optBoolean("italic"),
            underline = json.optBoolean("underline"),
            strikethrough = json.optBoolean("strikethrough"),
            subscript = json.optBoolean("subscript"),
            superscript = json.optBoolean("superscript"),
            code = json.optBoolean("code"),
            codeBlock = json.optBoolean("codeBlock"),
            blockquote = json.optBoolean("blockquote"),
            bulletList = json.optBoolean("bulletList"),
            orderedList = json.optBoolean("orderedList"),
            taskList = json.optBoolean("taskList"),
            inlineMath = json.optBoolean("inlineMath"),
            blockMath = json.optBoolean("blockMath"),
            canUndo = json.optBoolean("canUndo"),
            canRedo = json.optBoolean("canRedo"),
            canIndent = json.optBoolean("canIndent"),
            canOutdent = json.optBoolean("canOutdent"),
            inTable = json.optBoolean("inTable"),
            canAddRowBefore = json.optBoolean("canAddRowBefore"),
            canAddRowAfter = json.optBoolean("canAddRowAfter"),
            canDeleteRow = json.optBoolean("canDeleteRow"),
            canAddColumnBefore = json.optBoolean("canAddColumnBefore"),
            canAddColumnAfter = json.optBoolean("canAddColumnAfter"),
            canDeleteColumn = json.optBoolean("canDeleteColumn"),
        )
    }.getOrElse { EditorFormattingState() }
}

private suspend fun pushHighlightsEditor(webView: WebView, highlights: List<HighlightUiModel>) {
    val arr = JSONArray()
    for (h in highlights) {
        if (h.type != HighlightType.READABLE && h.type != HighlightType.NOTE) continue
        arr.put(
            JSONObject().apply {
                put("id", h.id)
                put("colorRole", h.colorRole.name)
            },
        )
    }
    val jsonStr = arr.toString()
    val escaped = escapeForJsSingleQuotedString(jsonStr)
    evaluateJs(
        webView,
        YabaEditorBridgeScripts.setHighlightsJsonParseScript(escaped),
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
                quote = HighlightQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
                pdfAnchor = PdfHighlightExtras(
                    startSectionKey = startSectionKey,
                    startOffsetInSection = startOffsetInSection,
                    endSectionKey = endSectionKey,
                    endOffsetInSection = endOffsetInSection,
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

    override suspend fun getDocumentJson(): String = ""

    override suspend fun applyHighlightToSelection(highlightId: String): Boolean = false

    override suspend fun removeHighlightFromDocument(highlightId: String): Int = 0
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
        val obj = JSONObject().apply {
            put("id", highlight.id)
            put("colorRole", highlight.colorRole.name)
        }
        if (highlight.type == HighlightType.PDF && !highlight.extrasJson.isNullOrBlank()) {
            runCatching {
                val extras = JSONObject(highlight.extrasJson)
                obj.put("startSectionKey", extras.optString("startSectionKey", ""))
                obj.put("startOffsetInSection", extras.optInt("startOffsetInSection", 0))
                obj.put("endSectionKey", extras.optString("endSectionKey", ""))
                obj.put("endOffsetInSection", extras.optInt("endOffsetInSection", 0))
            }
        }
        arr.put(obj)
    }
    val escaped = escapeForJsSingleQuotedString(arr.toString())
    evaluateJs(
        webView,
        YabaPdfReaderBridgeScripts.setHighlightsStringArgScript(escaped),
    )
}
