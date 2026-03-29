package dev.subfly.yaba.core.components.webview

import android.webkit.WebView
import dev.subfly.yabacore.model.annotation.AnnotationQuoteSnapshot
import dev.subfly.yabacore.model.annotation.AnnotationSourceContext
import dev.subfly.yabacore.model.annotation.AnnotationType
import dev.subfly.yabacore.model.annotation.EpubAnnotationExtras
import dev.subfly.yabacore.model.annotation.PdfAnnotationExtras
import dev.subfly.yabacore.model.annotation.ReadableSelectionDraft
import dev.subfly.yabacore.model.ui.AnnotationUiModel
import dev.subfly.yabacore.model.utils.ReaderPreferences
import dev.subfly.yabacore.webview.EditorFormattingState
import dev.subfly.yabacore.webview.WebViewEditorBridge
import dev.subfly.yabacore.webview.WebViewReaderBridge
import dev.subfly.yabacore.webview.YabaEditorBridgeScripts
import dev.subfly.yabacore.webview.YabaEpubReaderBridgeScripts
import dev.subfly.yabacore.webview.YabaPdfReaderBridgeScripts
import dev.subfly.yabacore.webview.YabaWebAppearance
import dev.subfly.yabacore.webview.WebShellLoadResult
import dev.subfly.yabacore.webview.YabaWebBridgeScripts
import dev.subfly.yabacore.webview.Toc
import dev.subfly.yabacore.webview.TocItem
import dev.subfly.yabacore.webview.YabaWebPlatform
import dev.subfly.yabacore.webview.escapeForJsSingleQuotedString
import dev.subfly.yabacore.webview.toJsAppearanceLiteral
import dev.subfly.yabacore.webview.toJsPlatformLiteral
import dev.subfly.yabacore.webview.toJsReaderFontSizeLiteral
import dev.subfly.yabacore.webview.toJsReaderLineHeightLiteral
import dev.subfly.yabacore.webview.toJsReaderThemeLiteral
import org.json.JSONArray
import org.json.JSONObject

internal data class EditorHostStateUpdate(
    val canCreateAnnotation: Boolean,
    val formatting: EditorFormattingState,
)

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
                sourceContext = AnnotationSourceContext.readable(bookmarkId, readableVersionId),
                quote = AnnotationQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
                pdfAnchor = null,
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateAnnotation(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaEditorBridgeScripts.getCanCreateAnnotationScript()).trim() == "true"
    }

    override suspend fun setAnnotations(annotations: List<AnnotationUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        pushAnnotationsEditor(webView, annotations)
    }

    override suspend fun scrollToAnnotation(annotationId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaEditorBridgeScripts.scrollToAnnotationScript(annotationId))
    }

    override suspend fun getDocumentJson(): String {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return ""
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.getDocumentJsonScript())
        return decodeJsStringResult(raw)
    }

    override suspend fun applyAnnotationToSelection(annotationId: String): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return false
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.applyAnnotationToSelectionScript(annotationId))
        return decodeJsStringResult(raw).trim() == "true"
    }

    override suspend fun removeAnnotationFromDocument(annotationId: String): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return 0
        val raw = evaluateJs(webView, YabaEditorBridgeScripts.removeAnnotationFromDocumentScript(annotationId))
        return decodeJsStringResult(raw).trim().toIntOrNull() ?: 0
    }

    override suspend fun navigateToTocItem(id: String, extrasJson: String?) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaEditorBridgeScripts.navigateToTocItemScript(id, extrasJson))
    }
}

