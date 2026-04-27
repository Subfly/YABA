//
//  TabItemBlock.swift
//  YABACore
//

import Foundation

public struct TabItemBlock: Sendable, Equatable {
    public var title: String
    public var children: [MarkdownRenderBlock]
    public init(title: String, children: [MarkdownRenderBlock]) {
        self.title = title
        self.children = children
    }
}
