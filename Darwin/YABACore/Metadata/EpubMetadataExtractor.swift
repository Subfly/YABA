//
//  EpubMetadataExtractor.swift
//  YABACore
//
//  EPUB metadata and cover image via EPUBKit.
//

import EPUBKit
import Foundation

public enum EpubMetadataExtractorError: Error {
    case couldNotParse
    case couldNotWriteTemporaryFile
}

public enum EpubMetadataExtractor {
    public static func extract(fromFile url: URL) -> EpubMetadataResult? {
        guard let document = EPUBDocument(url: url) else { return nil }
        return map(document)
    }

    public static func extract(from data: Data) throws -> EpubMetadataResult {
        let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("yaba-epub-\(UUID().uuidString).epub")
        do {
            try data.write(to: tmp, options: .atomic)
        } catch {
            throw EpubMetadataExtractorError.couldNotWriteTemporaryFile
        }
        defer {
            try? FileManager.default.removeItem(at: tmp)
        }
        guard let document = EPUBDocument(url: tmp) else {
            throw EpubMetadataExtractorError.couldNotParse
        }
        return map(document)
    }

    private static func map(_ document: EPUBDocument) -> EpubMetadataResult {
        let title = normalize(document.metadata.title ?? document.title)
        let author = normalize(document.author ?? document.metadata.publisher)
        let description = normalize(document.metadata.description)
        let pubdate = normalize(document.metadata.date)

        var coverImageData: Data?
        var coverImageMimeType: String?
        if let coverFile = document.cover,
           let data = try? Data(contentsOf: coverFile),
           !data.isEmpty {
            coverImageData = data
            coverImageMimeType = mimeType(forExtension: coverFile.pathExtension)
        }

        return EpubMetadataResult(
            coverImageData: coverImageData,
            coverImageMimeType: coverImageMimeType,
            title: title,
            author: author,
            description: description,
            pubdate: pubdate
        )
    }

    private static func normalize(_ s: String?) -> String? {
        guard let s = s?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return nil }
        return s
    }

    private static func mimeType(forExtension ext: String) -> String {
        let lower = ext.lowercased()
        switch lower {
        case "jpg", "jpeg": return "image/jpeg"
        case "png": return "image/png"
        case "gif": return "image/gif"
        case "webp": return "image/webp"
        case "svg": return "image/svg+xml"
        default: return "application/octet-stream"
        }
    }
}
