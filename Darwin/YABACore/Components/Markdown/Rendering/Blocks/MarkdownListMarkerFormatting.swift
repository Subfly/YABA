//
//  MarkdownListMarkerFormatting.swift
//  YABACore
//

import Foundation

/// Presentation for list markers (source may use `*`, `-`, `+`; UI uses a bullet glyph).
enum MarkdownListMarkerFormatting {
    /// Standard bullet (U+2022 •); reads lighter than U+25CF ● at typical body font metrics.
    static func unorderedGlyph(forSourceBullet bullet: Character) -> String {
        switch bullet {
        case "*", "-", "+":
            return "\u{2022}"
        default:
            return String(bullet)
        }
    }
}
