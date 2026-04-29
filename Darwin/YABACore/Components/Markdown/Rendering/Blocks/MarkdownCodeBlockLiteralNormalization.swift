//
//  MarkdownCodeBlockLiteralNormalization.swift
//  YABACore
//

import Foundation

/// Fenced/indented code literals often end with a trailing newline from the parser, which shows up as an extra blank line when split or in `Text`.
enum MarkdownCodeBlockLiteralNormalization {
    static func forDisplay(_ literal: String) -> String {
        guard literal.hasSuffix("\n") else { return literal }
        return String(literal.dropLast())
    }
}
