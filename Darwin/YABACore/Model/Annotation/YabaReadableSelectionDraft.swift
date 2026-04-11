//
//  YabaReadableSelectionDraft.swift
//  YABACore
//
//  Contract-only draft passed from web/PDF/EPUB hosts for annotation creation.
//

import Foundation

public struct YabaReadableSelectionDraft: Sendable {
    public var bookmarkId: String
    public var readableVersionId: String
    public var quoteText: String?
    /// Optional JSON for anchors (PDF offsets, EPUB CFI, etc.).
    public var extrasJson: String?

    public init(
        bookmarkId: String,
        readableVersionId: String,
        quoteText: String? = nil,
        extrasJson: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.readableVersionId = readableVersionId
        self.quoteText = quoteText
        self.extrasJson = extrasJson
    }
}
