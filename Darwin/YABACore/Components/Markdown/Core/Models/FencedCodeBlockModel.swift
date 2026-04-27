//
//  FencedCodeBlockModel.swift
//  YABACore
//

import Foundation

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
