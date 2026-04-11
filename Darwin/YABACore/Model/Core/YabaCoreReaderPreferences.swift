//
//  YabaCoreReaderPreferences.swift
//  YABACore
//
//  Parity with Compose `ReaderTheme` / `ReaderFontSize` / `ReaderLineHeight` / `ReaderPreferences`.
//

import Foundation

public enum YabaCoreReaderTheme: String, Sendable, CaseIterable {
    case system
    case dark
    case light
    case sepia
}

public enum YabaCoreReaderFontSize: String, Sendable, CaseIterable {
    case small
    case medium
    case large
}

public enum YabaCoreReaderLineHeight: String, Sendable, CaseIterable {
    case normal
    case relaxed
}

public struct YabaCoreReaderPreferences: Sendable, Equatable {
    public var theme: YabaCoreReaderTheme
    public var fontSize: YabaCoreReaderFontSize
    public var lineHeight: YabaCoreReaderLineHeight

    public init(
        theme: YabaCoreReaderTheme = .system,
        fontSize: YabaCoreReaderFontSize = .medium,
        lineHeight: YabaCoreReaderLineHeight = .normal
    ) {
        self.theme = theme
        self.fontSize = fontSize
        self.lineHeight = lineHeight
    }
}
