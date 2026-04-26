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

    public init(bookmarkId: String, type: AnnotationType) {
        self.bookmarkId = bookmarkId
        self.type = type
    }

    public static func readable(bookmarkId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(bookmarkId: bookmarkId, type: .readable)
    }

    public static func pdf(bookmarkId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(bookmarkId: bookmarkId, type: .pdf)
    }

    public static func epub(bookmarkId: String) -> AnnotationSourceContext {
        AnnotationSourceContext(bookmarkId: bookmarkId, type: .epub)
    }
}
