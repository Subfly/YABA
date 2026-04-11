//
//  IconSelectionUIState.swift
//  YABACore
//

import Foundation

/// Parity with Compose `IconSelectionUIState`.
public struct IconSelectionUIState: Sendable {
    public var icons: [YabaIconItem]
    public var isLoadingIcons: Bool
    public var selectedIconName: String

    public init(
        icons: [YabaIconItem] = [],
        isLoadingIcons: Bool = false,
        selectedIconName: String = ""
    ) {
        self.icons = icons
        self.isLoadingIcons = isLoadingIcons
        self.selectedIconName = selectedIconName
    }
}
