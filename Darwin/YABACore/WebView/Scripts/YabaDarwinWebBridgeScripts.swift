//
//  YabaDarwinWebBridgeScripts.swift
//  YABACore
//
//  Parity with Compose `YabaWebBridgeScripts.kt`.
//

import Foundation

public enum YabaDarwinWebBridgeScripts {
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
}
