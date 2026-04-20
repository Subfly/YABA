//
//  ReaderPreferences.swift
//  YABACore
//
//  Parity with Compose `ReaderTheme` / `ReaderFontSize` / `ReaderLineHeight` / `ReaderPreferences`.
//

import Foundation
import SwiftUI

public enum ReaderTheme: String, Sendable, CaseIterable {
    case system
    case dark
    case light
    case sepia

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .system: return LocalizedStringKey("Reader Preference Theme System Title")
        case .dark: return LocalizedStringKey("Reader Preference Theme Dark Title")
        case .light: return LocalizedStringKey("Reader Preference Theme Light Title")
        case .sepia: return LocalizedStringKey("Reader Preference Theme Sepia Title")
        }
    }
}

public enum ReaderFontSize: String, Sendable, CaseIterable {
    case small
    case medium
    case large

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .small: return LocalizedStringKey("Reader Preference Font Size Small Title")
        case .medium: return LocalizedStringKey("Reader Preference Font Size Medium Title")
        case .large: return LocalizedStringKey("Reader Preference Font Size Large Title")
        }
    }
}

public enum ReaderLineHeight: String, Sendable, CaseIterable {
    case normal
    case relaxed

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .normal: return LocalizedStringKey("Reader Preference Line Height Normal Title")
        case .relaxed: return LocalizedStringKey("Reader Preference Line Height Relaxed Title")
        }
    }
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
