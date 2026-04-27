//
//  MarkdownInlineContent.swift
//  YABACore
//
//  Inline text/image runs used by block models and `MarkdownASTConverter`.
//

import Foundation

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
