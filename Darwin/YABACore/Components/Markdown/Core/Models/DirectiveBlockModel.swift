//
//  DirectiveBlockModel.swift
//  YABACore
//

import Foundation

public struct DirectiveBlockModel: Sendable, Equatable {
    public var tag: String
    public var args: KMPStringDict
    public var children: [MarkdownRenderBlock]
    public init(tag: String, args: KMPStringDict, children: [MarkdownRenderBlock]) {
        self.tag = tag
        self.args = args
        self.children = children
    }
}
