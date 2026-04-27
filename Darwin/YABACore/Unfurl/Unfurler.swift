//
//  Unfurler.swift
//  YABACore
//
//  Fetches remote HTML for linkmarks; strips scripts, then Readability + rehype/remark in JSC for readable markdown + inline images.
//

import Foundation

/// Native link unfurl: HTML → script strip → Readability + Markdown (unified/rehype/remark via JSC) + inline image assets + metadata + preview bytes.
public struct LinkUnfurlResult: Sendable {
    public var metadata: LinkMetadataResult
    public var readable: ReadableUnfurl
    public var previewImageData: Data?
    public var previewIconData: Data?

    public init(
        metadata: LinkMetadataResult,
        readable: ReadableUnfurl,
        previewImageData: Data?,
        previewIconData: Data?
    ) {
        self.metadata = metadata
        self.readable = readable
        self.previewImageData = previewImageData
        self.previewIconData = previewIconData
    }
}

/// Metadata + preview assets only (no readable body processing).
public struct LinkMetadataRefresh: Sendable {
    public var metadata: LinkMetadataResult
    public var previewImageData: Data?
    public var previewIconData: Data?

    public init(metadata: LinkMetadataResult, previewImageData: Data?, previewIconData: Data?) {
        self.metadata = metadata
        self.previewImageData = previewImageData
        self.previewIconData = previewIconData
    }
}

/// Raw HTML fetch result for the converter pipeline.
public struct RawHtmlFetch: Sendable {
    public var normalizedUrl: String
    public var html: String

    public init(normalizedUrl: String, html: String) {
        self.normalizedUrl = normalizedUrl
        self.html = html
    }
}

public enum Unfurler {
    /// Fetch → metadata (raw HTML) → HTML→Markdown → markdown image download/rewrite → preview image/icon bytes.
    public static func unfurl(_ urlString: String) async throws -> LinkUnfurlResult {
        let fetch = try await fetchRawHtml(urlString)
        let meta = try LinkMetadataExtractor.extract(html: fetch.html, pageUrl: fetch.normalizedUrl)
        let baseForAssets = meta.cleanedUrl.nilIfEmpty ?? fetch.normalizedUrl
        
        let htmlForMarkdown: String
        do {
            htmlForMarkdown = try HTMLPurifier.purify(fetch.html, baseUri: fetch.normalizedUrl)
        } catch {
            throw UnfurlError.htmlSanitizationFailed(String(describing: error))
        }
        
        let markdown: String
        do {
            markdown = try HTMLToMarkdownProcessor.convert(html: htmlForMarkdown)
        } catch {
            throw UnfurlError.htmlToMarkdownFailed(String(describing: error))
        }
        
        let readable = await MarkdownReadableAssetProcessor.process(markdown: markdown, baseURL: baseForAssets)
        let previewImageData = await downloadPreviewImageBytes(urlString: meta.image)
        let previewIconData = await downloadPreviewImageBytes(urlString: meta.logo)
        
        return LinkUnfurlResult(
            metadata: meta,
            readable: readable,
            previewImageData: previewImageData,
            previewIconData: previewIconData
        )
    }

    /// Open Graph / link metadata + preview downloads only (no new readable version).
    public static func fetchMetadataAndPreviews(_ urlString: String) async throws -> LinkMetadataRefresh {
        let fetch = try await fetchRawHtml(urlString)
        let meta = try LinkMetadataExtractor.extract(html: fetch.html, pageUrl: fetch.normalizedUrl)
        let previewImageData = await downloadPreviewImageBytes(urlString: meta.image)
        let previewIconData = await downloadPreviewImageBytes(urlString: meta.logo)
        return LinkMetadataRefresh(
            metadata: meta,
            previewImageData: previewImageData,
            previewIconData: previewIconData
        )
    }

    /// Normalizes the URL string and downloads raw HTML (Compose `Unfurler.unfurl` HTTP portion).
    public static func fetchRawHtml(_ urlString: String) async throws -> RawHtmlFetch {
        let normalized = normalizeURL(urlString)
        guard let url = URL(string: normalized) else {
            throw UnfurlError.cannotCreateURL(normalized)
        }
        let html = try await UnfurlHttpClient.getHtmlString(url: url)
        guard !html.isEmpty else {
            throw UnfurlError.unableToFetchHtml
        }
        return RawHtmlFetch(normalizedUrl: normalized, html: html)
    }

    /// Downloads image bytes for bookmark preview (card image / logo) from metadata URLs.
    public static func downloadPreviewImageBytes(urlString: String?) async -> Data? {
        guard let urlString = urlString?.trimmingCharacters(in: .whitespacesAndNewlines), !urlString.isEmpty,
              let url = URL(string: urlString)
        else { return nil }
        return try? await UnfurlHttpClient.getBytes(url: url)
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
