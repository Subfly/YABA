//
//  YabaColor.swift
//  YABACore
//
//  Parity with Compose `YabaColor.code` values.
//

import Foundation
import SwiftUI

public enum YabaColor: Int, Sendable, CaseIterable {
    case none = 0
    case blue = 1
    case brown = 2
    case cyan = 3
    case gray = 4
    case green = 5
    case indigo = 6
    case mint = 7
    case orange = 8
    case pink = 9
    case purple = 10
    case red = 11
    case teal = 12
    case yellow = 13
}

public extension YabaColor {
    public func getUIColor() -> Color {
        switch self {
        case .blue: return .blue
        case .brown: return .brown
        case .cyan: return .cyan
        case .gray: return .gray
        case .green: return .green
        case .indigo: return .indigo
        case .mint: return .mint
        case .orange: return .orange
        case .pink: return .pink
        case .purple: return .purple
        case .red: return .red
        case .teal: return .teal
        case .yellow: return .yellow
        case .none: return .accentColor
        }
    }

    public func getUIText() -> String {
        switch self {
        case .blue: return "Blue"
        case .brown: return "Brown"
        case .cyan: return "Cyan"
        case .gray: return "Gray"
        case .green: return "Green"
        case .indigo: return "Indigo"
        case .mint: return "Mint"
        case .orange: return "Orange"
        case .pink: return "Pink"
        case .purple: return "Purple"
        case .red: return "Red"
        case .teal: return "Teal"
        case .yellow: return "Yellow"
        case .none: return "Theme Color"
        }
    }
}
