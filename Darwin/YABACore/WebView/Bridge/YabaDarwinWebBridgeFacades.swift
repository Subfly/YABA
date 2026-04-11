//
//  YabaDarwinWebBridgeFacades.swift
//  YABACore
//
//  Thin facades over `YabaWKWebViewRuntime.evaluateJavaScriptStringResult` — parity with
//  Compose `WebViewReaderBridge` / editor / canvas entry points (call sites pass scripts from
//  bundled web-component contracts).
//

import Foundation

/// Namespace for reader/editor PDF export and markdown hooks — extend with full script bodies as needed.
public enum YabaDarwinWebReaderBridgeFacades {
    /// After bridge ready, returns document JSON from `window.YabaEditorBridge.getDocumentJson()` when available.
    public static let getDocumentJsonScript = "try { return window.YabaEditorBridge && window.YabaEditorBridge.getDocumentJson ? window.YabaEditorBridge.getDocumentJson() : ''; } catch(e) { return ''; }"
}

/// Namespace for future canvas bridge commands (apply scene, export, etc.).
public enum YabaDarwinWebCanvasBridgeFacades {
    public static let getSceneJsonScript = "try { return window.YabaCanvasBridge && window.YabaCanvasBridge.getSceneJson ? window.YabaCanvasBridge.getSceneJson() : ''; } catch(e) { return ''; }"
}
