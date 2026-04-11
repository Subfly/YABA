//
//  AnnotationCreationUIState.swift
//  YABACore
//

import Foundation

public struct AnnotationCreationUIState: Sendable {
    public var bookmarkId: String
    public var readableVersionId: String?
    public var annotationId: String?
    public var annotationType: AnnotationType
    public var colorRole: YabaColor
    public var note: String
    public var quoteText: String?
    public var extrasJson: String?
    public var lastError: String?

    public init(
        bookmarkId: String = "",
        readableVersionId: String? = nil,
        annotationId: String? = nil,
        annotationType: AnnotationType = .readable,
        colorRole: YabaColor = .none,
        note: String = "",
        quoteText: String? = nil,
        extrasJson: String? = nil,
        lastError: String? = nil
    ) {
        self.bookmarkId = bookmarkId
        self.readableVersionId = readableVersionId
        self.annotationId = annotationId
        self.annotationType = annotationType
        self.colorRole = colorRole
        self.note = note
        self.quoteText = quoteText
        self.extrasJson = extrasJson
        self.lastError = lastError
    }

    public var isEditing: Bool { annotationId != nil }
}
