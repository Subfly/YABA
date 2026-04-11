//
//  IconCategorySelectionUIState.swift
//  YABACore
//

import Foundation

/// Parity with Compose `IconCategorySelectionUIState`.
public struct IconCategorySelectionUIState: Sendable {
    public var categories: [YabaIconCategory]
    public var isLoading: Bool

    public init(categories: [YabaIconCategory] = [], isLoading: Bool = true) {
        self.categories = categories
        self.isLoading = isLoading
    }
}
