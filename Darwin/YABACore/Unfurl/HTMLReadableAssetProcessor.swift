//
//  HTMLReadableAssetProcessor.swift
//  YABACore
//
//  Downloads images referenced in sanitized HTML, rewrites `src` / `srcset` to `../assets/<id>.<ext>`.
//

import Foundation
import SwiftSoup

public enum HTMLReadableAssetProcessor {
    public static func process(html: String, baseURL: String) async -> ReadableUnfurl {
        let trimmed = html.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return ReadableUnfurl(html: "", assets: [])
        }
        guard let doc = try? SwiftSoup.parse(trimmed, baseURL) else {
            return ReadableUnfurl(html: trimmed, assets: [])
        }

        let ordered = (try? collectImageURLs(doc: doc, baseURL: baseURL)) ?? []
        var absoluteToRelative: [String: String] = [:]
        var assets: [ReadableAssetPayload] = []

        for absolute in ordered {
            guard absoluteToRelative[absolute] == nil else { continue }
            guard let remote = URL(string: absolute),
                  let scheme = remote.scheme?.lowercased(), scheme == "http" || scheme == "https"
            else { continue }
            guard let bytes = try? await UnfurlHttpClient.getBytes(url: remote) else { continue }
            let out = YabaImageCompression.compressDataPreservingFormat(bytes)
            let ext = inferImageExtension(bytes: out, url: absolute)
            let assetId = UUID().uuidString
            let relative = "../assets/\(assetId).\(ext)"
            absoluteToRelative[absolute] = relative
            assets.append(ReadableAssetPayload(assetId: assetId, pathExtension: ext, bytes: out))
        }

        try? rewriteImageAttributes(in: doc, baseURL: baseURL, absoluteToRelative: absoluteToRelative)
        let outHtml = (try? doc.body()?.html())
            ?? (try? doc.html())
            ?? trimmed
        return ReadableUnfurl(html: outHtml, assets: assets)
    }

    private static func collectImageURLs(doc: Document, baseURL: String) throws -> [String] {
        var seen = Set<String>()
        var ordered: [String] = []
        func record(_ raw: String) {
            guard let abs = HTMLImageURLExtractor.resolveForDownload(raw: raw, baseURL: baseURL) else { return }
            if seen.insert(abs).inserted {
                ordered.append(abs)
            }
        }
        for el in try doc.select("img[src],img[srcset]") {
            if let src = try? el.attr("src"), !src.isEmpty { record(src) }
            if let srcset = try? el.attr("srcset"), !srcset.isEmpty {
                for u in HTMLImageURLExtractor.srcsetImageURLs(srcset) { record(u) }
            }
        }
        for el in try doc.select("source[srcset]") {
            if let srcset = try? el.attr("srcset"), !srcset.isEmpty {
                for u in HTMLImageURLExtractor.srcsetImageURLs(srcset) { record(u) }
            }
        }
        return ordered
    }

    private static func rewriteImageAttributes(
        in doc: Document,
        baseURL: String,
        absoluteToRelative: [String: String]
    ) throws {
        for el in try doc.select("img[src]") {
            let src = try el.attr("src")
            guard !src.isEmpty, let abs = HTMLImageURLExtractor.resolveForDownload(raw: src, baseURL: baseURL),
                  let rel = absoluteToRelative[abs]
            else { continue }
            try el.attr("src", rel)
        }
        for el in try doc.select("img[srcset]") {
            let srcset = try el.attr("srcset")
            guard !srcset.isEmpty else { continue }
            let newVal = rewriteSrcset(srcset, baseURL: baseURL, map: absoluteToRelative)
            if newVal != srcset {
                try el.attr("srcset", newVal)
            }
        }
        for el in try doc.select("source[srcset]") {
            let srcset = try el.attr("srcset")
            guard !srcset.isEmpty else { continue }
            let newVal = rewriteSrcset(srcset, baseURL: baseURL, map: absoluteToRelative)
            if newVal != srcset {
                try el.attr("srcset", newVal)
            }
        }
    }

    private static func rewriteSrcset(_ srcset: String, baseURL: String, map: [String: String]) -> String {
        let parts = srcset.split(separator: ",")
        let rebuilt: [String] = parts.map { part in
            let trimmed = part.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return String(part) }
            let tokens = trimmed.split(whereSeparator: { $0.isWhitespace }).map(String.init)
            guard let first = tokens.first, !first.isEmpty else { return String(part) }
            let rest = tokens.dropFirst().joined(separator: " ")
            guard let abs = HTMLImageURLExtractor.resolveForDownload(raw: first, baseURL: baseURL),
                  let rel = map[abs]
            else {
                return String(part)
            }
            if rest.isEmpty {
                return rel
            }
            return "\(rel) \(rest)"
        }
        return rebuilt.joined(separator: ", ")
    }

    private static func inferImageExtension(bytes: Data, url: String) -> String {
        if bytes.count >= 3, bytes[0] == 0xFF, bytes[1] == 0xD8, bytes[2] == 0xFF { return "jpg" }
        if bytes.count >= 4, bytes[0] == 0x89, bytes[1] == 0x50, bytes[2] == 0x4E, bytes[3] == 0x47 { return "png" }
        if bytes.count >= 3, bytes[0] == 0x47, bytes[1] == 0x49, bytes[2] == 0x46 { return "gif" }
        if bytes.count >= 12, bytes[0] == 0x52, bytes[1] == 0x49, bytes[2] == 0x46, bytes[3] == 0x46,
           bytes[8] == 0x57, bytes[9] == 0x45, bytes[10] == 0x42, bytes[11] == 0x50 { return "webp" }
        let u = url.lowercased()
        if u.contains(".png") { return "png" }
        if u.contains(".gif") { return "gif" }
        if u.contains(".webp") { return "webp" }
        if u.contains(".jpeg") || u.contains(".jpg") { return "jpg" }
        return "jpg"
    }
}
