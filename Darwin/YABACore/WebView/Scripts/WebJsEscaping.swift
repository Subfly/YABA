//
//  WebJsEscaping.swift
//  YABACore
//
//  Parity with Compose `WebJsEscaping.kt` / `decodeJsStringResult` in `YabaWebAndroidCommon.kt`.
//

import Foundation

public enum WebJsEscaping {
    /// Escapes a string for embedding in a JavaScript **single-quoted** literal.
    public static func escapeForJsSingleQuotedString(_ value: String) -> String {
        value
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "'", with: "\\'")
            .replacingOccurrences(of: "\r", with: "\\r")
            .replacingOccurrences(of: "\n", with: "\\n")
            .replacingOccurrences(of: "\"", with: "\\\"")
    }

    /// Decodes the string returned by `WKWebView.evaluateJavaScript`, which is JSON-encoded.
    public static func decodeJavaScriptStringResult(_ value: String?) -> String {
        guard let value else { return "" }
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "" }
        if trimmed == "null" || trimmed == "undefined" { return "" }
        if let data = trimmed.data(using: .utf8),
           let parsed = try? JSONSerialization.jsonObject(with: data),
           let str = parsed as? String {
            return str
        }
        if trimmed.hasPrefix("\""), trimmed.hasSuffix("\"") {
            let inner = String(trimmed.dropFirst().dropLast())
            return inner
                .replacingOccurrences(of: "\\\"", with: "\"")
                .replacingOccurrences(of: "\\\\", with: "\\")
                .replacingOccurrences(of: "\\n", with: "\n")
                .replacingOccurrences(of: "\\r", with: "\r")
                .replacingOccurrences(of: "\\t", with: "\t")
        }
        return trimmed
    }
}
