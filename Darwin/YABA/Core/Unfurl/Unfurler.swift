//
//  Unfurler.swift
//  YABA
//
//  Created by Ali Taha on 21.04.2025.
//

import Foundation
import OSLog
import SwiftUI

@MainActor
enum UnfurlError: Error {
    case cannotCreateURL(LocalizedStringKey)
    case unableToUnfurl(LocalizedStringKey)
}

private enum MetaKeys {
    static let title = "title"
    static let description = "description"
    static let type = "type"
    static let domain = "domain"
    static let videoUrl = "video"
    static let imageUrl = "image"
}

private extension String {
    func slice(from: String, to: String) -> String? {
        guard let fromRange = range(of: from)?.upperBound,
              let toRange = range(of: to, range: fromRange..<endIndex) else { return nil }
        return String(self[fromRange..<toRange.lowerBound])
    }
}

@MainActor
class Unfurler {
    private let logger: Logger = .init()
    
    func unfurl(urlString: String) async throws -> YabaLinkPreview? {
        let normalizedURL = normalizeURL(urlString)
        let cleanedUrl = LinkCleaner.clean(url: normalizedURL)
        guard let url = URL(string: cleanedUrl) else {
            self.logger.log(level: .error, "[UNFURLER] Cannot create url for: \(cleanedUrl) (original: \(urlString))")
            throw UnfurlError.cannotCreateURL(LocalizedStringKey("URL Error Text"))
        }
        
        do {
            if let html = try await loadURL(for: cleanedUrl, with: url) {
                let tags = extractAllMetaTags(from: html)
                var metadata = extractMetaData(from: tags)
                
                // Add JSON-LD metadata
                let jsonLdMetadata = extractJsonLDMetadata(from: html)
                for (key, value) in jsonLdMetadata where metadata[key] == nil {
                    metadata[key] = value
                }
                
                // Add additional fallbacks
                let extras = extractAdditionalMetadata(from: html)
                for (key, value) in extras where metadata[key] == nil {
                    metadata[key] = value
                }
                
                // Clean-up URL's and texts from HTML escape characters
                metadata = metadata.mapValues { decodeHTMLEntities($0) }
                
                let favIconURL = extractFaviconURL(from: html, with: url)
                
                // Extract all candidate images from the HTML
                var allImageUrls = extractAllImages(from: html, baseURL: url)
                
                // Add meta tag image if available and not already in the list
                if let metaImageUrl = metadata[MetaKeys.imageUrl],
                   !allImageUrls.contains(metaImageUrl) {
                    allImageUrls.insert(metaImageUrl, at: 0) // Prioritize meta tag image
                }
                
                // If no images found from meta tags or content, try fallback
                if allImageUrls.isEmpty {
                    if let imageFallback = extractMainImageFallback(from: html, baseURL: url) {
                        allImageUrls.append(imageFallback)
                        metadata[MetaKeys.imageUrl] = imageFallback
                    }
                } else {
                    // Set the first (highest scored) image as the primary one for backward compatibility
                    metadata[MetaKeys.imageUrl] = allImageUrls.first
                }
                
                // Download image data for all candidate images (limit to top 10 for performance)
                let limitedImageUrls = Array(allImageUrls.prefix(10))
                let imageOptions = await downloadMultipleImageData(from: limitedImageUrls)
                
                return YabaLinkPreview(
                    url: cleanedUrl,
                    title: metadata[MetaKeys.title],
                    description: metadata[MetaKeys.description],
                    host: metadata[MetaKeys.domain],
                    iconURL: favIconURL,
                    imageURL: metadata[MetaKeys.imageUrl],
                    videoURL: metadata[MetaKeys.videoUrl],
                    iconData: try? await downloadImageData(from: favIconURL),
                    imageData: imageOptions[metadata[MetaKeys.imageUrl] ?? ""], // Use primary image data for backward compatibility
                    imageOptions: imageOptions,
                    readableHTML: html
                )
            }
            return nil
        } catch {
            throw UnfurlError.unableToUnfurl(LocalizedStringKey("Unfurl Error Text"))
        }
    }
    
