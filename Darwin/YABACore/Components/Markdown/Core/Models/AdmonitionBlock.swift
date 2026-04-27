//
//  AdmonitionBlock.swift
//  YABACore
//

import Foundation

public struct AdmonitionBlock: Sendable, Equatable {
    public var type: String
    public var title: String
    public var children: [MarkdownRenderBlock]
    public init(type: String, title: String, children: [MarkdownRenderBlock]) {
        self.type = type
        self.title = title
        self.children = children
    }
}
