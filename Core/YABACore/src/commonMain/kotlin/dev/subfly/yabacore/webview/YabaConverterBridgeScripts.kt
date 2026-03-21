package dev.subfly.yabacore.webview

/**
 * JavaScript source for [window.YabaConverterBridge] calls. No WebView types.
 */
object YabaConverterBridgeScripts {

    fun sanitizeAndConvertHtmlToReaderHtmlScript(html: String, baseUrl: String?): String {
        val htmlEscaped = escapeForJsSingleQuotedString(html)
        val baseUrlLiteral =
            baseUrl?.let { "'${escapeForJsSingleQuotedString(it)}'" } ?: "null"
        return """
            (function() {
                try {
                    var result = window.YabaConverterBridge.sanitizeAndConvertHtmlToReaderHtml({
                        html: '$htmlEscaped',
                        baseUrl: $baseUrlLiteral
                    });
                    return JSON.stringify(result);
                } catch (e) {
                    return JSON.stringify({ error: e.message });
                }
            })();
        """.trimIndent()
    }

    fun startPdfExtractionScript(resolvedPdfUrl: String, renderScale: Float): String {
        val pdfUrlEscaped = escapeForJsSingleQuotedString(resolvedPdfUrl)
        return """
            (function() {
                try {
                    return window.YabaConverterBridge.startPdfExtraction({
                        pdfUrl: '$pdfUrlEscaped',
                        renderScale: $renderScale
                    });
                } catch (e) {
                    return "";
                }
            })();
        """.trimIndent()
    }

    fun getPdfExtractionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return """
            (function() {
                try {
                    var state = window.YabaConverterBridge.getPdfExtractionJob('$jobIdEscaped');
                    return JSON.stringify(state);
                } catch (e) {
                    return JSON.stringify({ status: "error", error: e.message });
                }
            })();
        """.trimIndent()
    }

    fun deletePdfExtractionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return "(function(){ try { window.YabaConverterBridge.deletePdfExtractionJob('$jobIdEscaped'); } catch(e){} })();"
    }
}
