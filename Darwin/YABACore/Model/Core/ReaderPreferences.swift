//
//  ReaderPreferences.swift
//  YABACore
//
//  Parity with Compose `ReaderTheme` / `ReaderFontSize` / `ReaderLineHeight` / `ReaderPreferences`.
//

import Foundation

public enum ReaderTheme: String, Sendable, CaseIterable {
    case system
    case dark
    case light
    case sepia
}

public enum ReaderFontSize: String, Sendable, CaseIterable {
    case small
    case medium
    case large
}

public enum ReaderLineHeight: String, Sendable, CaseIterable {
    case normal
    case relaxed
}

public struct ReaderPreferences: Sendable, Equatable {
    public var theme: ReaderTheme
    public var fontSize: ReaderFontSize
    public var lineHeight: ReaderLineHeight

    public init(
        theme: ReaderTheme = .system,
        fontSize: ReaderFontSize = .medium,
        lineHeight: ReaderLineHeight = .normal
    ) {
        self.theme = theme
        self.fontSize = fontSize
        self.lineHeight = lineHeight
    }
}
