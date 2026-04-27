//
//  CustomContainerBlock.swift
//  YABACore
//

import Foundation

public struct CustomContainerBlock: Sendable, Equatable {
    public var type: String
    public var title: String
    public var cssClasses: [String]
    public var cssId: String?
    public var children: [MarkdownRenderBlock]
    public init(type: String, title: String, cssClasses: [String], cssId: String?, children: [MarkdownRenderBlock]) {
        self.type = type
        self.title = title
        self.cssClasses = cssClasses
        self.cssId = cssId
        self.children = children
    }
}
