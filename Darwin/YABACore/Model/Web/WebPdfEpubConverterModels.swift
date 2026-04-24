//
//  WebPdfEpubConverterModels.swift
//  YABACore
//
//  Parity with Compose `WebPdfConverterModels.kt` / `WebEpubConverterModels.kt`
//  and JSON parsing in `YabaConverterJobBridge.parsePdfOutput` / `parseEpubOutput`.
//

import Foundation

public struct WebPdfTextSection: Sendable, Equatable {
    public var sectionKey: String
    public var text: String

    public init(sectionKey: String, text: String) {
        self.sectionKey = sectionKey
        self.text = text
    }
}

public struct WebPdfConverterResult: Sendable, Equatable {
    public var title: String?
    public var author: String?
    public var subject: String?
    public var creationDate: String?
    public var pageCount: Int
    public var firstPagePngDataUrl: String?
    public var sections: [WebPdfTextSection]

    public init(
        title: String?,
        author: String?,
        subject: String?,
        creationDate: String?,
        pageCount: Int,
        firstPagePngDataUrl: String?,
        sections: [WebPdfTextSection]
    ) {
        self.title = title
        self.author = author
        self.subject = subject
        self.creationDate = creationDate
        self.pageCount = pageCount
        self.firstPagePngDataUrl = firstPagePngDataUrl
        self.sections = sections
    }
}

public struct WebEpubConverterResult: Sendable, Equatable {
    public var coverPngDataUrl: String?
    public var title: String?
    public var author: String?
    public var description: String?
    public var pubdate: String?

    public init(
        coverPngDataUrl: String?,
        title: String?,
        author: String?,
        description: String?,
        pubdate: String?
    ) {
        self.coverPngDataUrl = coverPngDataUrl
        self.title = title
        self.author = author
        self.description = description
        self.pubdate = pubdate
    }
}

// MARK: - Parsing (Compose `YabaConverterJobBridge`)

public enum ConverterJobOutputParsing {
    private static func normalizeOptionalString(_ s: String?) -> String? {
        guard let s = s?.trimmingCharacters(in: .whitespacesAndNewlines), !s.isEmpty else { return nil }
        return s
    }

    public static func parsePdfOutput(_ outputJson: String) -> WebPdfConverterResult? {
        guard let data = outputJson.data(using: .utf8),
              let output = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }
        var sections: [WebPdfTextSection] = []
        if let sectionsJson = output["sections"] as? [[String: Any]] {
            for section in sectionsJson {
                let key = section["sectionKey"] as? String ?? ""
                let text = section["text"] as? String ?? ""
                sections.append(WebPdfTextSection(sectionKey: key, text: text))
            }
        }
        let pageCount: Int = {
            if let i = output["pageCount"] as? Int { return i }
            if let d = output["pageCount"] as? Double { return Int(d) }
            if let n = output["pageCount"] as? NSNumber { return n.intValue }
            return 0
        }()
        return WebPdfConverterResult(
            title: normalizeOptionalString(output["title"] as? String),
            author: normalizeOptionalString(output["author"] as? String),
            subject: normalizeOptionalString(output["subject"] as? String),
            creationDate: normalizeOptionalString(output["creationDate"] as? String),
            pageCount: pageCount,
            firstPagePngDataUrl: normalizeOptionalString(output["firstPagePngDataUrl"] as? String),
            sections: sections
        )
    }

    public static func parseEpubOutput(_ outputJson: String) -> WebEpubConverterResult? {
        guard let data = outputJson.data(using: .utf8),
              let output = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return nil
        }
        return WebEpubConverterResult(
            coverPngDataUrl: normalizeOptionalString(output["coverPngDataUrl"] as? String),
            title: normalizeOptionalString(output["title"] as? String),
            author: normalizeOptionalString(output["author"] as? String),
            description: normalizeOptionalString(output["description"] as? String),
            pubdate: normalizeOptionalString(output["pubdate"] as? String)
        )
    }
}
