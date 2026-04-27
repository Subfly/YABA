//
//  MarkdownReadableAssetProcessor.swift
//  YABACore
//
//  Parses readable Markdown, downloads HTTP(S) images in first-seen order, and rewrites destinations
//  to stable `yaba-asset://<assetId>` references (no `../assets/...` paths in stored content).
//

import Foundation
import MarkdownParser

public enum MarkdownReadableAssetProcessor {
    public static func process(markdown: String, baseURL: String) async -> ReadableUnfurl {
        let document = MarkdownParser.Companion().parseToDocument(
            input: markdown,
            flavour: ExtendedFlavour()
        )
        let astDestinations = collectImageDestinationsFromAst(document: document)
        let spans = ImageSpanScanner.orderedOccurrences(
            in: markdown,
            expectedCount: astDestinations.count
        )
        
        if astDestinations.isEmpty {
            return ReadableUnfurl(markdown: markdown, assets: [])
        }
        
        if spans.count != astDestinations.count {
            // Scanner / AST disagree — do not risk corrupting the body.
            return ReadableUnfurl(markdown: markdown, assets: [])
        }

        var seenAbsolute = Set<String>()
        var uniqueDownloadOrder: [String] = []
        for d in astDestinations {
            guard let abs = HTMLImageURLExtractor.resolveForDownload(
                raw: d,
                baseURL: baseURL
            ) else { continue }
            
            if seenAbsolute.insert(abs).inserted {
                uniqueDownloadOrder.append(abs)
            }
        }
        
        var bytesByAbsolute: [String: (id: String, ext: String, data: Data)] = [:]
        for abs in uniqueDownloadOrder {
            guard let u = URL(string: abs) else { continue }
            let data: Data
            
            do {
                data = try await UnfurlHttpClient.getBytes(url: u)
            } catch {
                continue
            }
            if data.isEmpty { continue }
            
            var extHint = inferImageExtension(bytes: data, url: abs)
            let compressed = YabaImageCompression.compressImageData(data) ?? data
            if compressed != data {
                extHint = inferImageExtension(bytes: compressed, url: abs)
            }
            if extHint == "jpg" { extHint = "jpeg" }
            let id = UUID().uuidString
            bytesByAbsolute[abs] = (id, extHint, compressed)
        }

        let assets = uniqueDownloadOrder.compactMap { abs -> ReadableAssetPayload? in
            guard let pack = bytesByAbsolute[abs] else { return nil }
            return ReadableAssetPayload(
                assetId: pack.id,
                pathExtension: pack.ext,
                bytes: pack.data
            )
        }

        let absByIndex: [String?] = astDestinations.map { HTMLImageURLExtractor.resolveForDownload(raw: $0, baseURL: baseURL)
        }
        let yabaByIndex: [String?] = absByIndex.map { abs in
            guard let abs, let pack = bytesByAbsolute[abs] else { return nil }
            return "yaba-asset://\(pack.id)"
        }
        var result = NSString(string: markdown)
        
        for k in (0 ..< spans.count).sorted(by: { spans[$0].utf16Range.location > spans[$1].utf16Range.location }) {
            let occ = spans[k]
            guard let yaba = yabaByIndex[k] else { continue }
            switch occ {
            case .inlineUrl(let r):
                result = result.replacingCharacters(in: r, with: yaba) as NSString
            case .refStyle(let full, let alt):
                // Escape ']' in alt is rare; alt is taken from the same source as the parser.
                result = result.replacingCharacters(
                    in: full,
                    with: "![\(alt)](\(yaba))"
                ) as NSString
            }
        }
        
        return ReadableUnfurl(markdown: result as String, assets: assets)
    }

    // MARK: - AST (pre-order: matches scanner order for normal blocks)

    private static func collectImageDestinationsFromAst(document: Document) -> [String] {
        var out: [String] = []
        visit(node: document, into: &out)
        return out
    }

