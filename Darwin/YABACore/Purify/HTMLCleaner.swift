//
//  HTMLCleaner.swift
//  YABACore
//
//  HTML sanitization via SwiftSoup, aligned with DOMPurify `SANITIZE_OPTIONS` in the web converter bridge.
//

import Foundation
import SwiftSoup

/// Sanitizes untrusted HTML fragments for safe downstream parsing (e.g. reader pipeline).
public enum HTMLCleaner {
    /// Whitelist aligned with `SANITIZE_OPTIONS` in `converter-bridge.ts`, built on SwiftSoup `relaxed()` plus reader extras.
    public static func readerFragmentWhitelist() throws -> Whitelist {
        try Whitelist.relaxed()
            .addTags("hr", "s", "del", "iframe", "section", "article", "main", "figure", "figcaption", "time")
            .addAttributes(":all", "class", "title")
            .addAttributes("iframe", "src", "allow", "allowfullscreen", "frameborder")
            .addAttributes("time", "datetime")
            .addProtocols("a", "href", "#")
            .addProtocols("iframe", "src", "http", "https")
            .preserveRelativeLinks(true)
    }

    /// Returns a cleaned HTML body fragment. Empty input yields an empty string.
    public static func clean(_ html: String, baseUri: String = "") throws -> String {
        let trimmed = html.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "" }
        let whitelist = try readerFragmentWhitelist()
        let dirty = try Parser.parseBodyFragment(trimmed, baseUri)
        let cleanedDoc = try Cleaner(headWhitelist: nil, bodyWhitelist: whitelist).clean(dirty)
        let out = try cleanedDoc.body()?.html() ?? ""
        return out.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
