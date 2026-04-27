//
//  MarkdownTocContext.swift
//  YABACore
//
//  Heading catalog for `TocPlaceholder` resolution (walks the KMP `Document` tree).
//

import Foundation
import MarkdownParser

public struct MarkdownTocHeading: Sendable, Equatable {
    public var headingId: String?
    public var level: Int
    public var title: String
    public init(headingId: String?, level: Int, title: String) {
        self.headingId = headingId
        self.level = level
        self.title = title
    }
}

public enum MarkdownTocSupport {
    /// Pre-order walk collecting ATX and Setext headings.
    public static func collectHeadings(from document: Document) -> [MarkdownTocHeading] {
        var out: [MarkdownTocHeading] = []
        for child in document.children {
            walk(node: child, into: &out)
        }
        return out
    }

    private static func walk(node: Node, into out: inout [MarkdownTocHeading]) {
        if let h = node as? Heading {
            let t = plainTitle(from: h)
            out.append(MarkdownTocHeading(headingId: h.id, level: Int(h.level), title: t))
        } else if let h = node as? SetextHeading {
            let t = plainTitle(from: h)
            out.append(MarkdownTocHeading(headingId: h.id, level: Int(h.level), title: t))
        }
        if let c = node as? ContainerNode {
            for ch in c.children { walk(node: ch, into: &out) }
        }
    }

    private static func plainTitle(from container: ContainerNode) -> String {
        let content = InlineAssembler.build(from: container)
        return MarkdownInlinePlainText.plainText(from: content)
    }
}
