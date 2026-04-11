//
//  TagCreationEvent.swift
//  YABACore
//
//  Parity with Compose `TagCreationEvent` — color as `YabaColor` (Compose `YabaColor.code`).
//

import Foundation

public enum TagCreationEvent: Sendable {
    case onInitWithTag(tagId: String?)
    case onSelectNewColor(YabaColor)
    case onSelectNewIcon(String)
    case onChangeLabel(String)
    case onSave
}
