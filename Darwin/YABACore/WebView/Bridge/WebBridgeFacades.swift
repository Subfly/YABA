//
//  YabaWebBridgeFacades.swift
//  YABACore
//
//  Thin facades over `WKWebViewRuntime.evaluateJavaScriptStringResult` — parity with
//  Compose `WebViewReaderBridge` / editor / canvas entry points (call sites pass scripts from
//  bundled web-component contracts).
//

import Foundation

/// Namespace for reader/editor PDF export and markdown hooks — extend with full script bodies as needed.
public enum WebReaderBridgeFacades {
    /// After bridge ready, returns document JSON from `window.YabaEditorBridge.getDocumentJson()` when available.
    /// Must be an IIFE: `WKWebView.evaluateJavaScript` runs a script *body*; bare `return` is a SyntaxError.
    /// Parity: `YabaReaderBridgeScripts.getDocumentJsonScript()` on Android.
    public static let getDocumentJsonScript = """
    (function() {
        try {
            if (window.YabaEditorBridge && window.YabaEditorBridge.getDocumentJson) {
                return window.YabaEditorBridge.getDocumentJson();
            }
            return "";
        } catch(e) { return ""; }
    })();
    """
}

/// Namespace for future canvas bridge commands (apply scene, export, etc.).
public enum WebCanvasBridgeFacades {
    /// Same IIFE requirement as [getDocumentJsonScript].
    public static let getSceneJsonScript = """
    (function() {
        try {
            if (window.YabaCanvasBridge && window.YabaCanvasBridge.getSceneJson) {
                return window.YabaCanvasBridge.getSceneJson();
            }
            return "";
        } catch(e) { return ""; }
    })();
    """
}
