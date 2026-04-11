//
//  YabaAnnotationReadableCreateRequest.swift
//  YABACore
//
//  Parity with Compose `AnnotationReadableCreateRequest`.
//

import Foundation

public struct YabaAnnotationReadableCreateRequest: Sendable, Equatable {
    public var selectionDraft: YabaReadableSelectionDraft
    public var colorRole: YabaCoreColorRole
    public var note: String?

    public init(
        selectionDraft: YabaReadableSelectionDraft,
        colorRole: YabaCoreColorRole,
        note: String?
    ) {
        self.selectionDraft = selectionDraft
        self.colorRole = colorRole
        self.note = note
    }
}
