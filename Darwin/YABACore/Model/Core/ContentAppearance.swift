//
//  ContentAppearance.swift
//  YABACore
//
//  Parity with Compose `ContentAppearance`.
//

import Foundation
import SwiftUI

public enum ContentAppearance: String, Sendable, Codable, CaseIterable {
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

    public func getUIIconName() -> String {
        uiIconName
    }

    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .list: return LocalizedStringKey("View List")
        case .card: return LocalizedStringKey("View Card")
        case .grid: return LocalizedStringKey("View Grid")
        }
    }
}
