//
//  YabaCoreThemePreference.swift
//  YABACore
//
//  Parity with Compose `ThemePreference`.
//

import Foundation

public enum YabaCoreThemePreference: String, Sendable, Codable, CaseIterable {
    case light
    case dark
    case system

    public var uiIconName: String {
        switch self {
        case .light: return "sun-03"
        case .dark: return "moon-02"
        case .system: return "smart-phone-02"
        }
    }
}
