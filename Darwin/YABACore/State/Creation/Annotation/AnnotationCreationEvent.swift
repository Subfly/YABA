//
//  AnnotationCreationEvent.swift
//  YABACore
//
//  Parity with Compose `AnnotationCreationEvent`.
//

import Foundation

public enum AnnotationCreationEvent: Sendable {
    case onInitWithSelection(YabaReadableSelectionDraft)
    case onInitWithAnnotation(bookmarkId: String, annotationId: String)
    case onSelectNewColor(YabaCoreColorRole)
    case onChangeNote(String)
    case onChangeQuote(String?)
    case onSave
    case onDelete

    /// Direct insert (legacy one-shot).
    case create(
        bookmarkId: String,
        readableVersionId: String?,
        type: YabaCoreAnnotationType,
        note: String?,
        quote: String?
    )
}
