//
//  MarkdownRenderDocument.swift
//  YABACore
//
//  Top-level render document and block kind enum.
//

import Foundation

// MARK: - Document

public struct MarkdownRenderDocument: Sendable, Equatable {
    public var blocks: [MarkdownRenderBlock]
    public init(blocks: [MarkdownRenderBlock]) {
        self.blocks = blocks
    }
}

// MARK: - Top-level block

public struct MarkdownRenderBlock: Identifiable, Sendable, Equatable {
    public var id: String
    public var kind: MarkdownRenderBlockKind
    public init(id: String, kind: MarkdownRenderBlockKind) {
        self.id = id
        self.kind = kind
    }
}

public enum MarkdownRenderBlockKind: Sendable, Equatable {
    case documentRoot(children: [MarkdownRenderBlock]) // usually flattened; use for debugging
    case heading(HeadingBlock)
    case setextHeading(HeadingBlock)
    case paragraph(ParagraphBlock)
    case thematicBreak
    case fencedCode(FencedCodeBlockModel)
    case indentedCode(String)
    case blockQuote(children: [MarkdownRenderBlock])
    case list(ListBlockModel)
    case htmlBlock(String)
    case linkReferenceDefinition(LinkRefDefBlock)
    case table(TableBlock)
    case footnoteDefinition(FootnoteDefBlock)
    case mathBlock(String)
    case definitionList(items: [DefinitionListItemBlock])
    case admonition(AdmonitionBlock)
    case frontMatter(String, format: String)
    case toc(TocBlock)
    case abbreviationDefinition(AbbreviationDefBlock)
    case customContainer(CustomContainerBlock)
    case diagram(DiagramBlockModel)
    case columns([ColumnBlock])
    case pageBreak
    case directiveBlock(DirectiveBlockModel)
    case tabBlock([TabItemBlock])
    case bibliography([BibEntryBlock])
    case figure(FigureBlock)
    case metadataOmitted(MarkdownMetadataKind) // link ref, abbr, front-matter: hidden in normal preview
    case unsupportedNode(name: String, detail: String?)
}

public enum MarkdownMetadataKind: Sendable, Equatable {
    case linkReferenceDefinition
    case abbreviationDefinition
    case frontMatter
}
