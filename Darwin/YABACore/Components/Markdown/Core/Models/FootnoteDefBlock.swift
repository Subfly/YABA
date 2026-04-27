//
//  FootnoteDefBlock.swift
//  YABACore
//

import Foundation

public struct FootnoteDefBlock: Sendable, Equatable {
    public var label: String
    public var index: Int
    public var children: [MarkdownRenderBlock]
    public init(label: String, index: Int, children: [MarkdownRenderBlock]) {
        self.label = label
        self.index = index
        self.children = children
    }
}
