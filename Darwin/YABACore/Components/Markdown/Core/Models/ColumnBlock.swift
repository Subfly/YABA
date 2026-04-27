//
//  ColumnBlock.swift
//  YABACore
//

import Foundation

public struct ColumnBlock: Sendable, Equatable {
    public var widthPercent: String? // e.g. "50%"
    public var children: [MarkdownRenderBlock]
    public init(widthPercent: String?, children: [MarkdownRenderBlock]) {
        self.widthPercent = widthPercent
        self.children = children
    }
}
