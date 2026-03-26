package dev.subfly.yabacore.webview

import dev.subfly.yabacore.model.utils.ReaderFontSize
import dev.subfly.yabacore.model.utils.ReaderLineHeight
import dev.subfly.yabacore.model.utils.ReaderTheme

// --- Enum → JS string literals (web components contract) ---

fun ReaderTheme.toJsReaderThemeLiteral(): String =
    when (this) {
        ReaderTheme.SYSTEM -> "system"
        ReaderTheme.DARK -> "dark"
        ReaderTheme.LIGHT -> "light"
        ReaderTheme.SEPIA -> "sepia"
    }

fun ReaderFontSize.toJsReaderFontSizeLiteral(): String =
    when (this) {
        ReaderFontSize.SMALL -> "small"
        ReaderFontSize.MEDIUM -> "medium"
        ReaderFontSize.LARGE -> "large"
    }

fun ReaderLineHeight.toJsReaderLineHeightLiteral(): String =
    when (this) {
        ReaderLineHeight.NORMAL -> "normal"
        ReaderLineHeight.RELAXED -> "relaxed"
    }

fun YabaWebPlatform.toJsPlatformLiteral(): String =
    when (this) {
        YabaWebPlatform.Compose -> "compose"
        YabaWebPlatform.Darwin -> "darwin"
    }

fun YabaWebAppearance.toJsAppearanceLiteral(): String =
    when (this) {
        YabaWebAppearance.Auto -> "auto"
        YabaWebAppearance.Light -> "light"
        YabaWebAppearance.Dark -> "dark"
    }

/**
 * Rich-text reader/editor — [window.YabaEditorBridge].
 */
object YabaEditorBridgeScripts {

    fun getSelectionSnapshotScript(): String =
        """
        (function() {
            try {
                var snap = window.YabaEditorBridge.getSelectionSnapshot();
                if (!snap) return null;
                return JSON.stringify(snap);
            } catch(e) { return null; }
        })();
        """.trimIndent()

    fun getSelectedTextScript(): String =
        """
        (function() {
            try {
                if (!window.YabaEditorBridge || typeof window.YabaEditorBridge.getSelectedText !== "function") {
                    return "";
                }
                return window.YabaEditorBridge.getSelectedText() || "";
            } catch(e) { return ""; }
        })();
        """.trimIndent()

    fun getCanCreateAnnotationScript(): String =
        """
        (function() {
            try {
                return !!(window.YabaEditorBridge && window.YabaEditorBridge.getCanCreateAnnotation && window.YabaEditorBridge.getCanCreateAnnotation());
            } catch(e) { return false; }
        })();
        """.trimIndent()

    /** JSON object text: active marks and undo/list command availability. */
    fun getActiveFormattingScript(): String =
        """
        (function() {
            try {
                if (window.YabaEditorBridge && typeof window.YabaEditorBridge.getActiveFormatting === "function") {
                    return window.YabaEditorBridge.getActiveFormatting();
                }
                return "";
            } catch(e) { return ""; }
        })();
        """.trimIndent()

