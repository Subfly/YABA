//
//  AnnotationSourceContext.swift
//  YABACore
//
//  Parity with Compose `AnnotationSourceContext`.
//

import Foundation

public struct AnnotationSourceContext: Sendable, Codable, Equatable {
    public var bookmarkId: String
    public var type: AnnotationType
    /// For readable: readableVersionId. For PDF: same id space as stored on the entity.
    public var contentId: String

    public init(bookmarkId: String, type: AnnotationType, contentId: String) {
        self.bookmarkId = bookmarkId
        self.type = type
        self.contentId = contentId
    }

    public static func readable(bookmarkId: String, readableVersionId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(
            bookmarkId: bookmarkId,
            type: .readable,
            contentId: readableVersionId
        )
    }

    public static func pdf(bookmarkId: String, readableVersionId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(
            bookmarkId: bookmarkId,
            type: .pdf,
            contentId: readableVersionId
        )
    }

    public static func epub(bookmarkId: String, readableVersionId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(
            bookmarkId: bookmarkId,
            type: .epub,
            contentId: readableVersionId
        )
    }
}
