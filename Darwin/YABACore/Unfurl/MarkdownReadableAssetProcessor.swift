//
//  MarkdownReadableAssetProcessor.swift
//  YABACore
//
//  Parses readable Markdown, downloads HTTP(S) images in first-seen order, and rewrites destinations
//  to stable `yaba-asset://<assetId>` references (no `../assets/...` paths in stored content).
//

import Foundation
import Markdown

public enum MarkdownReadableAssetProcessor {
    public static func process(markdown: String, baseURL: String) async -> ReadableUnfurl {
        let trimmed = markdown.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty {
            return ReadableUnfurl(markdown: "", assets: [])
        }
        let document = Document(parsing: trimmed)
        var collector = ImageURLCollector(baseURL: baseURL)
        collector.visitDocument(document)
        var absoluteToYaba: [String: String] = [:]
        var assets: [ReadableAssetPayload] = []
        for absolute in collector.orderedAbsolutes {
            guard absoluteToYaba[absolute] == nil else { continue }
            guard let remote = URL(string: absolute),
                  let scheme = remote.scheme?.lowercased(), scheme == "http" || scheme == "https"
            else { continue }
            guard let bytes = try? await UnfurlHttpClient.getBytes(url: remote) else { continue }
            let out = YabaImageCompression.compressDataPreservingFormat(bytes)
            let ext = inferImageExtension(bytes: out, url: absolute)
            let assetId = UUID().uuidString
            let yaba = "yaba-asset://\(assetId)"
            absoluteToYaba[absolute] = yaba
            assets.append(ReadableAssetPayload(assetId: assetId, pathExtension: ext, bytes: out))
        }
        var rewriter = ImageDestinationRewriter(baseURL: baseURL, absoluteToYaba: absoluteToYaba)
        guard let rewritten = rewriter.visitDocument(document) as? Document else {
            return ReadableUnfurl(markdown: trimmed, assets: assets)
        }
        let out = rewritten.format()
        return ReadableUnfurl(markdown: out, assets: assets)
    }

    fileprivate struct ImageURLCollector: MarkupWalker {
        let baseURL: String
        fileprivate(set) var orderedAbsolutes: [String] = []
        fileprivate var seen = Set<String>()

        mutating func visitImage(_ image: Image) {
            let raw = (image.source ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if !raw.isEmpty,
               let abs = HTMLImageURLExtractor.resolveForDownload(raw: raw, baseURL: baseURL),
               seen.insert(abs).inserted {
                orderedAbsolutes.append(abs)
            }
        }
    }

    fileprivate struct ImageDestinationRewriter: MarkupRewriter {
        let baseURL: String
        let absoluteToYaba: [String: String]

        mutating func visitImage(_ image: Image) -> Markup? {
            let raw = (image.source ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            if raw.isEmpty { return image }
            guard let abs = HTMLImageURLExtractor.resolveForDownload(raw: raw, baseURL: baseURL),
                  let yaba = absoluteToYaba[abs]
            else { return image }
            return Image(source: yaba, title: image.title ?? "")
        }
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
