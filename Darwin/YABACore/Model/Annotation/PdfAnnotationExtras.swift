//
//  PdfAnnotationExtras.swift
//  YABACore
//
//  Parity with Compose `PdfAnnotationExtras`.
//

import Foundation

public struct PdfAnnotationExtras: Sendable, Codable, Equatable {
    public var startSectionKey: String
    public var startOffsetInSection: Int
    public var endSectionKey: String
    public var endOffsetInSection: Int

    public init(
        startSectionKey: String,
        startOffsetInSection: Int,
        endSectionKey: String,
        endOffsetInSection: Int
    ) {
        self.startSectionKey = startSectionKey
        self.startOffsetInSection = startOffsetInSection
        self.endSectionKey = endSectionKey
        self.endOffsetInSection = endOffsetInSection
    }
}
