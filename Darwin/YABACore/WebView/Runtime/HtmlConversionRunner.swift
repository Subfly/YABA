//
//  HtmlConversionRunner.swift
//  YABACore
//
//  Polls the bundled converter-style shell for `YabaConverterBridge` (PDF/EPUB extraction jobs). The
//  legacy HTML → TipTap document WebView path has been removed in favor of native Turndown + Swift-Markdown.
//

import Foundation

@MainActor
public enum HtmlConversionRunner {
    /// Polls until `window.YabaConverterBridge` exists (hidden converter shell for PDF/EPUB extraction).
    public static func waitForConverterBridge(runtime: WKWebViewRuntime, maxAttempts: Int = 600) async -> Bool {
        for _ in 0 ..< maxAttempts {
            do {
                let raw = try await runtime.evaluateJavaScriptStringResult(WebBridgeScripts.converterBridgeDefined)
                let s = raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
                if s == "true" || s == "1" { return true }
            } catch {
                // keep polling
            }
            try? await Task.sleep(nanoseconds: 100_000_000)
        }
        return false
    }
}