@Suppress("FunctionName")
internal fun RichTextWebViewEditorBridge(
    webView: WebView,
): WebViewEditorBridge {
    val reader = RichTextWebViewReaderBridge(webView)
    return object : WebViewEditorBridge {
        override suspend fun getDocumentJson(): String = reader.getDocumentJson()

        override suspend fun getSelectedText(): String {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return ""
            val raw = evaluateJs(webView, YabaEditorBridgeScripts.getSelectedTextScript())
            return decodeJsStringResult(raw)
        }

        override suspend fun setEditable(editable: Boolean) {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            evaluateJs(webView, YabaEditorBridgeScripts.setEditableScript(editable))
        }

        override suspend fun setPlaceholder(placeholder: String) {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
            evaluateJs(webView, YabaEditorBridgeScripts.setPlaceholderScript(placeholder))
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

        override suspend fun getCanCreateAnnotation(): Boolean = reader.getCanCreateAnnotation()

        override suspend fun setAnnotations(annotations: List<AnnotationUiModel>) =
            reader.setAnnotations(annotations)

        override suspend fun scrollToAnnotation(annotationId: String) =
            reader.scrollToAnnotation(annotationId)

        override suspend fun applyAnnotationToSelection(annotationId: String): Boolean =
            reader.applyAnnotationToSelection(annotationId)

        override suspend fun removeAnnotationFromDocument(annotationId: String): Int =
            reader.removeAnnotationFromDocument(annotationId)

        override suspend fun navigateToTocItem(id: String, extrasJson: String?) =
            reader.navigateToTocItem(id, extrasJson)

        override suspend fun exportNoteMarkdown(): String {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return ""
            val raw = evaluateJs(webView, YabaEditorBridgeScripts.exportMarkdownScript())
            return decodeJsStringResult(raw)
        }

        override suspend fun exportNotePdfBase64(): String {
            if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return ""
            val raw = evaluateJs(webView, YabaEditorBridgeScripts.exportPdfBase64Script())
            return decodeJsStringResult(raw)
        }
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

internal suspend fun applyEpubReaderPreferences(
    webView: WebView,
    readerPreferences: ReaderPreferences,
    platform: YabaWebPlatform,
    appearance: YabaWebAppearance,
) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY)) return
    evaluateJs(
        webView,
        YabaEpubReaderBridgeScripts.applyReaderPreferencesScript(
            readerTheme = readerPreferences.theme.toJsReaderThemeLiteral(),
            readerFontSize = readerPreferences.fontSize.toJsReaderFontSizeLiteral(),
            readerLineHeight = readerPreferences.lineHeight.toJsReaderLineHeightLiteral(),
            platform = platform.toJsPlatformLiteral(),
            appearance = appearance.toJsAppearanceLiteral(),
        ),
    )
}

internal suspend fun applyEditorPlaceholder(
    webView: WebView,
    placeholder: String?,
) {
    val value = placeholder ?: return
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return
    evaluateJs(webView, YabaEditorBridgeScripts.setPlaceholderScript(value))
}

internal suspend fun installEditorAnnotationTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaEditorBridgeScripts.installAnnotationTapScript())
}

internal suspend fun getEditorActiveFormatting(webView: WebView): EditorFormattingState {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EDITOR_BRIDGE_READY)) return EditorFormattingState()
    val raw = evaluateJs(webView, YabaEditorBridgeScripts.getActiveFormattingScript())
    val jsonStr = decodeJsStringResult(raw)
    if (jsonStr.isBlank()) return EditorFormattingState()
    return runCatching {
        parseEditorFormattingState(JSONObject(jsonStr))
    }.getOrElse { EditorFormattingState() }
}

internal fun parseNoteAutosaveIdleMessage(message: String?): Boolean {
    val payload =
        message
            ?.takeIf { it.startsWith(YabaWebBridgeScripts.NOTE_AUTOSAVE_IDLE_EVENT_PREFIX) }
            ?.removePrefix(YabaWebBridgeScripts.NOTE_AUTOSAVE_IDLE_EVENT_PREFIX)
            ?.takeIf { it.isNotBlank() }
            ?: return false
    val json = runCatching { JSONObject(payload) }.getOrNull() ?: return false
    return json.optString("type") == "noteAutosaveIdle"
}

internal fun parseShellLoadMessage(message: String?): WebShellLoadResult? {
    val payload =
        message
            ?.takeIf { it.startsWith(YabaWebBridgeScripts.SHELL_LOAD_EVENT_PREFIX) }
            ?.removePrefix(YabaWebBridgeScripts.SHELL_LOAD_EVENT_PREFIX)
            ?.takeIf { it.isNotBlank() }
            ?: return null
    val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
    if (json.optString("type") != "shellLoad") return null
    return when (json.optString("result")) {
        "loaded" -> WebShellLoadResult.Loaded
        "error" -> WebShellLoadResult.Error
        else -> null
    }
}

internal fun parseEditorHostStateMessage(message: String?): EditorHostStateUpdate? {
    val payload = message
        ?.takeIf { it.startsWith(YabaWebBridgeScripts.EDITOR_HOST_EVENT_PREFIX) }
        ?.removePrefix(YabaWebBridgeScripts.EDITOR_HOST_EVENT_PREFIX)
        ?.takeIf { it.isNotBlank() }
        ?: return null
    val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
    if (json.optString("type") != "editorState") return null
    return EditorHostStateUpdate(
        canCreateAnnotation = json.optBoolean("canCreateAnnotation"),
        formatting = parseEditorFormattingState(json.optJSONObject("formatting") ?: JSONObject()),
    )
}

