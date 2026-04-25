//
//  LinkMetadataExtractor.swift
//  YABACore
//
//  HTML-only link metadata (Open Graph, Twitter, meta, JSON-LD, icons).
//

import Foundation
import SwiftSoup

public enum LinkMetadataExtractor {
    public static func extract(html: String, pageUrl: String) throws -> LinkMetadataResult {
        let cleanedUrl = LinkCleaner.clean(pageUrl)
        let baseForResolve = URL(string: cleanedUrl) ?? URL(string: pageUrl.trimmingCharacters(in: .whitespacesAndNewlines))

        let doc = try Parser.parse(html, cleanedUrl)

        let jsonLd = JsonLdMetadata.extract(from: doc)

        let title = firstNonEmpty(
            metaProperty(doc, "og:title"),
            metaName(doc, "twitter:title"),
            metaName(doc, "parsely-title"),
            jsonLd.title,
            documentTitle(doc)
        )

        let description = firstNonEmpty(
            metaProperty(doc, "og:description"),
            metaName(doc, "twitter:description"),
            metaName(doc, "description"),
            jsonLd.description
        )

        let author = firstNonEmpty(
            metaProperty(doc, "article:author"),
            metaName(doc, "author"),
            metaName(doc, "twitter:creator"),
            metaName(doc, "sailthru.author"),
            jsonLd.author
        )

        let date = firstNonEmpty(
            metaProperty(doc, "article:published_time"),
            metaProperty(doc, "article:modified_time"),
            metaProperty(doc, "og:updated_time"),
            metaName(doc, "date"),
            metaName(doc, "pubdate"),
            jsonLd.date
        )

        let image = firstNonEmpty(
            metaProperty(doc, "og:image"),
            metaName(doc, "twitter:image"),
            metaName(doc, "twitter:image:src"),
            jsonLd.image
        ).map { resolveUrl($0, base: baseForResolve) }

        let audio = firstNonEmpty(
            metaProperty(doc, "og:audio"),
            metaProperty(doc, "og:audio:url"),
            metaName(doc, "twitter:player:stream")
        ).map { resolveUrl($0, base: baseForResolve) }

        let video = firstNonEmpty(
            metaProperty(doc, "og:video"),
            metaProperty(doc, "og:video:url"),
            metaName(doc, "twitter:player")
        ).map { resolveUrl($0, base: baseForResolve) }

        let logo = pickLogo(doc: doc, base: baseForResolve, jsonLdImage: jsonLd.logoOrImage)

        return LinkMetadataResult(
            cleanedUrl: cleanedUrl,
            title: title,
            description: description,
            author: author,
            date: date,
            audio: audio,
            video: video,
            image: image,
            logo: logo
        )
    }

    private static func documentTitle(_ doc: Document) -> String? {
        normalizeOptional((try? doc.title()) ?? nil)
    }

    private static func metaProperty(_ doc: Document, _ property: String) -> String? {
        normalizeOptional(try? doc.select("meta[property=\"\(cssEscape(property))\"]").first()?.attr("content"))
    }

    private static func metaName(_ doc: Document, _ name: String) -> String? {
        normalizeOptional(try? doc.select("meta[name=\"\(cssEscape(name))\"]").first()?.attr("content"))
    }

    private static func cssEscape(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
    }

    private static func pickLogo(doc: Document, base: URL?, jsonLdImage: String?) -> String? {
        if let ogLogo = normalizeOptional(try? doc.select("meta[property=\"og:logo\"]").first()?.attr("content")) {
            return resolveUrl(ogLogo, base: base)
        }
        if let fromJson = normalizeOptional(jsonLdImage) {
            return resolveUrl(fromJson, base: base)
        }
        if let href = firstLinkHref(doc, relHints: ["apple-touch-icon", "shortcut icon", "icon", "mask-icon"]) {
            return resolveUrl(href, base: base)
        }
        return nil
    }

    private static func firstLinkHref(_ doc: Document, relHints: [String]) -> String? {
        guard let links = try? doc.select("link[rel][href]") else { return nil }
        for relHint in relHints {
            for el in links {
                guard let rel = try? el.attr("rel").lowercased() else { continue }
                let tokens = rel.split(whereSeparator: { $0.isWhitespace }).map(String.init)
                if tokens.contains(where: { $0 == relHint }) {
                    if let href = normalizeOptional(try? el.attr("href")) {
                        return href
                    }
                }
            }
        }
        return nil
    }

    private static func resolveUrl(_ raw: String, base: URL?) -> String {
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.hasPrefix("//"), let b = base, let scheme = b.scheme {
            return "\(scheme):\(t)"
        }
        if t.lowercased().hasPrefix("http://") || t.lowercased().hasPrefix("https://") {
            return t
        }
        if t.hasPrefix("data:") || t.hasPrefix("#") || t.hasPrefix("mailto:") {
            return t
        }
        guard let base, let resolved = URL(string: t, relativeTo: base)?.absoluteString else {
            return t
        }
        return resolved
    }

