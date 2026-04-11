//
//  YabaCoreContentAppearance.swift
//  YABACore
//
//  Parity with Compose `ContentAppearance`.
//

import Foundation

public enum YabaCoreContentAppearance: String, Sendable, Codable, CaseIterable {
    case list
    case card
    case grid

    public var uiIconName: String {
        switch self {
        case .list: return "list-view"
        case .card: return "rectangular"
        case .grid: return "grid-view"
        }
    }
}
