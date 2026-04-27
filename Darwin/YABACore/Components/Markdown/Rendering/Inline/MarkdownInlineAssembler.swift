//
//  MarkdownInlineAssembler.swift
//  YABACore
//
//  Builds `InlineContent` from KMP container / inline nodes.
//

import Foundation
import MarkdownParser

enum InlineAssembler {
    static func build(from container: ContainerNode) -> InlineContent {
        var runs: [InlineRun] = []
        for child in container.children {
            runs.append(contentsOf: inlineRuns(from: child))
        }
        return InlineContent(runs: runs)
    }

    private static func inlineRuns(from node: Node) -> [InlineRun] {
        if let t = node as? Text {
            return [.text(t.literal)]
        }
        if let _ = node as? SoftLineBreak {
            return [.lineBreak(soft: true)]
        }
        if let _ = node as? HardLineBreak {
            return [.lineBreak(soft: false)]
        }
        if let n = node as? Emphasis {
            return [.emphasis(build(from: n))]
        }
        if let n = node as? StrongEmphasis {
            return [.strong(build(from: n))]
        }
        if let n = node as? Strikethrough {
            return [.strikethrough(build(from: n))]
        }
        if let n = node as? InlineCode {
            return [.code(n.literal)]
        }
        if let n = node as? Link {
            let inner = build(from: n)
            return [.link(inner, url: n.destination, title: n.title)]
        }
        if let n = node as? Image {
            let alt = build(from: n)
            return [.image(
                alt: alt,
                url: n.destination,
                title: n.title,
                width: MarkdownKMPInterop.optionalInt32(from: n.imageWidth),
                height: MarkdownKMPInterop.optionalInt32(from: n.imageHeight),
                attributes: MarkdownKMPInterop.stringDict(from: n.attributes)
            )]
        }
        if let n = node as? Autolink {
            let display = n.rawText.isEmpty ? n.destination : n.rawText
            return [.autolink(url: n.destination, isEmail: n.isEmail, display: display)]
        }
        if let n = node as? InlineHtml {
            return [.htmlInline(n.literal)]
        }
        if let n = node as? HtmlEntity {
            let t = n.resolved.isEmpty ? n.literal : n.resolved
            return [.text(t)]
        }
        if let n = node as? EscapedChar {
            return [.text(n.literal)]
        }
        if let n = node as? FootnoteReference {
            return [.footnoteRef(label: n.label, index: Int(n.index))]
        }
        if let n = node as? InlineMath {
            return [.mathInline(n.literal)]
        }
        if let n = node as? Highlight {
            return [.highlight(build(from: n))]
        }
        if let n = node as? Superscript {
            return [.superscript(build(from: n))]
        }
        if let n = node as? Subscript {
            return [.subscripted(build(from: n))]
        }
        if let n = node as? InsertedText {
            return [.inserted(build(from: n))]
        }
        if let n = node as? Emoji {
            let lit = n.literal.isEmpty ? ":\(n.shortcode):" : n.literal
            return [.emoji(shortcode: n.shortcode, unicode: n.unicode)]
        }
        if let n = node as? StyledText {
            let inner = build(from: n)
            return [.styled(inner, attributes: MarkdownKMPInterop.stringDict(from: n.attributes))]
        }
        if let n = node as? Abbreviation {
            return [.abbreviation(short: n.abbreviation, full: n.fullText)]
        }
        if let n = node as? KeyboardInput {
            return [.kbd(n.literal)]
        }
        if let n = node as? CitationReference {
            return [.citation(key: n.key)]
        }
        if let n = node as? Spoiler {
            return [.spoiler(build(from: n))]
        }
        if let n = node as? WikiLink {
            return [.wikiLink(target: n.target, label: n.label)]
        }
        if let n = node as? RubyText {
            return [.ruby(base: n.base, annotation: n.annotation)]
        }
        if let n = node as? DirectiveInline {
            return [.directiveInline(n.tagName, args: MarkdownKMPInterop.stringDict(from: n.args))]
        }
        // Block nodes mistakenly passed: flatten
        if let c = node as? ContainerNode {
            var r: [InlineRun] = []
            for ch in c.children { r.append(contentsOf: inlineRuns(from: ch)) }
            return r
        }
        if let l = node as? LeafNode {
            return [.text(l.literal)]
        }
        return [.text("")]
    }
}
