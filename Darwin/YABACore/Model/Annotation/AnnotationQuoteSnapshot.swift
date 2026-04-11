//
//  AnnotationQuoteSnapshot.swift
//  YABACore
//
//  Parity with Compose `AnnotationQuoteSnapshot`.
//

import Foundation

public struct AnnotationQuoteSnapshot: Sendable, Codable, Equatable {
    public var selectedText: String
    public var prefixText: String?
    public var suffixText: String?

    public init(selectedText: String, prefixText: String? = nil, suffixText: String? = nil) {
        self.selectedText = selectedText
        self.prefixText = prefixText
        self.suffixText = suffixText
    }

    public var displayText: String {
        selectedText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    public static func fromSelectedText(_ text: String) -> AnnotationQuoteSnapshot {
        AnnotationQuoteSnapshot(
            selectedText: text.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }
}
