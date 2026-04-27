//
//  MarkdownRenderModels.swift
//  YABACore
//
//  UI-facing models produced from the KMP AST (independent of Kotlin node classes).
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

// MARK: - Block payloads

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

public struct ParagraphBlock: Sendable, Equatable {
    public var inline: InlineContent
    public var blockAttributes: KMPStringDict
    public init(inline: InlineContent, blockAttributes: KMPStringDict = [:]) {
        self.inline = inline
        self.blockAttributes = blockAttributes
    }
}

/// Render payload for a fenced code block (distinct from `MarkdownParser.FencedCodeBlock` AST type).
public struct FencedCodeBlockModel: Sendable, Equatable {
    public var language: String
    public var info: String
    public var code: String
    public var title: String?
    public var showLineNumbers: Bool
    public var startLine: Int
    public var highlightedLines: [ClosedRange<Int>]
    public init(
        language: String,
        info: String,
        code: String,
        title: String? = nil,
        showLineNumbers: Bool = true,
        startLine: Int = 1,
        highlightedLines: [ClosedRange<Int>] = []
    ) {
        self.language = language
        self.info = info
        self.code = code
        self.title = title
        self.showLineNumbers = showLineNumbers
        self.startLine = startLine
        self.highlightedLines = highlightedLines
    }
}

public struct ListBlockModel: Sendable, Equatable {
    public var ordered: Bool
    public var startNumber: Int
    public var bullet: Character
    public var delimiter: Character
    public var tight: Bool
    public var items: [ListItemBlock]
    public var blockAttributes: KMPStringDict
    public init(
        ordered: Bool,
        startNumber: Int,
        bullet: Character,
        delimiter: Character,
        tight: Bool,
        items: [ListItemBlock],
        blockAttributes: KMPStringDict = [:]
    ) {
        self.ordered = ordered
        self.startNumber = startNumber
        self.bullet = bullet
        self.delimiter = delimiter
        self.tight = tight
        self.items = items
        self.blockAttributes = blockAttributes
    }
}

public struct ListItemBlock: Sendable, Equatable {
    public var isTask: Bool
    public var checked: Bool
    public var children: [MarkdownRenderBlock]
    public init(isTask: Bool, checked: Bool, children: [MarkdownRenderBlock]) {
        self.isTask = isTask
        self.checked = checked
        self.children = children
    }
}

public struct TableBlock: Sendable, Equatable {
    public var columnAlignment: [ColumnAlignment]
    /// First row from `TableHead`, if present.
    public var headerRow: [InlineContent]?
    /// Rows from `TableBody` (or non-head rows if head empty).
    public var bodyRows: [[InlineContent]]
    public init(columnAlignment: [ColumnAlignment], headerRow: [InlineContent]?, bodyRows: [[InlineContent]]) {
        self.columnAlignment = columnAlignment
        self.headerRow = headerRow
        self.bodyRows = bodyRows
    }

    public enum ColumnAlignment: Sendable, Equatable {
        case left, center, right, none
    }
}

public struct LinkRefDefBlock: Sendable, Equatable {
    public var label: String
    public var destination: String
    public var title: String?
    public init(label: String, destination: String, title: String?) {
        self.label = label
        self.destination = destination
        self.title = title
    }
}

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

public struct DefinitionListItemBlock: Sendable, Equatable {
    public var term: InlineContent
    public var definitions: [MarkdownRenderBlock] // block children inside <dd>
    public init(term: InlineContent, definitions: [MarkdownRenderBlock]) {
        self.term = term
        self.definitions = definitions
    }
}

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

public struct TocBlock: Sendable, Equatable {
    public var minDepth: Int
    public var maxDepth: Int
    public var excludeIds: [String]
    public var order: String
    /// Pre-resolved entries from the document (optional; may be empty if not computed).
    public var resolvedEntries: [TocEntry]
    public init(
        minDepth: Int,
        maxDepth: Int,
        excludeIds: [String],
        order: String,
        resolvedEntries: [TocEntry] = []
    ) {
        self.minDepth = minDepth
        self.maxDepth = maxDepth
        self.excludeIds = excludeIds
        self.order = order
        self.resolvedEntries = resolvedEntries
    }
}

public struct TocEntry: Sendable, Equatable, Identifiable {
    public var id: String
    public var level: Int
    public var title: String
    public var headingId: String?
    public init(level: Int, title: String, headingId: String?) {
        self.level = level
        self.title = title
        self.headingId = headingId
        self.id = headingId ?? "\(level)-\(title.hashValue)"
    }
}

public struct AbbreviationDefBlock: Sendable, Equatable {
    public var abbreviation: String
    public var fullText: String
    public init(abbreviation: String, fullText: String) {
        self.abbreviation = abbreviation
        self.fullText = fullText
    }
}

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

public struct DiagramBlockModel: Sendable, Equatable {
    public var type: String
    public var source: String
    public init(type: String, source: String) {
        self.type = type
        self.source = source
    }
}

public struct ColumnBlock: Sendable, Equatable {
    public var widthPercent: String? // e.g. "50%"
    public var children: [MarkdownRenderBlock]
    public init(widthPercent: String?, children: [MarkdownRenderBlock]) {
        self.widthPercent = widthPercent
        self.children = children
    }
}

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

public struct TabItemBlock: Sendable, Equatable {
    public var title: String
    public var children: [MarkdownRenderBlock]
    public init(title: String, children: [MarkdownRenderBlock]) {
        self.title = title
        self.children = children
    }
}

public struct BibEntryBlock: Sendable, Equatable {
    public var key: String
    public var content: String
    public init(key: String, content: String) {
        self.key = key
        self.content = content
    }
}

public struct FigureBlock: Sendable, Equatable {
    public var imageURL: String
    public var caption: String
    public var width: Int?
    public var height: Int?
    public var attributes: KMPStringDict
    public init(imageURL: String, caption: String, width: Int?, height: Int?, attributes: KMPStringDict) {
        self.imageURL = imageURL
        self.caption = caption
        self.width = width
        self.height = height
        self.attributes = attributes
    }
}

// MARK: - Inline

public struct InlineContent: Sendable, Equatable {
    public var runs: [InlineRun]
    public init(runs: [InlineRun]) {
        self.runs = runs
    }

    public static let empty = InlineContent(runs: [])
}

public enum InlineRun: Sendable, Equatable {
    case text(String)
    case emphasis(InlineContent) // recursive already expanded to runs in builder — keep nested for styling
    case strong(InlineContent)
    case strikethrough(InlineContent)
    case code(String)
    case link(InlineContent, url: String, title: String?)
    case autolink(url: String, isEmail: Bool, display: String)
    case image(alt: InlineContent, url: String, title: String?, width: Int?, height: Int?, attributes: KMPStringDict)
    case footnoteRef(label: String, index: Int)
    case mathInline(String)
    case highlight(InlineContent)
    case superscript(InlineContent)
    /// Subscript (KMP: `Subscript`); not named `subscript` — reserved in Swift.
    case subscripted(InlineContent)
    case inserted(InlineContent)
    case emoji(shortcode: String, unicode: String?)
    case styled(InlineContent, attributes: KMPStringDict) // [text]{.class}
    case abbreviation(short: String, full: String)
    case kbd(String)
    case citation(key: String)
    case spoiler(InlineContent)
    case wikiLink(target: String, label: String?)
    case ruby(base: String, annotation: String)
    case lineBreak(soft: Bool) // true = soft (space), false = hard
    case htmlInline(String)
    case directiveInline(String, args: KMPStringDict) // tag + args, display fallback
}
