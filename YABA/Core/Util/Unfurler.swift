//
//  Unfurler.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import OSLog
import SwiftUI
import WebKit

@MainActor
enum UnfurlError: Error {
    case cannotCreateURL(LocalizedStringKey)
    case unableToUnfurl(LocalizedStringKey)
}

@MainActor
class Unfurler {
    private let logger: Logger = .init()
    
    func unfurl(urlString: String) async throws -> YabaLinkPreview? {
        guard let url = URL(string: urlString) else {
            self.logger.log(level: .error, "[UNFURLER] Cannot create url for: \(urlString)")
            throw UnfurlError.cannotCreateURL(LocalizedStringKey("URL Error Text"))
        }
        
        do {
            if let html = try await loadURL(for: urlString, with: url) {
                let tags = extractAllMetaTags(from: html)
                let metadata = extractMetaData(from: tags)
                let favIconURL = extractFaviconURL(from: html, with: url)
                return YabaLinkPreview(
                    url: urlString,
                    title: metadata[MetaKeys.title],
                    description: metadata[MetaKeys.description],
                    host: metadata[MetaKeys.domain],
                    iconURL: favIconURL,
                    imageURL: metadata[MetaKeys.imageUrl],
                    videoURL: metadata[MetaKeys.videoUrl],
                    iconData: try? await downloadImageData(from: favIconURL),
                    imageData: try? await downloadImageData(from: metadata[MetaKeys.imageUrl])
                )
            }
            return nil
        } catch {
            throw UnfurlError.unableToUnfurl(LocalizedStringKey("Unfurl Error Text"))
        }
    }
    
    private func loadURL(for urlString: String, with url: URL) async throws -> String? {
        let webView = WKWebView()
        
        try await withCheckedThrowingContinuation { continuation in
            let delegate = WebViewDelegate {
                continuation.resume()
            }
            var request = URLRequest(url: url)
            request.addValue(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36",
                forHTTPHeaderField: "User-Agent"
            )
            webView.navigationDelegate = delegate
            webView.load(request)
            
            // Retain delegate during navigation
            objc_setAssociatedObject(webView, "[delegate]", delegate, .OBJC_ASSOCIATION_RETAIN)
        }
        
        return try await webView.evaluateJavaScript("document.documentElement.outerHTML.toString()") as? String
    }
    
    func extractFaviconURL(from html: String, with baseURL: URL?) -> String? {
        let pattern = #"<link\s+[^>]*rel\s*=\s*["'](?:shortcut\s+icon|icon)["'][^>]*>"#

        do {
            let regex = try NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
            let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)

            if let match = regex.firstMatch(in: html, options: [], range: nsrange),
               let tagRange = Range(match.range, in: html) {

                let tag = String(html[tagRange])

                // Find the href inside the <link> tag
                let hrefPattern = #"href\s*=\s*["'](.*?)["']"#
                let hrefRegex = try NSRegularExpression(pattern: hrefPattern, options: [])
                let hrefRange = NSRange(tag.startIndex..<tag.endIndex, in: tag)

                if let hrefMatch = hrefRegex.firstMatch(in: tag, options: [], range: hrefRange),
                   let urlRange = Range(hrefMatch.range(at: 1), in: tag) {
                    let href = String(tag[urlRange])

                    // Convert to absolute URL if baseURL is given
                    if let baseURL = baseURL, let resolvedURL = URL(string: href, relativeTo: baseURL)?.absoluteURL {
                        return resolvedURL.absoluteString
                    } else {
                        return href
                    }
                }
            }
        } catch {
            print("Regex error: \(error)")
        }

        return nil
    }
    
    private func extractAllMetaTags(from html: String) -> [String] {
        let pattern = #"<meta\s+[^>]*?>"#

        do {
            let regex = try NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
            let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)

            let matches = regex.matches(in: html, options: [], range: nsrange)

            return matches.compactMap { match in
                guard let range = Range(match.range, in: html) else { return nil }
                return String(html[range])
            }
        } catch {
            return []
        }
    }
    
    private func extractMetaData(from metaTags: [String]) -> [String: String] {
        var result: [String: String] = [:]

        let prioritizedKeys: [(field: String, candidates: [String])] = [
            (MetaKeys.title,      ["twitter:title", "og:title", "title"]),
            (MetaKeys.description,["twitter:description", "og:description", "description"]),
            (MetaKeys.type,       ["twitter:type", "og:type"]),
            (MetaKeys.domain,     ["twitter:domain", "og:site_name"]),
            (MetaKeys.videoUrl,   ["twitter:player", "og:video"]),
            (MetaKeys.imageUrl,   ["twitter:image", "og:image"]),
        ]

        for (field, keys) in prioritizedKeys {
            for key in keys {
                if let content = contentForMetaKey(key, in: metaTags) {
                    result[field] = content
                    break // Found highest-priority match for this field
                }
            }
        }

        return result
    }

    private func contentForMetaKey(_ targetKey: String, in metaTags: [String]) -> String? {
        for tag in metaTags {
            let lowercasedTag = tag.lowercased()

            if lowercasedTag.contains("property=\"\(targetKey)\"") ||
               lowercasedTag.contains("name=\"\(targetKey)\"") {

                // Match content="..."
                if let contentMatch = tag.range(of: #"content\s*=\s*["'](.*?)["']"#, options: .regularExpression) {
                    let content = String(tag[contentMatch])
                    return content.replacingOccurrences(of: #"content\s*=\s*['"]"#, with: "", options: .regularExpression)
                                  .trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
                }
            }
        }
        return nil
    }
    
    func downloadImageData(from urlString: String?) async throws -> Data? {
        guard
            let urlString = urlString,
            let url = URL(string: urlString) else {
            return nil
        }

        let (data, _) = try await URLSession.shared.data(from: url)
        return data
    }

    private enum MetaKeys {
        static let title      = "title"
        static let description = "description"
        static let type       = "type"
        static let domain     = "domain"
        static let videoUrl   = "videoUrl"
        static let imageUrl   = "imageUrl"
    }
}

private class WebViewDelegate: NSObject, WKNavigationDelegate {
    let didFinish: () -> Void

    init(didFinish: @escaping () -> Void) {
        self.didFinish = didFinish
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        didFinish()
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        // Resume with error if navigation fails
        print("[UNFURLER] WebView load failed: \(error)")
    }
}
