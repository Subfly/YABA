//
//  MarkdownThemeTokens.swift
//  YABACore
//
//  Default styling for native Markdown components.
//

import SwiftUI

public struct MarkdownThemeTokens: Sendable {
    public var largeHeading: Font
    public var body: Font
    public var monospaced: Font
    public var linkColor: Color
    public var codeBackground: Color
    public var blockQuoteColor: Color
    public var tableBorder: Color
    public var tableHeader: Color
    public var mathBackground: Color
    public var diagramFallbackBackground: Color

    public static func standard(colorScheme: ColorScheme) -> MarkdownThemeTokens {
        let isDark = colorScheme == .dark
        return MarkdownThemeTokens(
            largeHeading: .title,
            body: .body,
            monospaced: .body.monospaced(),
            linkColor: isDark ? Color(red: 0.35, green: 0.6, blue: 1) : Color(red: 0.04, green: 0.4, blue: 0.85),
            codeBackground: isDark ? Color(white: 0.2) : Color(white: 0.95),
            blockQuoteColor: isDark ? Color(white: 0.65) : Color(white: 0.4),
            tableBorder: isDark ? Color(white: 0.3) : Color(white: 0.8),
            tableHeader: isDark ? Color(white: 0.15) : Color(white: 0.96),
            mathBackground: isDark ? Color(white: 0.12) : Color(white: 0.96),
            diagramFallbackBackground: isDark ? Color(white: 0.14) : Color(white: 0.94)
        )
    }
}