    private static func firstNonEmpty(_ parts: String?...) -> String? {
        for p in parts {
            if let v = normalizeOptional(p) { return v }
        }
        return nil
    }

    private static func normalizeOptional(_ s: String?) -> String? {
        guard let s = s?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return nil }
        let lower = s.lowercased()
        if lower == "null" || lower == "undefined" { return nil }
        return s
    }
}

// MARK: - JSON-LD

private struct JsonLdMetadata {
    var title: String?
    var description: String?
    var author: String?
    var date: String?
    var image: String?
    var logoOrImage: String?

    static func extract(from doc: Document) -> JsonLdMetadata {
        var out = JsonLdMetadata()
        guard let scripts = try? doc.select("script[type=\"application/ld+json\"]") else { return out }
        for el in scripts {
            guard let text = try? el.data(), !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { continue }
            guard let data = text.data(using: .utf8),
                  let json = try? JSONSerialization.jsonObject(with: data)
            else { continue }
            merge(json: json, into: &out)
        }
        return out
    }

    private static func merge(json: Any, into out: inout JsonLdMetadata) {
        if let arr = json as? [Any] {
            for item in arr { merge(json: item, into: &out) }
            return
        }
        guard let dict = json as? [String: Any] else { return }

        if let graph = dict["@graph"] as? [Any] {
            for item in graph { merge(json: item, into: &out) }
        }

        ingestObject(dict, into: &out)

        for (_, value) in dict {
            if let nested = value as? [String: Any] {
                merge(json: nested, into: &out)
            } else if let nestedArr = value as? [Any] {
                for item in nestedArr { merge(json: item, into: &out) }
            }
        }
    }

    private static func ingestObject(_ dict: [String: Any], into out: inout JsonLdMetadata) {
        let types = typeTokens(from: dict["@type"])
        let isCreative = types.contains(where: { creativeTypes.contains($0) })

        if isCreative || out.title == nil {
            if let t = stringFromLd(dict["headline"]) ?? stringFromLd(dict["name"]) ?? stringFromLd(dict["title"]) {
                out.title = out.title ?? t
            }
        }
        if isCreative || out.description == nil {
            if let d = stringFromLd(dict["description"]) {
                out.description = out.description ?? d
            }
        }
        if isCreative || out.date == nil {
            if let d = stringFromLd(dict["datePublished"]) ?? stringFromLd(dict["dateModified"]) ?? stringFromLd(dict["uploadDate"]) {
                out.date = out.date ?? d
            }
        }
        if isCreative || out.author == nil {
            if let a = authorFromLd(dict["author"]) ?? authorFromLd(dict["creator"]) {
                out.author = out.author ?? a
            }
        }
        if isCreative || out.image == nil {
            if let u = imageUrlFromLd(dict["image"]) {
                out.image = out.image ?? u
            }
        }
        if let logo = dict["logo"] {
            if let u = imageUrlFromLd(logo) ?? stringFromLd(logo) {
                out.logoOrImage = out.logoOrImage ?? u
            }
        }
        if let pub = dict["publisher"] as? [String: Any], let logo = pub["logo"] {
            if let u = imageUrlFromLd(logo) ?? stringFromLd(logo) {
                out.logoOrImage = out.logoOrImage ?? u
            }
        }
    }

    private static let creativeTypes: Set<String> = [
        "article", "newsarticle", "blogposting", "techarticle", "scholarlyarticle",
        "webpage", "website", "blog", "itempage", "creativework",
    ]

    private static func typeTokens(from any: Any?) -> [String] {
        if let s = any as? String {
            return [s.lowercased()]
        }
        if let arr = any as? [Any] {
            return arr.compactMap { ($0 as? String)?.lowercased() }
        }
        return []
    }

    private static func stringFromLd(_ any: Any?) -> String? {
        guard let any else { return nil }
        if let s = any as? String {
            let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
            return t.isEmpty ? nil : t
        }
        if let dict = any as? [String: Any] {
            if let t = dict["@value"] as? String { return stringFromLd(t) }
            if let t = dict["name"] as? String { return stringFromLd(t) }
            if let t = dict["url"] as? String { return stringFromLd(t) }
        }
        return nil
    }

    private static func authorFromLd(_ any: Any?) -> String? {
        guard let any else { return nil }
        if let s = any as? String { return stringFromLd(s) }
        if let dict = any as? [String: Any] {
            return stringFromLd(dict["name"]) ?? stringFromLd(dict["url"])
        }
        if let arr = any as? [Any] {
            for item in arr {
                if let name = authorFromLd(item) { return name }
            }
        }
        return nil
    }

    private static func imageUrlFromLd(_ any: Any?) -> String? {
        guard let any else { return nil }
        if let s = any as? String { return stringFromLd(s) }
        if let dict = any as? [String: Any] {
            return stringFromLd(dict["url"]) ?? stringFromLd(dict["@id"])
        }
        if let arr = any as? [Any] {
            for item in arr {
                if let u = imageUrlFromLd(item) { return u }
            }
        }
        return nil
    }
}
