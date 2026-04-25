//
//  HTMLImageURLExtractor.swift
//  YABACore
//
//  Collects HTTP(S) image URLs from sanitized HTML (img/src, srcset, picture/source).
//

import Foundation
import SwiftSoup

public enum HTMLImageURLExtractor {
    /// Unique absolute image URLs in first-seen order (only `http` / `https`).
    public static func imageURLs(from html: String, baseURL: String) throws -> [String] {
        let trimmed = html.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return [] }
        let doc = try SwiftSoup.parse(trimmed, baseURL)
        var seen = Set<String>()
        var ordered: [String] = []

        func record(_ raw: String) {
            guard let abs = resolveForDownload(raw: raw, baseURL: baseURL) else { return }
            if seen.insert(abs).inserted {
                ordered.append(abs)
            }
        }

        for el in try doc.select("img[src],img[srcset]") {
            if let src = try? el.attr("src"), !src.isEmpty {
                record(src)
            }
            if let srcset = try? el.attr("srcset"), !srcset.isEmpty {
                for u in srcsetImageURLs(srcset) {
                    record(u)
                }
            }
        }
        for el in try doc.select("source[srcset]") {
            if let srcset = try? el.attr("srcset"), !srcset.isEmpty {
                for u in srcsetImageURLs(srcset) {
                    record(u)
                }
            }
        }
        return ordered
    }

    // MARK: - URL resolution

    /// Resolves a raw attribute value to an absolute URL string, or nil if not a downloadable HTTP(S) image URL.
    public static func resolveForDownload(raw: String, baseURL: String) -> String? {
        let t = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.isEmpty { return nil }
        if t.lowercased().hasPrefix("data:") { return nil }
        if t.hasPrefix("#") { return nil }
        if let u = URL(string: t), let s = u.scheme?.lowercased(), s == "http" || s == "https" {
            return u.absoluteString
        }
        if t.hasPrefix("//") {
            guard let b = URL(string: baseURL), let scheme = b.scheme else { return nil }
            let joined = "\(scheme):\(t)"
            guard let u = URL(string: joined), let s = u.scheme?.lowercased(), s == "http" || s == "https" else { return nil }
            return u.absoluteString
        }
        guard let resolved = URL(string: t, relativeTo: URL(string: baseURL))?.absoluteURL,
              let s = resolved.scheme?.lowercased(),
              s == "http" || s == "https"
        else { return nil }
        return resolved.absoluteString
    }

    /// Splits a `srcset` value into candidate URL strings (before width/density descriptors).
    public static func srcsetImageURLs(_ srcset: String) -> [String] {
        srcset.split(separator: ",").compactMap { part -> String? in
            let trimmed = part.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return nil }
            let firstToken = trimmed.split(whereSeparator: { $0.isWhitespace }).map(String.init).first
            return firstToken
        }
    }
}
