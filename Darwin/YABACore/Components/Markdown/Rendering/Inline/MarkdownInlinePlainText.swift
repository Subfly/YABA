//
//  MarkdownInlinePlainText.swift
//  YABACore
//
//  Plain-text extraction from `InlineContent` (TOC, search, etc.).
//

import Foundation

public enum MarkdownInlinePlainText {
    public static func plainText(from content: InlineContent) -> String {
        var s = ""
        for r in content.runs {
            s += runText(r)
        }
        return s
    }

    private static func runText(_ run: InlineRun) -> String {
        switch run {
        case .text(let t): return t
        case .lineBreak(let soft): return soft ? " " : "\n"
        case .emphasis(let c), .strong(let c), .strikethrough(let c), .highlight(let c),
            .inserted(let c), .superscript(let c), .subscripted(let c), .spoiler(let c):
            return plainText(from: c)
        case .code(let t): return t
        case .link(let c, _, _): return plainText(from: c)
        case .autolink(_, _, let d): return d
        case .image(let alt, _, _, _, _, _): return plainText(from: alt)
        case .footnoteRef: return ""
        case .mathInline(let t): return t
        case .emoji(_, let u): return u ?? ""
        case .styled(let c, _): return plainText(from: c)
        case .abbreviation(let a, _): return a
        case .kbd(let t): return t
        case .citation(let k): return k
        case .wikiLink(_, let l): return l ?? ""
        case .ruby(let b, _): return b
        case .htmlInline(let h): return h
        case .directiveInline(let tag, _): return tag
        }
    }
}
