//
//  ThemePreference.swift
//  YABACore
//
//  Parity with Compose `ThemePreference`.
//

import Foundation
import SwiftUI

public enum ThemePreference: String, Sendable, Codable, CaseIterable {
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

    public func getUIIconName() -> String {
        uiIconName
    }

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .light: return LocalizedStringKey("Theme Light")
        case .dark: return LocalizedStringKey("Theme Dark")
        case .system: return LocalizedStringKey("Theme System")
        }
    }

    public func getScheme() -> ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }
}
