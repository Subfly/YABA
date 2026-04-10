//
//  IconSelectionUIState.swift
//  YABACore
//

import Foundation

public struct IconSelectionUIState: Sendable {
    public var category: YabaIconCategory?
    public var selectedIconName: String

    public init(category: YabaIconCategory? = nil, selectedIconName: String = "") {
        self.category = category
        self.selectedIconName = selectedIconName
    }
}