internal sealed interface TocConsoleParseResult {
    data object NotToc : TocConsoleParseResult

    /** [toc] is null when the web shell cleared the outline. */
    data class Matched(
        val toc: Toc?,
    ) : TocConsoleParseResult
}

internal fun parseTocConsoleMessage(message: String?): TocConsoleParseResult {
    val payload =
        message
            ?.takeIf { it.startsWith(YabaWebBridgeScripts.TOC_EVENT_PREFIX) }
            ?.removePrefix(YabaWebBridgeScripts.TOC_EVENT_PREFIX)
            ?.takeIf { it.isNotBlank() }
            ?: return TocConsoleParseResult.NotToc
    val json = runCatching { JSONObject(payload) }.getOrNull() ?: return TocConsoleParseResult.NotToc
    if (json.optString("type") != "toc") return TocConsoleParseResult.NotToc
    if (json.isNull("toc")) return TocConsoleParseResult.Matched(toc = null)
    val tocObj = json.optJSONObject("toc") ?: return TocConsoleParseResult.Matched(Toc())
    val itemsArr = tocObj.optJSONArray("items") ?: return TocConsoleParseResult.Matched(Toc())
    return TocConsoleParseResult.Matched(Toc(parseTocItems(itemsArr)))
}

private fun parseTocItems(arr: JSONArray): List<TocItem> {
    val out = ArrayList<TocItem>(arr.length())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val children = parseTocItems(o.optJSONArray("children") ?: JSONArray())
        val extras = o.optString("extrasJson", "").takeIf { it.isNotBlank() }
        out.add(
            TocItem(
                id = o.optString("id", ""),
                title = o.optString("title", ""),
                level = o.optInt("level", 1),
                children = children,
                extrasJson = extras,
            ),
        )
    }
    return out
}

internal fun parseEditorFormattingState(json: JSONObject): EditorFormattingState =
    EditorFormattingState(
        headingLevel = json.optInt("headingLevel"),
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
        textHighlight = json.optBoolean("textHighlight"),
    )

private suspend fun pushAnnotationsEditor(webView: WebView, annotations: List<AnnotationUiModel>) {
    val arr = JSONArray()
    for (h in annotations) {
        if (h.type != AnnotationType.READABLE) continue
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
        YabaEditorBridgeScripts.setAnnotationsJsonParseScript(escaped),
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
                sourceContext = AnnotationSourceContext.pdf(bookmarkId, readableVersionId),
                quote = AnnotationQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
                pdfAnchor = PdfAnnotationExtras(
                    startSectionKey = startSectionKey,
                    startOffsetInSection = startOffsetInSection,
                    endSectionKey = endSectionKey,
                    endOffsetInSection = endOffsetInSection,
                ),
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateAnnotation(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaPdfReaderBridgeScripts.getCanCreateAnnotationScript()).trim() == "true"
    }

    override suspend fun setAnnotations(annotations: List<AnnotationUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
        pushAnnotationsPdf(webView, annotations)
    }

    override suspend fun scrollToAnnotation(annotationId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaPdfReaderBridgeScripts.scrollToAnnotationScript(annotationId))
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

    override suspend fun applyAnnotationToSelection(annotationId: String): Boolean = false

    override suspend fun removeAnnotationFromDocument(annotationId: String): Int = 0

    override suspend fun navigateToTocItem(id: String, extrasJson: String?) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaPdfReaderBridgeScripts.navigateToTocItemScript(id, extrasJson))
    }
}

