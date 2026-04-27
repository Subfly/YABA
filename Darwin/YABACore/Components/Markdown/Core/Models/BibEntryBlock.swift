//
//  BibEntryBlock.swift
//  YABACore
//

import Foundation

public struct BibEntryBlock: Sendable, Equatable {
    public var key: String
    public var content: String
    public init(key: String, content: String) {
        self.key = key
        self.content = content
    }
}
