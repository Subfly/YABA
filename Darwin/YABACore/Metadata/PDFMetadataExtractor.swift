//
//  PDFMetadataExtractor.swift
//  YABACore
//
//  Native PDF metadata and first-page preview via PDFKit.
//

import Foundation
import PDFKit

#if canImport(UIKit)
import UIKit
#elseif canImport(AppKit)
import AppKit
#endif

public enum PDFMetadataExtractor {
    public static func extract(fromFile url: URL, renderScale: CGFloat = 1.2) -> PdfMetadataResult? {
        guard let document = PDFDocument(url: url) else { return nil }
        return extract(document: document, renderScale: renderScale)
    }

    public static func extract(from data: Data, renderScale: CGFloat = 1.2) -> PdfMetadataResult? {
        guard let document = PDFDocument(data: data) else { return nil }
        return extract(document: document, renderScale: renderScale)
    }

    public static func extract(document: PDFDocument, renderScale: CGFloat = 1.2) -> PdfMetadataResult {
        let attrs = document.documentAttributes ?? [:]

        let title = pdfAttr(attrs, PDFDocumentAttribute.titleAttribute.rawValue, "Title")
        let author = pdfAttr(attrs, PDFDocumentAttribute.authorAttribute.rawValue, "Author")
            ?? pdfAttr(attrs, PDFDocumentAttribute.creatorAttribute.rawValue, "Creator")
            ?? pdfAttr(attrs, PDFDocumentAttribute.producerAttribute.rawValue, "Producer")
        let subject = pdfAttr(attrs, PDFDocumentAttribute.subjectAttribute.rawValue, "Subject")
        let creationDate = isoMetadataString(
            attrs[PDFDocumentAttribute.creationDateAttribute.rawValue] ?? attrs["CreationDate"]
        )

        let pageCount = document.pageCount
        let firstPageImageData = firstPagePngData(document: document, renderScale: renderScale)

        return PdfMetadataResult(
            title: title,
            author: author,
            subject: subject,
            creationDate: creationDate,
            pageCount: pageCount,
            firstPageImageData: firstPageImageData
        )
    }

    private static func pdfAttr(_ attrs: [AnyHashable: Any], _ keys: String...) -> String? {
        for key in keys {
            if let v = normalizeString(attrs[key]) { return v }
        }
        return nil
    }

    private static func normalizeString(_ any: Any?) -> String? {
        guard let any else { return nil }
        if let s = any as? String {
            let t = s.trimmingCharacters(in: .whitespacesAndNewlines)
            return t.isEmpty ? nil : t
        }
        return nil
    }

    private static func isoMetadataString(_ any: Any?) -> String? {
        guard let any else { return nil }
        if let d = any as? Date {
            return ISO8601DateFormatter().string(from: d)
        }
        return normalizeString(any)
    }

    private static func firstPagePngData(document: PDFDocument, renderScale: CGFloat) -> Data? {
        guard let page = document.page(at: 0) else { return nil }
        let bounds = page.bounds(for: .mediaBox)
        let w = max(1, bounds.width * renderScale)
        let h = max(1, bounds.height * renderScale)
        let size = CGSize(width: w, height: h)

        #if canImport(UIKit)
        let image = page.thumbnail(of: size, for: .mediaBox)
        return image.pngData()
        #elseif canImport(AppKit)
        let image = page.thumbnail(of: size, for: .mediaBox)
        guard let tiff = image.tiffRepresentation,
              let rep = NSBitmapImageRep(data: tiff),
              let data = rep.representation(using: .png, properties: [:])
        else {
            return nil
        }
        return data
        #else
        return nil
        #endif
    }
}
