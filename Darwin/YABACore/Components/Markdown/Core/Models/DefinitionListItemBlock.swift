//
//  DefinitionListItemBlock.swift
//  YABACore
//

import Foundation

public struct DefinitionListItemBlock: Sendable, Equatable {
    public var term: InlineContent
    public var definitions: [MarkdownRenderBlock] // block children inside <dd>
    public init(term: InlineContent, definitions: [MarkdownRenderBlock]) {
        self.term = term
        self.definitions = definitions
    }
}
