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
        guard let url = URL(string: urlString) else {
            self.logger.log(level: .error, "[UNFURLER] Cannot create url for: \(urlString)")
            throw UnfurlError.cannotCreateURL(LocalizedStringKey("URL Error Text"))
        }
        
        do {
            if let html = try await loadURL(for: urlString, with: url) {
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
                
                if metadata[MetaKeys.imageUrl] == nil {
                    let imageFallback = extractMainImageFallback(from: html, baseURL: url)
                    metadata[MetaKeys.imageUrl] = imageFallback
                }
                
                return YabaLinkPreview(
                    url: urlString,
                    title: metadata[MetaKeys.title],
                    description: metadata[MetaKeys.description],
                    host: metadata[MetaKeys.domain],
                    iconURL: favIconURL,
                    imageURL: metadata[MetaKeys.imageUrl],
                    videoURL: metadata[MetaKeys.videoUrl],
                    iconData: try? await downloadImageData(from: favIconURL),
                    imageData: try? await downloadImageData(from: metadata[MetaKeys.imageUrl]),
                    readableHTML: nil // TODO: ENABLE WHEN HTML RENDERING IS WELL DESIGNED
                )
            }
            return nil
        } catch {
            throw UnfurlError.unableToUnfurl(LocalizedStringKey("Unfurl Error Text"))
        }
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
    
    private func extractMainImageFallback(from html: String, baseURL: URL) -> String? {
        let pattern = #"<img\s+[^>]*src\s*=\s*["']([^"']+)["'][^>]*>"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
            return nil
        }
        
        let nsrange = NSRange(html.startIndex..<html.endIndex, in: html)
        let matches = regex.matches(in: html, options: [], range: nsrange)
        
        var candidates: [(src: String, score: Int)] = []
        
        for match in matches {
            guard let range = Range(match.range(at: 1), in: html) else { continue }
            let src = String(html[range])
            
            let lowerSrc = src.lowercased()
            
            // Score based on heuristics
            var score = 0
            if lowerSrc.contains("hero") || lowerSrc.contains("main") || lowerSrc.contains("header") {
                score += 3
            }
            if lowerSrc.contains("logo") || lowerSrc.contains("icon") || lowerSrc.contains("sprite") {
                score -= 5
            }
            if lowerSrc.hasSuffix(".png") || lowerSrc.hasSuffix(".jpg") || lowerSrc.hasSuffix(".jpeg") {
                score += 1
            }
            if !lowerSrc.starts(with: "data:") { // skip embedded base64 images
                candidates.append((src, score))
            }
        }
        
        print(candidates)
        
        let best = candidates.sorted { $0.score > $1.score }.first
        return best.flatMap { URL(string: $0.src, relativeTo: baseURL)?.absoluteString }
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
