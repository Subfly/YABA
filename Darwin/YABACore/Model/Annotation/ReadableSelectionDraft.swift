//
//  ReadableSelectionDraft.swift
//  YABACore
//
//  Contract-only draft passed from web/PDF/EPUB hosts for annotation creation.
//

import Foundation

public struct ReadableSelectionDraft: Sendable, Equatable {
    public var bookmarkId: String
    public var readableVersionId: String
    public var quoteText: String?
    /// Optional JSON for anchors (PDF offsets, EPUB CFI, etc.).
    public var extrasJson: String?
    /// Source annotation surface (`READABLE`, `PDF`, `EPUB`).
    public var annotationType: AnnotationType

    public init(
        bookmarkId: String,
        readableVersionId: String,
        quoteText: String? = nil,
        extrasJson: String? = nil,
        annotationType: AnnotationType = .readable
    ) {
        self.bookmarkId = bookmarkId
        self.readableVersionId = readableVersionId
        self.quoteText = quoteText
        self.extrasJson = extrasJson
        self.annotationType = annotationType
    }

    public var sourceContext: AnnotationSourceContext {
        switch annotationType {
        case .readable:
            return .readable(bookmarkId: bookmarkId, readableVersionId: readableVersionId)
        case .pdf:
            return .pdf(bookmarkId: bookmarkId, readableVersionId: readableVersionId)
        case .epub:
            return .epub(bookmarkId: bookmarkId, readableVersionId: readableVersionId)
        }
    }

    public var quote: AnnotationQuoteSnapshot {
        AnnotationQuoteSnapshot.fromSelectedText(quoteText ?? "")
    }
}
