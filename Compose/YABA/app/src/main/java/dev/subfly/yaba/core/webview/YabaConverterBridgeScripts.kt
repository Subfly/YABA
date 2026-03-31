package dev.subfly.yaba.core.webview

/** JavaScript source for [window.YabaConverterBridge] calls. No WebView types. */
object YabaConverterBridgeScripts {

    fun sanitizeAndConvertHtmlToReaderHtmlScript(html: String, baseUrl: String?, jobId: String): String {
        val htmlEscaped = escapeForJsSingleQuotedString(html)
        val baseUrlLiteral = baseUrl?.let { "'${escapeForJsSingleQuotedString(it)}'" } ?: "null"
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return """
            (function() {
                try {
                    return window.YabaConverterBridge.startHtmlConversion({
                        html: '$htmlEscaped',
                        baseUrl: $baseUrlLiteral,
                        jobId: '$jobIdEscaped'
                    });
                } catch (e) {
                    return "";
                }
            })();
        """.trimIndent()
    }

    fun getHtmlConversionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return """
            (function() {
                try {
                    var state = window.YabaConverterBridge.getHtmlConversionJob('$jobIdEscaped');
                    return JSON.stringify(state);
                } catch (e) {
                    return JSON.stringify({ status: "error", error: e.message });
                }
            })();
        """.trimIndent()
    }

    fun deleteHtmlConversionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return "(function(){ try { window.YabaConverterBridge.deleteHtmlConversionJob('$jobIdEscaped'); } catch(e){} })();"
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

    fun startEpubExtractionScript(resolvedEpubUrl: String): String {
        val epubUrlEscaped = escapeForJsSingleQuotedString(resolvedEpubUrl)
        return """
            (function() {
                try {
                    return window.YabaConverterBridge.startEpubExtraction({
                        epubUrl: '$epubUrlEscaped'
                    });
                } catch (e) {
                    return "";
                }
            })();
        """.trimIndent()
    }

    fun getEpubExtractionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return """
            (function() {
                try {
                    var state = window.YabaConverterBridge.getEpubExtractionJob('$jobIdEscaped');
                    return JSON.stringify(state);
                } catch (e) {
                    return JSON.stringify({ status: "error", error: e.message });
                }
            })();
        """.trimIndent()
    }

    fun deleteEpubExtractionJobScript(jobId: String): String {
        val jobIdEscaped = escapeForJsSingleQuotedString(jobId)
        return "(function(){ try { window.YabaConverterBridge.deleteEpubExtractionJob('$jobIdEscaped'); } catch(e){} })();"
    }
}