@Suppress("FunctionName")
internal fun EpubWebViewReaderBridge(
    webView: WebView,
): WebViewReaderBridge = object : WebViewReaderBridge {
    override suspend fun getSelectionSnapshot(
        bookmarkId: String,
        readableVersionId: String,
    ): ReadableSelectionDraft? {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY)) return null
        val raw = evaluateJs(webView, YabaEpubReaderBridgeScripts.getSelectionSnapshotScript())
        val jsonStr = decodeJsStringResult(raw)
        if (jsonStr == "null" || jsonStr.isBlank()) return null
        return runCatching {
            val json = JSONObject(jsonStr)
            val cfiRange = json.optString("cfiRange", "")
            val selectedText = json.optString("selectedText", "")
            val prefixText = json.optString("prefixText").takeIf { it.isNotBlank() }
            val suffixText = json.optString("suffixText").takeIf { it.isNotBlank() }
            if (cfiRange.isBlank() || selectedText.isBlank()) return@runCatching null
            ReadableSelectionDraft(
                sourceContext = AnnotationSourceContext.epub(bookmarkId, readableVersionId),
                quote = AnnotationQuoteSnapshot(
                    selectedText = selectedText,
                    prefixText = prefixText,
                    suffixText = suffixText,
                ),
                epubAnchor = EpubAnnotationExtras(cfiRange = cfiRange),
            )
        }.getOrNull()
    }

    override suspend fun getCanCreateAnnotation(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaEpubReaderBridgeScripts.getCanCreateAnnotationScript()).trim() == "true"
    }

    override suspend fun setAnnotations(annotations: List<AnnotationUiModel>) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return
        pushAnnotationsEpub(webView, annotations)
    }

    override suspend fun scrollToAnnotation(annotationId: String) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaEpubReaderBridgeScripts.scrollToAnnotationScript(annotationId))
    }

    override suspend fun getPageCount(): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return 0
        return evaluateJs(webView, YabaEpubReaderBridgeScripts.GET_PAGE_COUNT_SCRIPT).trim().toIntOrNull() ?: 0
    }

    override suspend fun getCurrentPageNumber(): Int {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return 1
        return evaluateJs(webView, YabaEpubReaderBridgeScripts.GET_CURRENT_PAGE_NUMBER_SCRIPT).trim().toIntOrNull() ?: 1
    }

    override suspend fun nextPage(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaEpubReaderBridgeScripts.NEXT_PAGE_SCRIPT).trim() == "true"
    }

    override suspend fun prevPage(): Boolean {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return false
        return evaluateJs(webView, YabaEpubReaderBridgeScripts.PREV_PAGE_SCRIPT).trim() == "true"
    }

    override suspend fun getDocumentJson(): String = ""

    override suspend fun applyAnnotationToSelection(annotationId: String): Boolean = false

    override suspend fun removeAnnotationFromDocument(annotationId: String): Int = 0

    override suspend fun navigateToTocItem(id: String, extrasJson: String?) {
        if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return
        evaluateJs(webView, YabaEpubReaderBridgeScripts.navigateToTocItemScript(id, extrasJson))
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

internal suspend fun installPdfAnnotationTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.PDF_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaPdfReaderBridgeScripts.installAnnotationTapScript())
}

internal suspend fun applyEpubUrl(webView: WebView, context: android.content.Context, epubUrl: String) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY)) return
    val resolved = toInternalStorageAssetLoaderFileUrl(context, epubUrl) ?: epubUrl
    evaluateJs(webView, YabaEpubReaderBridgeScripts.setEpubUrlScript(resolved))
}

internal suspend fun installEpubAnnotationTap(webView: WebView) {
    if (!waitForBridgeReady(webView, YabaWebBridgeScripts.EPUB_BRIDGE_READY_LOOSE)) return
    evaluateJs(webView, YabaEpubReaderBridgeScripts.installAnnotationTapScript())
}

private suspend fun pushAnnotationsPdf(webView: WebView, annotations: List<AnnotationUiModel>) {
    val arr = JSONArray()
    annotations.forEach { annotation ->
        val obj = JSONObject().apply {
            put("id", annotation.id)
            put("colorRole", annotation.colorRole.name)
        }
        if (annotation.type == AnnotationType.PDF && !annotation.extrasJson.isNullOrBlank()) {
            runCatching {
                val extras = JSONObject(annotation.extrasJson)
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
        YabaPdfReaderBridgeScripts.setAnnotationsStringArgScript(escaped),
    )
}

private suspend fun pushAnnotationsEpub(webView: WebView, annotations: List<AnnotationUiModel>) {
    val arr = JSONArray()
    annotations.forEach { annotation ->
        val obj = JSONObject().apply {
            put("id", annotation.id)
            put("colorRole", annotation.colorRole.name)
        }
        if (annotation.type == AnnotationType.EPUB && !annotation.extrasJson.isNullOrBlank()) {
            runCatching {
                val extras = JSONObject(annotation.extrasJson)
                obj.put("cfiRange", extras.optString("cfiRange", ""))
            }
        }
        arr.put(obj)
    }
    val escaped = escapeForJsSingleQuotedString(arr.toString())
    evaluateJs(
        webView,
        YabaEpubReaderBridgeScripts.setAnnotationsStringArgScript(escaped),
    )
}