    /// Normalizes a URL string to handle common user mistakes
    private func normalizeURL(_ urlString: String) -> String {
        var normalized = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Remove any leading/trailing quotes that users might accidentally include
        if (normalized.hasPrefix("\"") && normalized.hasSuffix("\"")) ||
           (normalized.hasPrefix("'") && normalized.hasSuffix("'")) {
            normalized = String(normalized.dropFirst().dropLast())
        }
        
        // Handle common protocol mistakes and missing protocols
        let lowercased = normalized.lowercased()
        
        // Fix common typos in protocols
        if lowercased.hasPrefix("http://") || lowercased.hasPrefix("https://") {
            // Protocol already exists, just normalize case
            if lowercased.hasPrefix("http://") {
                normalized = "http://" + String(normalized.dropFirst(7))
            } else if lowercased.hasPrefix("https://") {
                normalized = "https://" + String(normalized.dropFirst(8))
            }
        } else if lowercased.hasPrefix("www.") {
            // Add https:// to www. URLs
            normalized = "https://" + normalized
        } else if lowercased.hasPrefix("//") {
            // Handle protocol-relative URLs by adding https:
            normalized = "https:" + normalized
        } else if !lowercased.hasPrefix("ftp://") && 
                  !lowercased.hasPrefix("file://") && 
                  !lowercased.hasPrefix("mailto:") {
            // For regular domain names, add https://
            // Check if it looks like a domain (contains at least one dot)
            if normalized.contains(".") && !normalized.contains(" ") {
                normalized = "https://" + normalized
            }
        }
        
        // Remove multiple consecutive slashes in the path (but preserve protocol slashes)
        if let protocolRange = normalized.range(of: "://") {
            let protocolPart = String(normalized[..<protocolRange.upperBound])
            let pathPart = String(normalized[protocolRange.upperBound...])
            let cleanedPath = pathPart.replacingOccurrences(of: "//+", with: "/", options: .regularExpression)
            normalized = protocolPart + cleanedPath
        }
        
        return normalized
    }
    
    private func loadURL(for urlString: String, with url: URL) async throws -> String? {
        var request = URLRequest(url: url)
        // Twitter by themselve broken.
        // I am not falling back to LPMetadataProvider just for them.
        request.setValue("WhatsApp/2", forHTTPHeaderField: "User-Agent")
        request.setValue("https://google.com/", forHTTPHeaderField: "Referer")
        request.timeoutInterval = 15
        
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse,
              200..<300 ~= httpResponse.statusCode else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }
    
    private func extractAllImages(from html: String, baseURL: URL) -> [String] {
        let pattern = #"<img\s+[^>]*src\s*=\s*["']([^"']+)["'][^>]*>"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
            return []
        }
        
