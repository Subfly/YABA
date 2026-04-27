//
//  HTMLPurifier.swift
//  YABACore
//
//  Strips `<script>` nodes from fetched HTML before evaluation in JavaScriptCore (XSS reduction).
//

import Foundation
import SwiftSoup

public enum HTMLPurifier {
    /// Removes all `<script>` elements (and their contents). Empty input yields an empty string.
    public static func purify(_ html: String, baseUri: String = "") throws -> String {
        let trimmed = html.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "" }
        let uri = baseUri.trimmingCharacters(in: .whitespacesAndNewlines)
        let doc = try Parser.parse(trimmed, uri.isEmpty ? "" : uri)
        let scripts = try doc.select("script")
        for el in scripts {
            try el.remove()
        }
        let out = try doc.html()
        return out.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