    private static func visit(node: Node, into out: inout [String]) {
        if let image = node as? Image {
            out.append(image.destination)
        } else if let fig = node as? Figure {
            out.append(fig.imageUrl)
        }
        guard let c = node as? ContainerNode else { return }
        for child in c.children {
            visit(node: child, into: &out)
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

// MARK: - Source scan (find each image, aligned 1:1 with AST pre-order)

private enum ScannedImageOccurrence {
    case inlineUrl(NSRange)
    case refStyle(full: NSRange, alt: String)

    var utf16Range: NSRange {
        switch self {
        case .inlineUrl(let r): return r
        case .refStyle(let full, _): return full
        }
    }
}

private enum ImageSpanScanner {
    private static let exclamation: unichar = 33 // !
    private static let lBracket: unichar = 91 // [
    private static let rBracket: unichar = 93 // ]
    private static let lParen: unichar = 40 // (
    private static let rParen: unichar = 41 // )
    private static let lAngle: unichar = 60 // <
    private static let rAngle: unichar = 62 // >
    private static let backslash: unichar = 92 // \
    private static let space: unichar = 32 //  
    private static let tab: unichar = 9
    private static let newline: unichar = 10

    static func orderedOccurrences(in markdown: String, expectedCount: Int) -> [ScannedImageOccurrence] {
        guard expectedCount > 0 else { return [] }
        let ns = markdown as NSString
        let n = ns.length
        var i = 0
        var out: [ScannedImageOccurrence] = []
        while i < n, out.count < expectedCount {
            let range = NSRange(location: i, length: n - i)
            let found = ns.range(of: "![", options: [], range: range, locale: nil)
            if found.location == NSNotFound { break }
            if let o = tryScan(ns: ns, n: n, at: found.location) {
                out.append(o)
                switch o {
                case .inlineUrl(let r):
                    i = r.location + r.length
                case .refStyle(let full, _):
                    i = full.location + full.length
                }
            } else {
                i = found.location + 2
            }
        }
        return out
    }

    private static func tryScan(ns: NSString, n: Int, at bang: Int) -> ScannedImageOccurrence? {
        // "!["
        guard bang + 1 < n, ns.character(at: bang) == exclamation, ns.character(at: bang + 1) == lBracket else {
            return nil
        }
        guard let labelClose = findUnescaped(ns: ns, n: n, from: bang + 2, target: rBracket) else { return nil }
        let next = labelClose + 1
        guard next < n else { return nil }

        if ns.character(at: next) == lParen {
            guard let destRange = parseInlineDestinationRange(ns: ns, n: n, openParen: next) else { return nil }
            return .inlineUrl(destRange)
        }

        if ns.character(at: next) == lBracket {
            guard let refClose = findUnescaped(ns: ns, n: n, from: next + 1, target: rBracket) else { return nil }
            let altRange = NSRange(location: bang + 2, length: labelClose - (bang + 2))
            let fullRange = NSRange(location: bang, length: (refClose + 1) - bang)
            return .refStyle(full: fullRange, alt: ns.substring(with: altRange))
        }

        return nil
    }

    private static func findUnescaped(
        ns: NSString,
        n: Int,
        from start: Int,
        target: unichar
    ) -> Int? {
        var i = start
        while i < n {
            let ch = ns.character(at: i)
            if ch == backslash, i + 1 < n {
                i += 2
                continue
            }
            if ch == target { return i }
            i += 1
        }
        return nil
    }

    private static func parseInlineDestinationRange(ns: NSString, n: Int, openParen: Int) -> NSRange? {
        guard openParen < n, ns.character(at: openParen) == lParen else { return nil }
        var i = openParen + 1
        while i < n {
            let ch = ns.character(at: i)
            if ch == space || ch == tab || ch == newline {
                i += 1
                continue
            }
            break
        }
        guard i < n, ns.character(at: i) != rParen else { return nil }

        // <url>
        if ns.character(at: i) == lAngle {
            i += 1
            let start = i
            while i < n {
                let ch = ns.character(at: i)
                if ch == lAngle || ch == newline { return nil }
                if ch == backslash, i + 1 < n {
                    i += 2
                    continue
                }
                if ch == rAngle {
                    return NSRange(location: start, length: i - start)
                }
                i += 1
            }
            return nil
        }

        // url (allow balanced nested parentheses)
        let start = i
        var depth = 0
        while i < n {
            let ch = ns.character(at: i)
            if ch == backslash, i + 1 < n {
                i += 2
                continue
            }
            if depth == 0, (ch == space || ch == tab || ch == newline || ch == rParen) {
                break
            }
            if ch == lParen { depth += 1 }
            else if ch == rParen, depth > 0 { depth -= 1 }
            if ch < space { break }
            i += 1
        }
        return NSRange(location: start, length: i - start)
    }
}
