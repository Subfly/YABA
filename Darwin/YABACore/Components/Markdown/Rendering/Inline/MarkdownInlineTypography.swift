//
//  MarkdownInlineTypography.swift
//  YABACore
//

import Foundation

/// Maps block-level typography (formerly reflected via `.font(...)` on `MarkdownInlineBlockView`).
public enum MarkdownInlineTypography: Equatable, Sendable {
    case body
    case heading(level: Int)
    case definitionTerm
    case tableCell
}
