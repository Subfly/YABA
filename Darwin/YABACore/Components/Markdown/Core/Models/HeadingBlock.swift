//
//  HeadingBlock.swift
//  YABACore
//

import Foundation

public struct HeadingBlock: Sendable, Equatable {
    public var level: Int
    public var id: String?
    public var inline: InlineContent
    public init(level: Int, id: String?, inline: InlineContent) {
        self.level = level
        self.id = id
        self.inline = inline
    }
}
