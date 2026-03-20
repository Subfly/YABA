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
 * Markdown / TipTap reader — [window.YabaEditorBridge].
 */
object YabaMarkdownReaderBridgeScripts {

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

    fun getCanCreateHighlightScript(): String =
        """
        (function() {
            try {
                return !!(window.YabaEditorBridge && window.YabaEditorBridge.getCanCreateHighlight && window.YabaEditorBridge.getCanCreateHighlight());
            } catch(e) { return false; }
        })();
        """.trimIndent()

    fun scrollToHighlightScript(highlightId: String): String {
        val escaped = escapeForJsSingleQuotedString(highlightId)
        return """
        (function() {
            try {
                if (window.YabaEditorBridge && window.YabaEditorBridge.scrollToHighlight) {
                    window.YabaEditorBridge.scrollToHighlight('$escaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    /**
     * @param markdown raw markdown; escaped for embedding
     * @param setMarkdownOptionsJs `undefined` or e.g. `{ assetsBaseUrl: 'https://...' }` (already valid JS)
     */
    fun setMarkdownScript(markdown: String, setMarkdownOptionsJs: String): String {
        val markdownEscaped = escapeForJsSingleQuotedString(markdown)
        return """
        (function() {
            window.YabaEditorBridge.setMarkdown('$markdownEscaped', $setMarkdownOptionsJs);
        })();
        """.trimIndent()
    }

    fun setMarkdownOptionsFromAssetsBaseUrl(resolvedAssetsBaseUrl: String?): String =
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
     * [highlightsJsonEscaped] must be safe inside a single-quoted JS string (use [escapeForJsSingleQuotedString] on JSON text).
     */
    fun setHighlightsJsonParseScript(highlightsJsonEscaped: String): String =
        """
        (function() {
            try {
                var json = JSON.parse('$highlightsJsonEscaped');
                if (window.YabaEditorBridge && window.YabaEditorBridge.setHighlights) {
                    window.YabaEditorBridge.setHighlights(JSON.stringify(json));
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun installHighlightTapScript(): String {
        val prefix =
            escapeForJsSingleQuotedString(
                YabaWebBridgeScripts.HIGHLIGHT_TAP_SCHEME_PREFIX + "id=",
            )
        return """
        (function() {
            if (window.YabaEditorBridge) {
                window.YabaEditorBridge.onHighlightTap = function(id) {
                    if (id) window.location = '$prefix' + encodeURIComponent(id);
                };
            }
        })();
        """.trimIndent()
    }
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

    fun getCanCreateHighlightScript(): String =
        """
        (function() {
            try {
                return !!(window.YabaPdfBridge && window.YabaPdfBridge.getCanCreateHighlight && window.YabaPdfBridge.getCanCreateHighlight());
            } catch(e) { return false; }
        })();
        """.trimIndent()

    fun scrollToHighlightScript(highlightId: String): String {
        val escaped = escapeForJsSingleQuotedString(highlightId)
        return """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.scrollToHighlight) {
                    window.YabaPdfBridge.scrollToHighlight('$escaped');
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
     * [highlightsJsonEscaped] must be safe inside a single-quoted JS string.
     */
    fun setHighlightsStringArgScript(highlightsJsonEscaped: String): String =
        """
        (function() {
            try {
                if (window.YabaPdfBridge && window.YabaPdfBridge.setHighlights) {
                    window.YabaPdfBridge.setHighlights('$highlightsJsonEscaped');
                }
            } catch(e) {}
        })();
        """.trimIndent()

    fun installHighlightTapScript(): String {
        val prefix =
            escapeForJsSingleQuotedString(
                YabaWebBridgeScripts.HIGHLIGHT_TAP_SCHEME_PREFIX + "id=",
            )
        return """
        (function() {
            if (window.YabaPdfBridge) {
                window.YabaPdfBridge.onHighlightTap = function(id) {
                    if (id) window.location = '$prefix' + encodeURIComponent(id);
                };
            }
        })();
        """.trimIndent()
    }
}
