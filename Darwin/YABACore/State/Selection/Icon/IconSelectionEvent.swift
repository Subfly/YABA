//
//  IconSelectionEvent.swift
//  YABACore
//
//  Parity with Compose `IconSelectionEvent`.
//

import Foundation

public enum IconSelectionEvent: Sendable {
    case onInit(category: YabaIconCategory, initialSelectedIcon: String)
    case onSelectIcon(iconName: String)
}
