//
//  YabaCoreSortModels.swift
//  YABACore
//
//  Parity with Compose `SortType` / `SortOrderType` / `BookmarkAppearance` / `CardImageSizing`.
//

import Foundation
import SwiftUI

public enum SortType: String, Sendable, CaseIterable {
    case createdAt
    case editedAt
    case label
    case custom
}

public extension SortType {
    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .createdAt: return LocalizedStringKey("Sort Created At")
        case .editedAt: return LocalizedStringKey("Sort Edited At")
        case .label: return LocalizedStringKey("Sort Label")
        case .custom: return LocalizedStringKey("Sort Custom")
        }
    }

    public func getUIIconName() -> String {
        switch self {
        case .createdAt: return "clock-04"
        case .editedAt: return "edit-02"
        case .label: return "sorting-a-z-02"
        case .custom: return "custom-field"
        }
    }
}

public enum SortOrderType: String, Sendable, CaseIterable {
    case ascending
    case descending
}

public extension SortOrderType {
    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .ascending: return LocalizedStringKey("Sort Order Ascending")
        case .descending: return LocalizedStringKey("Sort Order Descending")
        }
    }

    public func getUIIconName() -> String {
        switch self {
        case .ascending: return "sorting-1-9"
        case .descending: return "sorting-9-1"
        }
    }
}

public enum BookmarkAppearance: String, Sendable, CaseIterable {
    case list
    case card
    case grid
}

public enum CardImageSizing: String, Sendable, CaseIterable {
    case big
    case small
}

public extension CardImageSizing {
    public func getUITitle() -> LocalizedStringKey {
        switch self {
        case .big: return LocalizedStringKey("Card Image Sizing Big")
        case .small: return LocalizedStringKey("Card Image Sizing Small")
        }
    }

    public func getUIIconName() -> String {
        switch self {
        case .big: return "image-composition-oval"
        case .small: return "image-composition"
        }
    }
}
