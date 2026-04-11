//
//  YabaEpubAnnotationExtras.swift
//  YABACore
//
//  Parity with Compose `EpubAnnotationExtras`.
//

import Foundation

public struct YabaEpubAnnotationExtras: Sendable, Codable, Equatable {
    /// epub.js CFI range for the highlight (inclusive).
    public var cfiRange: String

    public init(cfiRange: String) {
        self.cfiRange = cfiRange
    }
}
