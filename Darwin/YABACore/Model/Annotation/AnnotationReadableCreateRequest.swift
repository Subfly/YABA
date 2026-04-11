//
//  AnnotationReadableCreateRequest.swift
//  YABACore
//
//  Parity with Compose `AnnotationReadableCreateRequest`.
//

import Foundation

public struct AnnotationReadableCreateRequest: Sendable, Equatable {
    public var selectionDraft: ReadableSelectionDraft
    public var colorRole: YabaColor
    public var note: String?

    public init(
        selectionDraft: ReadableSelectionDraft,
        colorRole: YabaColor,
        note: String?
    ) {
        self.selectionDraft = selectionDraft
        self.colorRole = colorRole
        self.note = note
    }
}
