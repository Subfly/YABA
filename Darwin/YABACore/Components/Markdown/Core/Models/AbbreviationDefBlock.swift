//
//  AbbreviationDefBlock.swift
//  YABACore
//

import Foundation

public struct AbbreviationDefBlock: Sendable, Equatable {
    public var abbreviation: String
    public var fullText: String
    public init(abbreviation: String, fullText: String) {
        self.abbreviation = abbreviation
        self.fullText = fullText
    }
}
