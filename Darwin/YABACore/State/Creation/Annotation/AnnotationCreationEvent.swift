//
//  AnnotationCreationEvent.swift
//  YABACore
//
//  Parity with Compose `AnnotationCreationEvent`.
//

import Foundation

public enum AnnotationCreationEvent: Sendable {
    case onInitWithSelection(ReadableSelectionDraft)
    case onInitWithAnnotation(bookmarkId: String, annotationId: String)
    case onSelectNewColor(YabaColor)
    case onChangeNote(String)
    case onChangeQuote(String?)
    case onSave
    case onDelete

    /// Direct insert (legacy one-shot).
    case create(
        bookmarkId: String,
        type: AnnotationType,
        note: String?,
        quote: String?
    )
}