    fun scrollToAnnotationScript(annotationId: String): String {
        val escaped = escapeForJsSingleQuotedString(annotationId)
        return """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.scrollToAnnotation) {
                    window.YabaEditorBridge.scrollToAnnotation('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    /**
     * @param documentJson Rich-text document JSON string; escaped for embedding
     * @param setDocumentJsonOptionsJs `undefined` or e.g. `{ assetsBaseUrl: 'https://...' }` (already valid JS)
     */
    fun setDocumentJsonScript(documentJson: String, setDocumentJsonOptionsJs: String): String {
        val jsonEscaped = escapeForJsSingleQuotedString(documentJson)
        return """
        (function() {
            window.YabaEditorBridge.setDocumentJson('$jsonEscaped', $setDocumentJsonOptionsJs);
        })();
        """.trimIndent()
    }

    /**
     * Loads sanitized reader HTML into the read-only viewer shell.
     */
    fun setReaderHtmlScript(readerHtml: String, setReaderHtmlOptionsJs: String): String {
        val htmlEscaped = escapeForJsSingleQuotedString(readerHtml)
        return """
        (function() {
            window.YabaEditorBridge.setReaderHtml('$htmlEscaped', $setReaderHtmlOptionsJs);
        })();
        """.trimIndent()
    }

    fun setReaderHtmlOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl: String?): String =
        setDocumentJsonOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl)

    fun setDocumentJsonOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl: String?): String =
        if (resolvedAssetsBaseUrl != null) {
            "{ assetsBaseUrl: '${escapeForJsSingleQuotedString(resolvedAssetsBaseUrl)}' }"
        } else {
            "undefined"
        }

    fun applyReaderPreferencesScript(
        readerTheme: String,
        readerFontSize: String,
        readerLineHeight: String,
        platform: String,
        appearance: String,
    ): String =
        """
        (function() {
            if (!window.YabaEditorBridge) return;
            if (typeof window.YabaEditorBridge.setPlatform === "function") {
                window.YabaEditorBridge.setPlatform('$platform');
            }
            if (typeof window.YabaEditorBridge.setAppearance === "function") {
                window.YabaEditorBridge.setAppearance('$appearance');
            }
            if (typeof window.YabaEditorBridge.setReaderPreferences === "function") {
                window.YabaEditorBridge.setReaderPreferences({
                    theme: '$readerTheme',
                    fontSize: '$readerFontSize',
                    lineHeight: '$readerLineHeight'
                });
            }
        })();
        """.trimIndent()

    /**
     * [annotationsJsonEscaped] must be safe inside a single-quoted JS string (use [escapeForJsSingleQuotedString] on JSON text).
     */
    fun setAnnotationsJsonParseScript(annotationsJsonEscaped: String): String =
        """
        (function() {
            try {
                var json = JSON.parse('$annotationsJsonEscaped');
                if (window.YabaEditorBridge && window.YabaEditorBridge.setAnnotations) {
                    window.YabaEditorBridge.setAnnotations(JSON.stringify(json));
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun installAnnotationTapScript(): String {
        val prefix =
            escapeForJsSingleQuotedString(
                YabaWebBridgeScripts.ANNOTATION_TAP_SCHEME_PREFIX + "id=",
            )
        return """
        (function() {
            if (window.YabaEditorBridge) {
                window.YabaEditorBridge.onAnnotationTap = function(id) {
                    if (id) window.location = '$prefix' + encodeURIComponent(id);
                };
            }
        })();
        """.trimIndent()
    }

    fun getDocumentJsonScript(): String =
        """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.getDocumentJson) {
                    return window.YabaEditorBridge.getDocumentJson();
                }
                return "";
            } catch(e) { return ""; }
        })();
        """.trimIndent()

    fun applyAnnotationToSelectionScript(annotationId: String): String {
        val escaped = escapeForJsSingleQuotedString(annotationId)
        return """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.applyAnnotationToSelection) {
                    return window.YabaEditorBridge.applyAnnotationToSelection('$escaped') ? 'true' : 'false';
                }
            } catch(e) {}
            return 'false';
        })();
        """.trimIndent()
    }

    fun removeAnnotationFromDocumentScript(annotationId: String): String {
        val escaped = escapeForJsSingleQuotedString(annotationId)
        return """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.removeAnnotationFromDocument) {
                    return String(window.YabaEditorBridge.removeAnnotationFromDocument('$escaped'));
                }
            } catch(e) {}
            return '0';
        })();
        """.trimIndent()
    }

    fun setEditableScript(editable: Boolean): String =
        """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.setEditable) {
                    window.YabaEditorBridge.setEditable($editable);
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun setPlaceholderScript(placeholder: String): String {
        val escaped = escapeForJsSingleQuotedString(placeholder)
        return """
        (function() {
            try {
                if (window.YabaEditorBridge && typeof window.YabaEditorBridge.setPlaceholder === "function") {
                    window.YabaEditorBridge.setPlaceholder('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    fun unFocusScript(): String =
        """
        (function() {
            try {
                if (window.YabaEditorBridge && typeof window.YabaEditorBridge.unFocus === "function") {
                    window.YabaEditorBridge.unFocus();
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun focusScript(): String =
        """
        (function() {
            try {
                if (window.YabaEditorBridge && typeof window.YabaEditorBridge.focus === "function") {
                    window.YabaEditorBridge.focus();
                }
            } catch(e) {}
        })();
        """.trimIndent()

    /**
     * @param payloadJsonEscaped JSON text escaped for embedding in a single-quoted JS string.
     */
    fun dispatchScript(payloadJsonEscaped: String): String =
        """
        (function() {
            try {
                var payload = JSON.parse('$payloadJsonEscaped');
                if (window.YabaEditorBridge && window.YabaEditorBridge.dispatch) {
                    window.YabaEditorBridge.dispatch(payload);
                }
            } catch(e) {}
        })();
        """.trimIndent()
}

/**
 * PDF.js reader — [window.YabaPdfBridge].
 */
object YabaPdfReaderBridgeScripts {

    fun getSelectionSnapshotScript(): String =
        """
        (function() {
            try {
                var snap = window.YabaPdfBridge.getSelectionSnapshot();
                if (!snap) return null;
                return JSON.stringify(snap);
            } catch(e) { return null; }
        })();
        """.trimIndent()

    fun getCanCreateAnnotationScript(): String =
        """
        (function() {
            try {
                return !!(window.YabaPdfBridge && window.YabaPdfBridge.getCanCreateAnnotation && window.YabaPdfBridge.getCanCreateAnnotation());
            } catch(e) { return false; }
        })();
        """.trimIndent()

    fun scrollToAnnotationScript(annotationId: String): String {
        val escaped = escapeForJsSingleQuotedString(annotationId)
        return """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.scrollToAnnotation) {
                    window.YabaPdfBridge.scrollToAnnotation('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    const val GET_PAGE_COUNT_SCRIPT: String =
        "(function(){ try { return window.YabaPdfBridge?.getPageCount?.() ?? 0; } catch(e){ return 0; } })();"

    const val GET_CURRENT_PAGE_NUMBER_SCRIPT: String =
        "(function(){ try { return window.YabaPdfBridge?.getCurrentPageNumber?.() ?? 1; } catch(e){ return 1; } })();"

    const val NEXT_PAGE_SCRIPT: String =
        "(function(){ try { return window.YabaPdfBridge?.nextPage?.() ?? false; } catch(e){ return false; } })();"

    const val PREV_PAGE_SCRIPT: String =
        "(function(){ try { return window.YabaPdfBridge?.prevPage?.() ?? false; } catch(e){ return false; } })();"

    fun setPdfUrlScript(resolvedPdfUrl: String): String {
        val escapedPdfUrl = escapeForJsSingleQuotedString(resolvedPdfUrl)
        return """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.setPdfUrl) {
                    window.YabaPdfBridge.setPdfUrl('$escapedPdfUrl');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    fun applyThemeScript(platform: String, appearance: String): String =
        """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.setPlatform) {
                    window.YabaPdfBridge.setPlatform('$platform');
                }
                if (window.YabaPdfBridge && window.YabaPdfBridge.setAppearance) {
                    window.YabaPdfBridge.setAppearance('$appearance');
                }
            } catch(e) {}
        })();
        """.trimIndent()

    /**
     * [annotationsJsonEscaped] must be safe inside a single-quoted JS string.
     */
    fun setAnnotationsStringArgScript(annotationsJsonEscaped: String): String =
        """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.setAnnotations) {
                    window.YabaPdfBridge.setAnnotations('$annotationsJsonEscaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun installAnnotationTapScript(): String {
        val prefix =
            escapeForJsSingleQuotedString(
                YabaWebBridgeScripts.ANNOTATION_TAP_SCHEME_PREFIX + "id=",
            )
        return """
        (function() {
            if (window.YabaPdfBridge) {
                window.YabaPdfBridge.onAnnotationTap = function(id) {
                    if (id) window.location = '$prefix' + encodeURIComponent(id);
                };
            }
        })();
        """.trimIndent()
    }
}

/**
 * EPUB.js reader — [window.YabaEpubBridge].
 */
object YabaEpubReaderBridgeScripts {

    fun getSelectionSnapshotScript(): String =
        """
        (function() {
            try {
                var snap = window.YabaEpubBridge.getSelectionSnapshot();
                if (!snap) return null;
                return JSON.stringify(snap);
            } catch(e) { return null; }
        })();
        """.trimIndent()

    fun getCanCreateAnnotationScript(): String =
        """
        (function() {
            try {
                return !!(window.YabaEpubBridge && window.YabaEpubBridge.getCanCreateAnnotation && window.YabaEpubBridge.getCanCreateAnnotation());
            } catch(e) { return false; }
        })();
        """.trimIndent()

    fun scrollToAnnotationScript(annotationId: String): String {
        val escaped = escapeForJsSingleQuotedString(annotationId)
        return """
        (function() {
            try {
                if (window.YabaEpubBridge && window.YabaEpubBridge.scrollToAnnotation) {
                    window.YabaEpubBridge.scrollToAnnotation('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    const val GET_PAGE_COUNT_SCRIPT: String =
        "(function(){ try { return window.YabaEpubBridge?.getPageCount?.() ?? 0; } catch(e){ return 0; } })();"

    const val GET_CURRENT_PAGE_NUMBER_SCRIPT: String =
        "(function(){ try { return window.YabaEpubBridge?.getCurrentPageNumber?.() ?? 1; } catch(e){ return 1; } })();"

    const val NEXT_PAGE_SCRIPT: String =
        "(function(){ try { return window.YabaEpubBridge?.nextPage?.() ?? false; } catch(e){ return false; } })();"

    const val PREV_PAGE_SCRIPT: String =
        "(function(){ try { return window.YabaEpubBridge?.prevPage?.() ?? false; } catch(e){ return false; } })();"

    fun setEpubUrlScript(resolvedEpubUrl: String): String {
        val escaped = escapeForJsSingleQuotedString(resolvedEpubUrl)
        return """
        (function() {
            try {
                if (window.YabaEpubBridge && window.YabaEpubBridge.setEpubUrl) {
                    window.YabaEpubBridge.setEpubUrl('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    fun applyReaderPreferencesScript(
        readerTheme: String,
        readerFontSize: String,
        readerLineHeight: String,
        platform: String,
        appearance: String,
    ): String =
        """
        (function() {
            try {
                if (!window.YabaEpubBridge) return;
                if (typeof window.YabaEpubBridge.setPlatform === "function") {
                    window.YabaEpubBridge.setPlatform('$platform');
                }
                if (typeof window.YabaEpubBridge.setAppearance === "function") {
                    window.YabaEpubBridge.setAppearance('$appearance');
                }
                if (typeof window.YabaEpubBridge.setReaderPreferences === "function") {
                    window.YabaEpubBridge.setReaderPreferences({
                        theme: '$readerTheme',
                        fontSize: '$readerFontSize',
                        lineHeight: '$readerLineHeight'
                    });
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun setAnnotationsStringArgScript(annotationsJsonEscaped: String): String =
        """
        (function() {
            try {
                if (window.YabaEpubBridge && window.YabaEpubBridge.setAnnotations) {
                    window.YabaEpubBridge.setAnnotations('$annotationsJsonEscaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun installAnnotationTapScript(): String {
        val prefix =
            escapeForJsSingleQuotedString(
                YabaWebBridgeScripts.ANNOTATION_TAP_SCHEME_PREFIX + "id=",
            )
        return """
        (function() {
            if (window.YabaEpubBridge) {
                window.YabaEpubBridge.onAnnotationTap = function(id) {
                    if (id) window.location = '$prefix' + encodeURIComponent(id);
                };
            }
        })();
        """.trimIndent()
    }
}
