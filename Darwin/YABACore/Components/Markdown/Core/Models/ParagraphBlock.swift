//
//  ParagraphBlock.swift
//  YABACore
//

import Foundation

public struct ParagraphBlock: Sendable, Equatable {
    public var inline: InlineContent
    public var blockAttributes: KMPStringDict
    public init(inline: InlineContent, blockAttributes: KMPStringDict = [:]) {
        self.inline = inline
        self.blockAttributes = blockAttributes
    }
}
