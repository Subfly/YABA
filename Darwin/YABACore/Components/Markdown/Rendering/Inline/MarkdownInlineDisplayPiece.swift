//
//  MarkdownInlineDisplayPiece.swift
//  YABACore
//
//  Splitting inline runs into text segments vs top-level images.
//

import Foundation

// MARK: - Paragraph pieces (text vs image at top level)

enum InlineDisplayPiece {
    case text(InlineContent)
    case image(alt: InlineContent, url: String, title: String?, width: Int?, height: Int?, attributes: KMPStringDict)
}

func splitInlineForDisplay(_ content: InlineContent) -> [InlineDisplayPiece] {
    var out: [InlineDisplayPiece] = []
    var buf: [InlineRun] = []
    for r in content.runs {
        if case let .image(alt, url, title, w, h, attr) = r {
            if !buf.isEmpty {
                out.append(.text(InlineContent(runs: buf)))
                buf = []
            }
            out.append(.image(alt: alt, url: url, title: title, width: w, height: h, attributes: attr))
        } else {
            buf.append(r)
        }
    }
    if !buf.isEmpty {
        out.append(.text(InlineContent(runs: buf)))
    }
    if out.isEmpty, content.runs.isEmpty { return [] }
    if out.isEmpty { return [.text(content)] }
    return out
}
