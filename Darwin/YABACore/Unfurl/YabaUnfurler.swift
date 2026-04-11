//
//  YabaUnfurler.swift
//  YABACore
//
//  Fetches remote HTML for linkmarks. Parsing runs in `yaba-web-components` (WebView converter).
//

import Foundation

/// Raw HTML fetch result for the converter pipeline.
public struct YabaRawHtmlFetch: Sendable {
    public var normalizedUrl: String
    public var html: String

    public init(normalizedUrl: String, html: String) {
        self.normalizedUrl = normalizedUrl
        self.html = html
    }
}

public enum YabaUnfurler {
    /// Normalizes the URL string and downloads raw HTML (Compose `Unfurler.unfurl` HTTP portion).
    public static func fetchRawHtml(_ urlString: String) async throws -> YabaRawHtmlFetch {
        let normalized = normalizeURL(urlString)
        guard let url = URL(string: normalized) else {
            throw YabaUnfurlError.cannotCreateURL(normalized)
        }
        let html = try await YabaUnfurlHttpClient.getHtmlString(url: url)
        guard !html.isEmpty else {
            throw YabaUnfurlError.unableToFetchHtml
        }
        return YabaRawHtmlFetch(normalizedUrl: normalized, html: html)
    }

    /// Downloads image bytes for bookmark preview (card image / logo) from converter metadata URLs.
    public static func downloadPreviewImageBytes(urlString: String?) async -> Data? {
        guard let urlString = urlString?.trimmingCharacters(in: .whitespacesAndNewlines), !urlString.isEmpty,
              let url = URL(string: urlString)
        else { return nil }
        return try? await YabaUnfurlHttpClient.getBytes(url: url)
    }

    private static func normalizeURL(_ urlString: String) -> String {
        var normalized = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        if (normalized.hasPrefix("\"") && normalized.hasSuffix("\"")) ||
            (normalized.hasPrefix("'") && normalized.hasSuffix("'")) {
            normalized = String(normalized.dropFirst().dropLast())
        }
        let lower = normalized.lowercased()
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") {
            // keep
        } else if lower.hasPrefix("www.") {
            normalized = "https://\(normalized)"
        } else if lower.hasPrefix("//") {
            normalized = "https:\(normalized)"
        } else if lower.hasPrefix("ftp://") || lower.hasPrefix("file://") || lower.hasPrefix("mailto:") {
            // keep
        } else if normalized.contains("."), !normalized.contains(" ") {
            normalized = "https://\(normalized)"
        }
        if let protocolRange = normalized.range(of: "://") {
            let protocolPart = String(normalized[..<protocolRange.upperBound])
            let pathPart = String(normalized[protocolRange.upperBound...])
            let cleanedPath = pathPart.replacingOccurrences(of: "//+", with: "/", options: .regularExpression)
            normalized = protocolPart + cleanedPath
        }
        return normalized
    }
}
