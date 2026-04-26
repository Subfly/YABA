//
//  WebBridgeScripts.swift
//  YABACore
//
//  Parity with Compose `WebBridgeScripts.kt`.
//

import Foundation

public enum WebBridgeScripts {
    public static let editorBridgeReady = """
    (function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady && window.YabaEditorBridge.isReady()); } catch(e){ return false; } })();
    """

    public static let editorBridgeReadyLoose = """
    (function(){ try { return !!(window.YabaEditorBridge && window.YabaEditorBridge.isReady); } catch(e){ return false; } })();
    """

    public static let converterBridgeDefined = """
    (function(){ try { return typeof window.YabaConverterBridge !== 'undefined'; } catch(e){ return false; } })();
    """

    public static let pdfBridgeReady = """
    (function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady && window.YabaPdfBridge.isReady()); } catch(e){ return false; } })();
    """

    public static let pdfBridgeReadyLoose = """
    (function(){ try { return !!(window.YabaPdfBridge && window.YabaPdfBridge.isReady); } catch(e){ return false; } })();
    """

    public static let epubBridgeReady = """
    (function(){ try { return !!(window.YabaEpubBridge && window.YabaEpubBridge.isReady && window.YabaEpubBridge.isReady()); } catch(e){ return false; } })();
    """

    public static let epubBridgeReadyLoose = """
    (function(){ try { return !!(window.YabaEpubBridge && window.YabaEpubBridge.isReady); } catch(e){ return false; } })();
    """

    public static let canvasBridgeReady = """
    (function(){ try { return !!(window.YabaCanvasBridge && window.YabaCanvasBridge.isReady && window.YabaCanvasBridge.isReady()); } catch(e){ return false; } })();
    """

    public static let canvasBridgeReadyLoose = """
    (function(){ try { return !!(window.YabaCanvasBridge && window.YabaCanvasBridge.isReady); } catch(e){ return false; } })();
    """

    // MARK: - PDF / EPUB extraction

    public static func startPdfExtractionScript(resolvedPdfUrl: String, renderScale: Float) -> String {
        let pdfUrlEscaped = WebJsEscaping.escapeForJsSingleQuotedString(resolvedPdfUrl)
        return """
        (function() {
            try {
                return window.YabaConverterBridge.startPdfExtraction({
                    pdfUrl: '\(pdfUrlEscaped)',
                    renderScale: \(renderScale)
                });
            } catch (e) {
                return "";
            }
        })();
        """
    }

    public static func deletePdfExtractionJobScript(jobId: String) -> String {
        let jobIdEscaped = WebJsEscaping.escapeForJsSingleQuotedString(jobId)
        return "(function(){ try { window.YabaConverterBridge.deletePdfExtractionJob('\(jobIdEscaped)'); } catch(e){} })();"
    }

    public static func startEpubExtractionScript(resolvedEpubUrl: String) -> String {
        let epubUrlEscaped = WebJsEscaping.escapeForJsSingleQuotedString(resolvedEpubUrl)
        return """
        (function() {
            try {
                return window.YabaConverterBridge.startEpubExtraction({
                    epubUrl: '\(epubUrlEscaped)'
                });
            } catch (e) {
                return "";
            }
        })();
        """
    }

    public static func deleteEpubExtractionJobScript(jobId: String) -> String {
        let jobIdEscaped = WebJsEscaping.escapeForJsSingleQuotedString(jobId)
        return "(function(){ try { window.YabaConverterBridge.deleteEpubExtractionJob('\(jobIdEscaped)'); } catch(e){} })();"
    }
}