        let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)
        let matches = regex.matches(in: html, options: [], range: nsrange)
        
        var candidates: [(src: String, score: Int)] = []
        
        for match in matches {
            guard let range = Range(match.range(at: 1), in: html) else { continue }
            let src = String(html[range])
            
            let lowerSrc = src.lowercased()
            
            // Skip low-quality/unwanted images
            if shouldSkipImage(src: lowerSrc) {
                continue
            }
            
            // Score based on heuristics for quality
            var score = calculateImageScore(src: lowerSrc)
            
            if !lowerSrc.starts(with: "data:") { // skip embedded base64 images
                candidates.append((src, score))
            }
        }
        
        // Sort by score and convert to absolute URLs
        let sortedCandidates = candidates.sorted { $0.score > $1.score }
        return sortedCandidates.compactMap { candidate in
            URL(string: candidate.src, relativeTo: baseURL)?.absoluteString
        }
    }
    
    private func shouldSkipImage(src: String) -> Bool {
        let lowercased = src.lowercased()
        
        // Skip common low-quality indicators
        let skipPatterns = [
            "icon", "sprite", "badge", "button", "arrow", "bullet",
            "tracking", "pixel", "analytics", "ad", "banner",
            "logo", "favicon", "thumbnail", "avatar", "profile"
        ]
        
        for pattern in skipPatterns {
            if lowercased.contains(pattern) {
                return true
            }
        }
        
        // Skip very small images based on common naming patterns
        let sizePatterns = [
            "16x16", "32x32", "48x48", "64x64", "1x1", "2x2",
            "small", "tiny", "mini", "_s.", "_xs.", "_sm."
        ]
        
        for pattern in sizePatterns {
            if lowercased.contains(pattern) {
                return true
            }
        }
        
        return false
    }
    
    private func calculateImageScore(src: String) -> Int {
        var score = 0
        let lowercased = src.lowercased()
        
        // Positive indicators
        if lowercased.contains("hero") || lowercased.contains("main") || lowercased.contains("header") {
            score += 5
        }
        if lowercased.contains("feature") || lowercased.contains("cover") || lowercased.contains("banner") {
            score += 3
        }
        if lowercased.contains("large") || lowercased.contains("big") || lowercased.contains("full") {
            score += 2
        }
        
        // File format preferences
        if lowercased.hasSuffix(".jpg") || lowercased.hasSuffix(".jpeg") {
            score += 2
        } else if lowercased.hasSuffix(".png") {
            score += 1
        } else if lowercased.hasSuffix(".webp") {
            score += 1
        }
        
        // Size indicators in filename
        if lowercased.contains("1200") || lowercased.contains("1920") || lowercased.contains("2048") {
            score += 3
        } else if lowercased.contains("800") || lowercased.contains("1024") {
            score += 2
        } else if lowercased.contains("600") || lowercased.contains("640") {
            score += 1
        }
        
        return score
    }
    
    private func extractMainImageFallback(from html: String, baseURL: URL) -> String? {
        let allImages = extractAllImages(from: html, baseURL: baseURL)
        return allImages.first
    }
    
    private func extractFaviconURL(from html: String, with baseURL: URL) -> String? {
        let rels = ["icon", "shortcut icon", "apple-touch-icon", "mask-icon"]
        let mimePriority = ["image/png", "image/jpeg", "image/jpg", "image/webp", "image/svg+xml", "image/x-icon"]
        
        let pattern = #"<link[^>]*?rel\s*=\s*["']([^"']+)["'][^>]*?>"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
            return nil
        }
        
        let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)
        let matches = regex.matches(in: html, options: [], range: nsrange)
        
        var candidates: [(href: String, type: String?, rel: String)] = []
        
        for match in matches {
            guard let range = Range(match.range, in: html) else { continue }
            let tag = String(html[range])
            
            guard let relMatch = tag.range(of: #"rel\s*=\s*["']([^"']+)["']"#, options: .regularExpression),
                  let hrefMatch = tag.range(of: #"href\s*=\s*["']([^"']+)["']"#, options: .regularExpression) else {
                continue
            }
            
            let rel = String(tag[relMatch])
                .replacingOccurrences(of: #"rel\s*=\s*['"]"#, with: "", options: .regularExpression)
                .trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
            
            let href = String(tag[hrefMatch])
                .replacingOccurrences(of: #"href\s*=\s*['"]"#, with: "", options: .regularExpression)
                .trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
            
            guard rels.contains(where: { rel.localizedCaseInsensitiveContains($0) }) else { continue }
            
            var type: String? = nil
            if let typeMatch = tag.range(of: #"type\s*=\s*["']([^"']+)["']"#, options: .regularExpression) {
                type = String(tag[typeMatch])
                    .replacingOccurrences(of: #"type\s*=\s*['"]"#, with: "", options: .regularExpression)
                    .trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
            }
            
            candidates.append((href: href, type: type, rel: rel))
        }
        
        let sorted = candidates.sorted { a, b in
            let indexA = mimePriority.firstIndex(of: a.type?.lowercased() ?? "") ?? mimePriority.count
            let indexB = mimePriority.firstIndex(of: b.type?.lowercased() ?? "") ?? mimePriority.count
            return indexA < indexB
        }
        
        if let best = sorted.first {
            return URL(string: best.href, relativeTo: baseURL)?.absoluteString
        }
        
        return URL(string: "/favicon.ico", relativeTo: baseURL)?.absoluteString
    }
    
    private func extractAllMetaTags(from html: String) -> [String] {
        let pattern = #"<meta\s+[^>]*?>"#
        do {
            let regex = try NSRegularExpression(pattern: pattern, options: [.caseInsensitive])
            let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)
            return regex.matches(in: html, options: [], range: nsrange).compactMap {
                Range($0.range, in: html).map { String(html[$0]) }
            }
        } catch {
            return []
        }
    }
    
    private func extractMetaData(from metaTags: [String]) -> [String: String] {
        var result: [String: String] = [:]
        
        let prioritizedKeys: [(field: String, candidates: [String])] = [
            (MetaKeys.title,      ["title", "og:title", "twitter:title", "itemprop=\"name\""]),
            (MetaKeys.description,["description", "og:description", "twitter:description", "itemprop=\"description\""]),
            (MetaKeys.type,       ["og:type", "twitter:type"]),
            (MetaKeys.domain,     ["og:site_name", "twitter:domain"]),
            (MetaKeys.videoUrl,   ["og:video", "twitter:player", "itemprop=\"video\""]),
            (MetaKeys.imageUrl,   ["og:image", "twitter:image", "itemprop=\"image\""]),
        ]
        
        for (field, keys) in prioritizedKeys {
            for key in keys {
                if let content = contentForMetaKey(key, in: metaTags) {
                    result[field] = content
                    break
                }
            }
        }
        
        return result
    }
    
    private func contentForMetaKey(_ targetKey: String, in metaTags: [String]) -> String? {
        for tag in metaTags {
            let lower = tag.lowercased()
            
            if lower.contains("property=\"\(targetKey)\"") ||
                lower.contains("name=\"\(targetKey)\"") ||
                lower.contains("itemprop=\(targetKey)") {
                
                if let match = tag.range(of: #"content\s*=\s*["'](.*?)["']"#, options: .regularExpression) {
                    var content = String(tag[match])
                    content = content.replacingOccurrences(of: #"content\s*=\s*['"]"#, with: "", options: .regularExpression)
                    return content.trimmingCharacters(in: CharacterSet(charactersIn: "\"'"))
                }
            }
        }
        return nil
    }
    
    private func extractJsonLDMetadata(from html: String) -> [String: String] {
        var result: [String: String] = [:]
        let pattern = #"<script[^>]+type=["']application/ld\+json["'][^>]*>(.*?)</script>"#
        
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.dotMatchesLineSeparators, .caseInsensitive]) else {
            return result
        }
        
        let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)
        let matches = regex.matches(in: html, options: [], range: nsrange)
        
        for match in matches {
            guard let range = Range(match.range(at: 1), in: html) else { continue }
            let jsonString = String(html[range])
            
            if let data = jsonString.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] {
                
                if let title = json["headline"] as? String ?? json["name"] as? String {
                    result[MetaKeys.title] = title
                }
                if let desc = json["description"] as? String {
                    result[MetaKeys.description] = desc
                }
                if let image = json["image"] as? String {
                    result[MetaKeys.imageUrl] = image
                } else if let imageArray = json["image"] as? [String] {
                    result[MetaKeys.imageUrl] = imageArray.first
                }
                if let video = json["video"] as? [String: Any],
                   let contentUrl = video["contentUrl"] as? String {
                    result[MetaKeys.videoUrl] = contentUrl
                }
            }
        }
        
        return result
    }
    
    private func extractAdditionalMetadata(from html: String) -> [String: String] {
        var result: [String: String] = [:]
        
        if let title = html.slice(from: "<title>", to: "</title>") {
            result[MetaKeys.title] = title
        }
        
        if result[MetaKeys.description] == nil,
           let match = html.range(of: #"<p>(.*?)</p>"#, options: .regularExpression) {
            result[MetaKeys.description] = String(html[match])
                .replacingOccurrences(of: "<[^>]+>", with: "", options: .regularExpression)
                .trimmingCharacters(in: .whitespacesAndNewlines)
        }
        
        return result
    }
    
    private func downloadImageData(from urlString: String?) async throws -> Data? {
        guard let urlString, let url = URL(string: urlString) else { return nil }
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let httpResponse = response as? HTTPURLResponse,
              200..<300 ~= httpResponse.statusCode else {
            return nil
        }
        return data
    }
    
    private func downloadMultipleImageData(from imageUrls: [String]) async -> [String: Data] {
        var imageOptions: [String: Data] = [:]
        
        // Download images concurrently
        await withTaskGroup(of: (String, Data?).self) { group in
            for imageUrl in imageUrls {
                group.addTask {
                    let data = try? await self.downloadImageData(from: imageUrl)
                    return (imageUrl, data)
                }
            }
            
            for await (url, data) in group {
                if let data = data {
                    // Additional quality filtering based on actual image data
                    if await self.isHighQualityImage(data: data) {
                        imageOptions[url] = data
                    }
                }
            }
        }
        
        return imageOptions
    }
    
    private func isHighQualityImage(data: Data) async -> Bool {
        // Basic size check - skip very small images (likely icons/sprites)
        if data.count < 1024 { // Less than 1KB
            return false
        }
        
        // For more sophisticated filtering, you could decode the image
        // and check dimensions, but for now we'll use file size as a proxy
        // Skip extremely large files too (over 5MB) for performance
        if data.count > 5 * 1024 * 1024 {
            return false
        }
        
        return true
    }
    
    func decodeHTMLEntities(_ string: String) -> String {
        guard let data = string.data(using: .utf8) else { return string }
        let options: [NSAttributedString.DocumentReadingOptionKey: Any] = [
            .documentType: NSAttributedString.DocumentType.html,
            .characterEncoding: String.Encoding.utf8.rawValue
        ]
        
        if let attributedString = try? NSAttributedString(data: data, options: options, documentAttributes: nil) {
            return attributedString.string
        }
        
        return string
    }
}
